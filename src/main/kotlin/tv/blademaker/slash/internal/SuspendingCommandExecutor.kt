package tv.blademaker.slash.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.client.DefaultSlashCommandClient
import tv.blademaker.slash.context.*
import tv.blademaker.slash.extensions.newCoroutineDispatcher
import tv.blademaker.slash.ratelimit.RateLimitClient
import java.util.regex.Matcher
import kotlin.coroutines.CoroutineContext

open class SuspendingCommandExecutor(
    private val client: DefaultSlashCommandClient,
    private val rateLimit: RateLimitClient?
) : CoroutineScope {

    private val dispatcher = newCoroutineDispatcher("slash-commands-worker-%s", 2, 50)

    override val coroutineContext: CoroutineContext
        get() = dispatcher + Job()

    private suspend fun creteContext(handler: SlashCommandHandler, event: SlashCommandInteractionEvent): SlashCommandContext {
        return when (handler.target) {
            InteractionTarget.GUILD -> client.contextCreator.createGuildContext(event, handler.function)
            InteractionTarget.DM -> client.contextCreator.createContext(event, handler.function)
            InteractionTarget.ALL -> when (event.isFromGuild) {
                true -> client.contextCreator.createGuildContext(event, handler.function)
                false -> client.contextCreator.createContext(event, handler.function)
            }
        }
    }

    private suspend fun checkGlobals(ctx: SlashCommandContext): Boolean {
        if (client.checks.isEmpty()) return true
        return client.checks.all { it(ctx) }
    }

    internal fun execute(event: SlashCommandInteractionEvent, handler: SlashCommandHandler) = launch {
        try {
            client.metrics?.incHandledCommand(event)
            val ctx = creteContext(handler, event)

            if (handler.rateLimit != null && rateLimit != null) {
                val waitFor = rateLimit.acquire(handler.rateLimit, event)

                if (waitFor != null) {
                    rateLimit.onRateLimitHit(ctx, handler.rateLimit, waitFor)
                    return@launch
                }
            }

            log.debug("Running global checks")
            if (!checkGlobals(ctx)) return@launch

            log.debug("Running handler parent checks")
            if (!handler.parent.doChecks(ctx)) return@launch
            if (ctx is GuildSlashCommandContext) {
                log.debug("Running Guild checks")
                Checks.handlerPermissions(ctx, handler.permissions)
            }


            logEvent(event)
            val startTime = System.nanoTime()
            handler.execute(ctx, client.timeout)
            val time = (System.nanoTime() - startTime) / 1_000_000

            client.metrics?.incSuccessCommand(event, time)
        } catch (timeout: TimeoutCancellationException){
            client.exceptionHandler.onTimeoutCancellationException(timeout, event, client.timeout)
            client.metrics?.incFailedCommand(event)
        } catch (expected: Exception) {
            client.exceptionHandler.wrap(expected, handler.parent, event)
            client.metrics?.incFailedCommand(event)
        }
    }

    internal fun execute(event: CommandAutoCompleteInteractionEvent, handler: AutoCompleteHandler) = launch {
        try {
            val ctx = AutoCompleteContext(event, handler.function)

            logEvent(event)
            handler.execute(ctx)
        } catch (e: Exception) {
            client.exceptionHandler.wrap(e, handler.parent, event)
        }
    }

    internal fun execute(event: ModalInteractionEvent, handler: ModalHandler, matcher: Matcher) = launch {
        try {
            val ctx = ModalContext(event, matcher, handler.function)

            logEvent(event)
            handler.execute(ctx)
        } catch (e: Throwable) {
            client.exceptionHandler.onException(e, handler.parent, event)
        }
    }

    internal fun execute(event: ButtonInteractionEvent, handler: ButtonHandler, matcher: Matcher) = launch {
        try {
            val ctx = ButtonContext(event, matcher, handler.function)

            logEvent(event)
            handler.execute(ctx)
        } catch (e: Throwable) {
            client.exceptionHandler.onException(e, handler.parent, event)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SuspendingCommandExecutor::class.java)

        private fun logEvent(event: SlashCommandInteractionEvent) {
            log.info("${getEventLogPrefix(event)} [Slash Command] --> ${event.commandString}")
        }

        private fun logEvent(event: CommandAutoCompleteInteractionEvent) {
            log.info("${getEventLogPrefix(event)} [Auto Complete] --> ${event.commandString}")
        }

        private fun logEvent(event: ModalInteractionEvent) {
            log.info("${getEventLogPrefix(event)} [Modal] --> ${event.modalId}")
        }

        private fun logEvent(event: ButtonInteractionEvent) {
            log.info("${getEventLogPrefix(event)} [Button] --> ${event.button.id}")
        }

        private fun getEventLogPrefix(event: GenericInteractionCreateEvent) = when (event.isFromGuild) {
            true -> "[\u001b[32mSP::${event.guild?.name}(${event.guild?.id})\u001b[0m] ${event.user.asTag}"
            false -> "[\u001b[32mDM::${event.user.asTag}(${event.user.id})\u001b[0m]"
        }
    }
}