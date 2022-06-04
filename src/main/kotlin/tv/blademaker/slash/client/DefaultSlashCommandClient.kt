package tv.blademaker.slash.client

import kotlinx.coroutines.CoroutineScope
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.metrics.Metrics
import tv.blademaker.slash.context.ContextCreator
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.internal.*
import tv.blademaker.slash.internal.AutoCompleteHandler
import tv.blademaker.slash.internal.CommandHandlers
import tv.blademaker.slash.metrics.MetricsStrategy
import tv.blademaker.slash.ratelimit.RateLimitClient
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
    internal val checks: MutableSet<CommandExecutionCheck>,
    internal val timeout: Duration,
    internal val rateLimit: RateLimitClient?,
    strategy: MetricsStrategy?
) : SlashCommandClient {

    internal val metrics: Metrics? = if (strategy != null) Metrics(strategy) else null

    private val executor = SuspendingCommandExecutor(this, rateLimit)

    private val discoveryResult = SlashUtils.discoverSlashCommands(packageName)

    override val registry = discoveryResult.let {
        log.info("Discovered a total of ${it.commands.size} commands in ${it.elapsedTime}ms.")
        it.commands
    }

    private val commandHandlers: CommandHandlers = SlashUtils.compileCommandHandlers(discoveryResult.commands)

    private fun findHandler(event: SlashCommandInteractionEvent): SlashCommandHandler? {
        return commandHandlers.slash.find { it.path == event.commandPath }
    }
    private fun findHandler(event: CommandAutoCompleteInteractionEvent): AutoCompleteHandler? {
        return commandHandlers.autoComplete.find { it.path == event.commandPath && it.optionName == event.focusedOption.name }
    }

    override fun onSlashCommandEvent(event: SlashCommandInteractionEvent) {
        val handler = findHandler(event)

        if (handler == null) {
            log.error("Not found handler for command path ${event.commandPath}")
            return event.reply("Not found handler for command path ${event.commandPath}," +
                    "this exceptions is reported to developer automatically.").setEphemeral(true).queue()
        }

        log.debug("Executing handler ${handler.path} for command path ${event.commandPath}")

        executor.execute(event, handler)
    }

    override fun onCommandAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent) {
        findHandler(event)?.run { executor.execute(event, this) }
    }

    fun addCheck(check: CommandExecutionCheck) {
        if (checks.contains(check)) error("check already registered.")
        checks.add(check)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(DefaultSlashCommandClient::class.java)
    }
}