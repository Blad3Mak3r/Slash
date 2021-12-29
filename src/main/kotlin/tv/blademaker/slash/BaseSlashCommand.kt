package tv.blademaker.slash

import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.internal.CommandExecutionCheck

abstract class BaseSlashCommand(val commandName: String) {

    private val checks: MutableList<CommandExecutionCheck> = mutableListOf()

    internal suspend fun doChecks(ctx: SlashCommandContext): Boolean {
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

    /*private suspend fun handleSlashCommand(ctx: SlashCommandContext, handler: SlashCommandHandler) {
        if (ctx is GuildSlashCommandContext) Checks.commandPermissions(ctx, handler.permissions)
        handler.execute(ctx)
    }*/

    /*private suspend fun handleAutoComplete(ctx: AutoCompleteContext) {
        val commandPath = ctx.commandPath
        val option = ctx.focusedOption

        val handler = handlers.autoComplete.find { it.path == commandPath && it.optionName == option.name }
            ?: error("No handler found for auto-complete command path $commandPath")

        handler.execute(ctx)
    }*/

    /*open suspend fun execute(ctx: SlashCommandContext, handler: SlashCommandHandler) {
        SlashUtils.log.debug("Starting execution of command (${this.commandName}).")
        if (!doChecks(ctx)) return

        handleSlashCommand(ctx, handler)
        SlashUtils.log.debug("Finalized execution of command (${this.commandName}).")
    }*/

    /*suspend fun executeAutoComplete(ctx: AutoCompleteContext) {
        SlashUtils.log.debug("Starting execution of auto-complete handler of command (${this.commandName}).")
        handleAutoComplete(ctx)
        SlashUtils.log.debug("Finalized execution of auto-complete handler of command (${this.commandName}).")
    }*/
}
