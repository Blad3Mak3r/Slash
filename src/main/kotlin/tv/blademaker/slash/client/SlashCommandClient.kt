package tv.blademaker.slash.client

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
import org.slf4j.LoggerFactory
import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.context.*
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.extensions.commandPath
import tv.blademaker.slash.extensions.newCoroutineDispatcher
import tv.blademaker.slash.internal.*
import tv.blademaker.slash.metrics.Metrics
import tv.blademaker.slash.metrics.MetricsStrategy
import tv.blademaker.slash.ratelimit.RateLimitClient
import java.util.regex.Matcher
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * Extendable coroutine based SlashCommandClient
 *
 * @param packageName The package name where commands are located (me.example.commands)
 *
 * @see SlashCommandClient
 * @see CoroutineScope
 *
 * @see SlashUtils.discoverSlashCommands
 */
class SlashCommandClient internal constructor(
    packageName: String,
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

    private val discoveryResult = SlashUtils.discoverSlashCommands(packageName)

    private val registry = discoveryResult.also {
        log.info("Discovered a total of ${it.slashCommands.size} BaseSlashCommands, ${it.messageCommands.size} " +
            "MessageCommands and ${it.userCommands.size} UserCommands in ${it.elapsedTime}ms.")
    }.let { Registry.fromDiscovery(it) }

    private val commandHandlers: CommandHandlers = SlashUtils.compileSlashCommandHandlers(registry.slash)

    private fun findHandler(event: SlashCommandInteractionEvent): SlashCommandHandler? {
        return commandHandlers.slash.find { it.path == event.commandPath }
    }
    private fun findHandler(event: CommandAutoCompleteInteractionEvent): AutoCompleteHandler? {
        return commandHandlers.autoComplete.find { it.path == event.commandPath && it.optionName == event.focusedOption.name }
    }

    private fun findHandler(event: ModalInteractionEvent): Pair<Matcher, ModalHandler>? {
        return commandHandlers.modalHandlers.find { it.matches(event.modalId) }?.let {
            Pair(it.matcher(event.modalId), it)
        }
    }

    private fun findHandler(event: ButtonInteractionEvent): Pair<Matcher, ButtonHandler>? {
        val buttonId = event.button.id ?: return null
        return commandHandlers.buttonHandlers.find { it.matches(buttonId) }?.let {
            Pair(it.matcher(buttonId), it)
        }
    }


    override fun onEvent(event: GenericEvent) {
        launch { eventsFlow.emit(event) }
        when (event) {
            is SlashCommandInteractionEvent -> launch { onSlashCommandEvent(event) }
            is CommandAutoCompleteInteractionEvent -> launch { onCommandAutoCompleteEvent(event) }
            is ModalInteractionEvent -> launch { onModalInteractionEvent(event) }
            is ButtonInteractionEvent -> launch { onButtonInteractionEvent(event) }
            is MessageContextInteractionEvent -> launch { onMessageContextInteractionEvent(event) }
            is UserContextInteractionEvent -> launch { onUserContextInteractionEvent(event) }
        }
    }

    private fun createSlashCommandContext(
        handler: SlashCommandHandler,
        event: SlashCommandInteractionEvent
    ): SlashCommandContext {
        return when (handler.target) {
            InteractionTarget.GUILD -> GuildSlashCommandContext(this, event, handler.function)
            InteractionTarget.DM -> SlashCommandContext(this, event, handler.function)
            InteractionTarget.ALL -> when (event.isFromGuild) {
                true -> GuildSlashCommandContext(this, event, handler.function)
                false -> SlashCommandContext(this, event, handler.function)
            }
        }
    }

    private suspend fun onSlashCommandEvent(event: SlashCommandInteractionEvent) {
        val handler = findHandler(event)

        if (handler == null) {
            log.error("Not found handler for command path ${event.commandPath}")
            return event.reply("Not found handler for command path ${event.commandPath}," +
                    "this exceptions is reported to developer automatically.").setEphemeral(true).queue()
        }

        log.debug("Executing handler ${handler.path} for command path ${event.commandPath}")

        try {
            metrics?.incHandledCommand(event)
            val ctx = createSlashCommandContext(handler, event)

            if (handler.rateLimit != null && rateLimit != null) {
                val waitFor = rateLimit.acquire(handler.rateLimit, event)

                if (waitFor != null) {
                    rateLimit.onRateLimitHit(ctx, handler.rateLimit, waitFor)
                    return
                }
            }

            val passed = interceptors
                .filterIsInstance<SlashCommandInterceptor>()
                .all { interceptor ->  interceptor.intercept(ctx) }

            if (!passed)
                return

            log.debug("Running handler parent checks")
            if (!handler.parent.runInterceptors(ctx)) return
            if (ctx is GuildSlashCommandContext) {
                log.debug("Running Guild checks")
                Interceptors.handlerPermissions(ctx, handler.permissions)
            }

            log.info("${getEventLogPrefix(event)} [Slash Command] --> ${event.commandString}")
            val startTime = System.nanoTime()
            handler.execute(ctx, timeout)
            val time = (System.nanoTime() - startTime) / 1_000_000

            metrics?.incSuccessCommand(event, time)
        } catch (tce: TimeoutCancellationException){
            exceptionHandler.onTimeoutCancellationException(tce, event, timeout)
            metrics?.incFailedCommand(event)
        } catch (ex: Exception) {
            exceptionHandler.wrap(ex, handler.parent, event)
            metrics?.incFailedCommand(event)
        }
    }

    private suspend fun onCommandAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent) {
        val handler = findHandler(event)
            ?: return log.error("Not found handler for AutoComplete path ${event.commandPath}")

        val ctx = AutoCompleteContext(event, this, handler.function)

        log.info("${getEventLogPrefix(event)} [Auto Complete] --> ${event.commandString}")
        handler.execute(ctx)
    }

    private suspend fun onModalInteractionEvent(event: ModalInteractionEvent) {
        val handler = findHandler(event)
            ?: return log.error("Not found handler for ModalInteraction path ${event.modalId}")

        val ctx = ModalContext(event, this, handler.first, handler.second.function)

        log.info("${getEventLogPrefix(event)} [Modal] --> ${event.modalId}")
        handler.second.execute(ctx)
    }

    private suspend fun onButtonInteractionEvent(event: ButtonInteractionEvent) {
        val handler = findHandler(event)
            ?: return log.error("Not found handler for ButtonInteraction path ${event.button.id}")

        val ctx = ButtonContext(event, this, handler.first, handler.second.function)

        log.info("${getEventLogPrefix(event)} [Button] --> ${event.button.id}")
        handler.second.execute(ctx)
    }

    private suspend fun onMessageContextInteractionEvent(event: MessageContextInteractionEvent) {
        log.info("${getEventLogPrefix(event)} [Message Context] --> ${event.fullCommandName} (${event.target})")

        val command = registry.message.find { it.commandName.equals(event.fullCommandName, true) }
            ?: return log.error("Not found handler for UserContextInteraction ${event.fullCommandName}")

        val ctx = MessageCommandContext(event, this)

        if (command.runInterceptors(ctx)) {
            command.handle(ctx)
        }
    }

    private suspend fun onUserContextInteractionEvent(event: UserContextInteractionEvent) {
        log.info("${getEventLogPrefix(event)} [User Context] --> ${event.fullCommandName} (${event.target})")

        val command = registry.user.find { it.commandName.equals(event.fullCommandName, true) }
            ?: return log.error("Not found handler for UserContextInteraction ${event.fullCommandName}")

        val ctx = UserCommandContext(event, this)

        if (command.runInterceptors(ctx)) {
            command.handle(ctx)
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(SlashCommandClient::class.java)

        private fun getEventLogPrefix(event: GenericInteractionCreateEvent) = when (event.isFromGuild) {
            true -> "[\u001b[32mSP::${event.guild?.name}(${event.guild?.id})\u001b[0m] ${event.user.effectiveName}"
            false -> "[\u001b[32mDM::${event.user.effectiveName}(${event.user.id})\u001b[0m]"
        }

        fun builder(packageName: String): SlashCommandClientBuilder = SlashCommandClientBuilder(packageName)
    }
}