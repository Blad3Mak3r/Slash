package tv.blademaker.slash

import org.slf4j.LoggerFactory
import tv.blademaker.slash.annotations.AutoComplete
import tv.blademaker.slash.annotations.SlashCommand
import tv.blademaker.slash.context.AutoCompleteContext
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.context.impl.GuildSlashCommandContext
import tv.blademaker.slash.internal.AutoCompleteHandler
import tv.blademaker.slash.internal.Checks
import tv.blademaker.slash.internal.CommandExecutionCheck
import tv.blademaker.slash.internal.InteractionHandler
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

abstract class BaseSlashCommand(val commandName: String) {

    private val checks: MutableList<CommandExecutionCheck> = mutableListOf()

    private val handlers by lazy { compileHandlers(this) }

    @Suppress("unused")
    val paths: List<String> by lazy { handlers.slash.map { it.path }.sorted() }

    private suspend fun doChecks(ctx: SlashCommandContext): Boolean {
        if (checks.isEmpty()) return true
        return checks.all { it(ctx) }
    }

    @Suppress("unused")
    fun addCheck(check: CommandExecutionCheck) {
        if (checks.contains(check)) error("Check already registered.")
        checks.add(check)
    }


    @Suppress("unused")
    fun addChecks(checks: Collection<CommandExecutionCheck>) {
        for (check in checks) {
            addCheck(check)
        }
    }

    @Suppress("unused")
    fun removeCheck(check: CommandExecutionCheck) {
        if (!checks.contains(check)) error("Check is not registered.")
        checks.remove(check)
    }

    @Suppress("unused")
    fun removeChecks(checks: Collection<CommandExecutionCheck>) {
        for (check in checks) {
            removeCheck(check)
        }
    }

    private suspend fun handleSlashCommand(ctx: SlashCommandContext) {
        val commandPath = ctx.event.commandPath

        val handler = handlers.slash.find { it.path == commandPath }
            ?: error("No handler found for slash command path $commandPath")

        if (ctx is GuildSlashCommandContext) Checks.commandPermissions(ctx, handler.permissions)

        handler.execute(ctx)
    }

    private suspend fun handleAutoComplete(ctx: AutoCompleteContext) {
        val commandPath = ctx.commandPath
        val option = ctx.focusedOption

        val handler = handlers.autoComplete.find { it.path == commandPath && it.optionName == option.name }
            ?: error("No handler found for auto-complete command path $commandPath")

        handler.execute(ctx)
    }

    open suspend fun execute(ctx: SlashCommandContext) {
        SlashUtils.log.debug("Starting execution of command (${this.commandName}).")
        if (!doChecks(ctx)) return

        handleSlashCommand(ctx)
        SlashUtils.log.debug("Finalized execution of command (${this.commandName}).")
    }

    suspend fun executeAutoComplete(ctx: AutoCompleteContext) {
        SlashUtils.log.debug("Starting execution of auto-complete handler of command (${this.commandName}).")
        handleAutoComplete(ctx)
        SlashUtils.log.debug("Finalized execution of auto-complete handler of command (${this.commandName}).")
    }

    internal data class Handlers(
        val slash: List<InteractionHandler>,
        val autoComplete: List<AutoCompleteHandler>
    )

    private companion object {
        private fun compileHandlers(command: BaseSlashCommand) = Handlers(
            compileInteractionHandlers(command),
            compileAutoCompleteHandlers(command)
        )

        private fun compileInteractionHandlers(command: BaseSlashCommand): List<InteractionHandler> {
            val handlers = command::class.functions
                .filter { it.hasAnnotation<SlashCommand>() && it.visibility == KVisibility.PUBLIC && !it.isAbstract }
                .map { InteractionHandler(command, it) }

            val finalList = mutableListOf<InteractionHandler>()

            for (handler in handlers) {
                check(!finalList.any { it.path == handler.path }) {
                    "Found more than one InteractionHandler for the same path ${handler.path}"
                }
                finalList.add(handler)
            }

            check(finalList.isNotEmpty()) {
                "SlashCommand ${command.commandName} does not have registered handlers."
            }

            checkDefault(command, finalList) {
                "SlashCommand ${command.commandName} have registered more than 1 handler having a default handler."
            }

            return finalList
        }

        private fun compileAutoCompleteHandlers(command: BaseSlashCommand): List<AutoCompleteHandler> {
            val handlers = command::class.functions
                .filter { it.hasAnnotation<AutoComplete>() && it.visibility == KVisibility.PUBLIC && !it.isAbstract }
                .map { AutoCompleteHandler(command, it) }

            val finalList = mutableListOf<AutoCompleteHandler>()

            for (handler in handlers) {
                check(!finalList.any { it.path == handler.path && it.optionName == handler.optionName }) {
                    "Found more than one AutocompleteHandler for the same path (${handler.path}) and option (${handler.optionName})."
                }
                finalList.add(handler)
            }

            check(finalList.isNotEmpty()) {
                "SlashCommand ${command.commandName} does not have registered handlers."
            }

            return finalList
        }

        private fun checkDefault(command: BaseSlashCommand, list: List<InteractionHandler>, lazyMessage: () -> String) {
            if (list.size <= 1) return
            if (list.any { it.path == command.commandName }) error(lazyMessage())
        }
    }
}
