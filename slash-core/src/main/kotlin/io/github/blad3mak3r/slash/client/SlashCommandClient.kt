package io.github.blad3mak3r.slash.client

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import io.github.blad3mak3r.slash.SlashUtils
import io.github.blad3mak3r.slash.annotations.InteractionTarget
import io.github.blad3mak3r.slash.context.*
import io.github.blad3mak3r.slash.exceptions.ExceptionHandler
import io.github.blad3mak3r.slash.extensions.commandPath
import io.github.blad3mak3r.slash.extensions.newCoroutineDispatcher
import io.github.blad3mak3r.slash.internal.*
import io.github.blad3mak3r.slash.metrics.Metrics
import io.github.blad3mak3r.slash.metrics.MetricsStrategy
import io.github.blad3mak3r.slash.ratelimit.RateLimitClient
import io.github.blad3mak3r.slash.registry.CommandRegistrar
import io.github.blad3mak3r.slash.registry.HandlerRegistry
import java.util.ServiceLoader
import java.util.regex.Matcher
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class SlashCommandClient internal constructor(
    private val eventsFlow: MutableSharedFlow<GenericEvent>,
    private val exceptionHandler: ExceptionHandler,
    private val interceptors: MutableSet<Interceptor<*>>,
    private val timeout: Duration,
    private val rateLimit: RateLimitClient?,
    strategy: MetricsStrategy?
) : EventListener, CoroutineScope {

    val events = eventsFlow.asSharedFlow()

    private val dispatcher = newCoroutineDispatcher("SlashWorker-%s", 8, 50)
    private val supervisorJob = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = dispatcher + Job(supervisorJob)

    private val metrics: Metrics? = if (strategy != null) Metrics(strategy) else null

    private val registry: HandlerRegistry = HandlerRegistry().also { reg ->
        val loader = ServiceLoader.load(CommandRegistrar::class.java)
        var count = 0
        for (registrar in loader) {
            registrar.register(reg)
            count++
        }
        log.info(
            "Loaded $count CommandRegistrar(s) — " +
            "${reg.slash.size} slash, ${reg.message.size} message, ${reg.user.size} user, " +
            "${reg.buttons.size} button(s), ${reg.modals.size} modal(s)."
        )
    }

    // ── Lookup helpers ────────────────────────────────────────────────────────

    private fun findSlashEntry(event: SlashCommandInteractionEvent) =
        registry.slash[event.commandPath]

    private fun findAutoCompleteEntry(event: CommandAutoCompleteInteractionEvent) =
        registry.autoComplete["${event.commandPath}:${event.focusedOption.name}"]

    private fun findButtonEntry(event: ButtonInteractionEvent): Pair<Matcher, suspend (ButtonContext) -> Unit>? {
        val id = event.button.customId ?: return null
        val entry = registry.buttons.firstOrNull { it.matches(id) } ?: return null
        return Pair(entry.matcher(id), entry.handler)
    }

    private fun findModalEntry(event: ModalInteractionEvent): Pair<Matcher, suspend (ModalContext) -> Unit>? {
        val entry = registry.modals.firstOrNull { it.matches(event.modalId) } ?: return null
        return Pair(entry.matcher(event.modalId), entry.handler)
    }

    // ── EventListener ─────────────────────────────────────────────────────────

    override fun onEvent(event: GenericEvent) {
        launch { eventsFlow.emit(event) }
        when (event) {
            is SlashCommandInteractionEvent        -> launch { onSlashCommandEvent(event) }
            is CommandAutoCompleteInteractionEvent -> launch { onCommandAutoCompleteEvent(event) }
            is ModalInteractionEvent               -> launch { onModalInteractionEvent(event) }
            is ButtonInteractionEvent              -> launch { onButtonInteractionEvent(event) }
            is MessageContextInteractionEvent      -> launch { onMessageContextInteractionEvent(event) }
            is UserContextInteractionEvent         -> launch { onUserContextInteractionEvent(event) }
        }
    }

    // ── Context factory ───────────────────────────────────────────────────────

    private fun createSlashCommandContext(
        target: InteractionTarget,
        event: SlashCommandInteractionEvent
    ): SlashCommandContext = when (target) {
        InteractionTarget.GUILD -> GuildSlashCommandContext(this, event)
        InteractionTarget.DM    -> SlashCommandContext(this, event)
        InteractionTarget.ALL   -> if (event.isFromGuild) GuildSlashCommandContext(this, event)
                                   else SlashCommandContext(this, event)
    }

    // ── Target / detach guards ────────────────────────────────────────────────

    private fun checkEventTarget(event: SlashCommandInteractionEvent, target: InteractionTarget): Boolean {
        return when (target) {
            InteractionTarget.ALL  -> true
            InteractionTarget.GUILD -> {
                if (!event.isFromGuild) {
                    event.reply("This command cannot be used outside of a **Guild**.").setEphemeral(true).queue()
                    false
                } else true
            }
            InteractionTarget.DM -> {
                if (event.isFromGuild) {
                    event.reply("This command cannot be used on a **Guild**.").setEphemeral(true).queue()
                    false
                } else true
            }
        }
    }

    private fun checkDetachedSupport(event: SlashCommandInteractionEvent, supportDetached: Boolean): Boolean {
        if (event.isFromAttachedGuild || supportDetached) return true
        event.reply("This command does not support detached messages.").setEphemeral(true).queue()
        log.warn("Detached message rejected for ${event.commandPath}")
        return false
    }

    // ── Event handlers ────────────────────────────────────────────────────────

    private suspend fun onSlashCommandEvent(event: SlashCommandInteractionEvent) {
        val entry = findSlashEntry(event)
        if (entry == null) {
            log.error("No handler for slash command path '${event.commandPath}'")
            event.reply("Command not found: `${event.commandPath}`.").setEphemeral(true).queue()
            return
        }

        if (!checkEventTarget(event, entry.target)) return
        if (!checkDetachedSupport(event, entry.supportDetached)) return

        log.debug("Executing slash handler '${entry.path}'")

        try {
            metrics?.incHandledCommand(event)
            val ctx = createSlashCommandContext(entry.target, event)

            // Rate limiting
            if (entry.rateLimit != null && rateLimit != null) {
                val waitFor = rateLimit.acquire(entry.rateLimit, event)
                if (waitFor != null) {
                    rateLimit.onRateLimitHit(ctx, entry.rateLimit, waitFor)
                    return
                }
            }

            // Global interceptors
            val passedGlobal = interceptors
                .filterIsInstance<SlashCommandInterceptor>()
                .all { it.intercept(ctx) }
            if (!passedGlobal) return

            // Preconditions
            val passedPre = entry.preconditions.all { it.check(ctx) }
            if (!passedPre) return

            // Permission check (guild only)
            if (ctx is GuildSlashCommandContext) {
                Interceptors.handlerPermissions(ctx, entry.permissions)
            }

            log.info("${getEventLogPrefix(event)} [Slash] --> ${event.commandString}")
            val start = System.nanoTime()
            withTimeout(timeout) { entry.handler(ctx) }
            metrics?.incSuccessCommand(event, (System.nanoTime() - start) / 1_000_000)

        } catch (tce: TimeoutCancellationException) {
            exceptionHandler.onTimeoutCancellationException(tce, event, timeout)
            metrics?.incFailedCommand(event)
        } catch (ex: Exception) {
            exceptionHandler.wrap(ex, event)
            metrics?.incFailedCommand(event)
        }
    }

    private suspend fun onCommandAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent) {
        val entry = findAutoCompleteEntry(event)
        if (entry == null) {
            log.error("No autocomplete handler for '${event.commandPath}' / option '${event.focusedOption.name}'")
            return
        }
        log.info("${getEventLogPrefix(event)} [AutoComplete] --> ${event.commandString}")
        try {
            entry.handler(AutoCompleteContext(event, this))
        } catch (ex: Exception) {
            exceptionHandler.onException(ex, event)
        }
    }

    private suspend fun onModalInteractionEvent(event: ModalInteractionEvent) {
        val (matcher, handler) = findModalEntry(event) ?: run {
            log.error("No modal handler for '${event.modalId}'")
            return
        }
        log.info("${getEventLogPrefix(event)} [Modal] --> ${event.modalId}")
        try {
            handler(ModalContext(event, this, matcher))
        } catch (ex: Exception) {
            exceptionHandler.onException(ex, event)
        }
    }

    private suspend fun onButtonInteractionEvent(event: ButtonInteractionEvent) {
        val (matcher, handler) = findButtonEntry(event) ?: run {
            log.error("No button handler for '${event.button.customId}'")
            return
        }
        log.info("${getEventLogPrefix(event)} [Button] --> ${event.button.customId}")
        try {
            handler(ButtonContext(event, this, matcher))
        } catch (ex: Exception) {
            exceptionHandler.onException(ex, event)
        }
    }

    private suspend fun onMessageContextInteractionEvent(event: MessageContextInteractionEvent) {
        val entry = registry.message[event.fullCommandName.lowercase()]
        if (entry == null) {
            log.error("No message-context handler for '${event.fullCommandName}'")
            return
        }
        log.info("${getEventLogPrefix(event)} [Message Context] --> ${event.fullCommandName}")
        try {
            val ctx = MessageCommandContext(event, this)
            val passedGlobal = interceptors
                .filterIsInstance<MessageCommandInterceptor>()
                .all { it.intercept(ctx) }
            if (!passedGlobal) return
            entry.handler(ctx)
        } catch (ex: Exception) {
            exceptionHandler.onException(ex, event)
        }
    }

    private suspend fun onUserContextInteractionEvent(event: UserContextInteractionEvent) {
        val entry = registry.user[event.fullCommandName.lowercase()]
        if (entry == null) {
            log.error("No user-context handler for '${event.fullCommandName}'")
            return
        }
        log.info("${getEventLogPrefix(event)} [User Context] --> ${event.fullCommandName}")
        try {
            val ctx = UserCommandContext(event, this)
            val passedGlobal = interceptors
                .filterIsInstance<UserCommandInterceptor>()
                .all { it.intercept(ctx) }
            if (!passedGlobal) return
            entry.handler(ctx)
        } catch (ex: Exception) {
            exceptionHandler.onException(ex, event)
        }
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        val log: Logger = LoggerFactory.getLogger(SlashCommandClient::class.java)

        private fun getEventLogPrefix(event: GenericInteractionCreateEvent) = when (event.isFromGuild) {
            true  -> "[\u001b[32mSP::${event.guild?.name}(${event.guild?.id})\u001b[0m] ${event.user.effectiveName}"
            false -> "[\u001b[32mDM::${event.user.effectiveName}(${event.user.id})\u001b[0m]"
        }

        fun builder(): SlashCommandClientBuilder = SlashCommandClientBuilder()
    }
}
