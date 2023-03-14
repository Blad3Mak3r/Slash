package tv.blademaker.slash.client

import kotlinx.coroutines.CoroutineScope
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.context.ContextCreator
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.extensions.commandPath
import tv.blademaker.slash.internal.*
import tv.blademaker.slash.metrics.Metrics
import tv.blademaker.slash.metrics.MetricsStrategy
import tv.blademaker.slash.ratelimit.RateLimitClient
import java.util.regex.Matcher
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
class DefaultSlashCommandClient internal constructor(
    packageName: String,
    override val exceptionHandler: ExceptionHandler,
    internal val contextCreator: ContextCreator,
    internal val checks: MutableSet<Interceptor>,
    internal val timeout: Duration,
    rateLimit: RateLimitClient?,
    strategy: MetricsStrategy?
) : SlashCommandClient {

    internal val metrics: Metrics? = if (strategy != null) Metrics(strategy) else null

    private val executor = SuspendingCommandExecutor(this, rateLimit)

    override val handlers = SlashUtils.discoverSlashCommands(packageName)

    private fun findHandler(event: SlashCommandInteractionEvent): SlashCommandHandler? {
        return handlers.onSlashCommand.find { it.annotation.fullName == event.fullCommandName }
    }
    private fun findHandler(event: CommandAutoCompleteInteractionEvent): AutoCompleteHandler? {
        return handlers.onAutoComplete.find { it.annotation.fullName == event.fullCommandName && it.optionName == event.focusedOption.name }
    }

    private fun findHandler(event: ModalInteractionEvent): Pair<Matcher, ModalHandler>? {
        return handlers.onModal.find { it.matches(event.modalId) }?.let {
            Pair(it.matcher(event.modalId), it)
        }
    }

    private fun findHandler(event: ButtonInteractionEvent): Pair<Matcher, ButtonHandler>? {
        val buttonId = event.button.id ?: return null
        return handlers.onButton.find { it.matches(buttonId) }?.let {
            Pair(it.matcher(buttonId), it)
        }
    }

    override fun onSlashCommandEvent(event: SlashCommandInteractionEvent) {
        val handler = findHandler(event)

        if (handler == null) {
            log.error("Not found handler for command path ${event.commandPath}")
            return event.reply("Not found handler for command path ${event.commandPath}," +
                    "this exceptions is reported to developer automatically.").setEphemeral(true).queue()
        }

        log.debug("Executing handler ${handler.annotation.fullName} for command path ${event.commandPath}")

        executor.execute(event, handler)
    }

    override fun onCommandAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent) {
        findHandler(event)?.run { executor.execute(event, this) }
    }

    override fun onModalInteractionEvent(event: ModalInteractionEvent) {
        findHandler(event)?.run { executor.execute(event, this.second, this.first) }
    }

    override fun onButtonInteractionEvent(event: ButtonInteractionEvent) {
        findHandler(event)?.run { executor.execute(event, this.second, this.first) }
    }

    fun addInterceptor(interceptor: Interceptor) {
        if (checks.contains(interceptor)) error("interceptor already registered.")
        checks.add(interceptor)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(DefaultSlashCommandClient::class.java)
    }
}