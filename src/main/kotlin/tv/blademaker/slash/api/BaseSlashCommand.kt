package tv.blademaker.slash.api

import org.slf4j.LoggerFactory
import tv.blademaker.slash.api.annotations.SlashCommand
import tv.blademaker.slash.internal.CommandExecutionCheck
import tv.blademaker.slash.internal.InteractionHandler
import tv.blademaker.slash.internal.SlashUtils
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

abstract class BaseSlashCommand(private val commandClient: SlashCommandClient, val commandName: String) {

    private val checks: MutableList<CommandExecutionCheck> = mutableListOf()

    private val interactionHandlers: List<InteractionHandler> by lazy { compileInteractionHandlers(this) }

    @Suppress("unused")
    val paths: List<String> by lazy { interactionHandlers.map { it.path }.sorted() }

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

    private suspend fun handleInteraction(ctx: SlashCommandContext) {
        val commandPath = ctx.event.commandPath

        val handler = interactionHandlers.find { it.path == commandPath }
            ?: error("No handler found for command path $commandPath")

        try {
            if (!SlashUtils.hasPermissions(commandClient, ctx, handler.permissions)) return

            handler.execute(this, ctx)
        } catch (e: Exception) {
            SlashUtils.captureSlashCommandException(ctx, e, log)
        }
    }

    /*private suspend fun handleSubCommand(ctx: SlashCommandContext): Boolean {
        val subCommandGroup = ctx.event.subcommandGroup

        val subCommandName = ctx.event.subcommandName
            ?: return false

        try {
            val subCommand = subCommands
                .filter { if (subCommandGroup != null) it.groupName == subCommandGroup else true }
                .find { s -> s.name.equals(subCommandName, true) }

            if (subCommand == null) {
                logger.warn("Not found any valid handler for options '$subCommandName', executing default handler.")
                return false
            }

            logger.debug("Executing '${subCommand.name}' for option '$subCommandName'")

            try {
                if (!SlashUtils.hasPermissions(ctx, subCommand.permissions)) return true

                subCommand.execute(this, ctx)
            } catch (e: Exception) {
                SlashUtils.captureSlashCommandException(ctx, e, logger)

                return true
            }
            return true
        } catch (e: Exception) {
            logger.error("Exception getting KFunctions to handle subcommand $subCommandName", e)
            SlashUtils.captureSlashCommandException(ctx, e, logger)
            return false
        }
    }*/

    open suspend fun execute(ctx: SlashCommandContext) {
        if (!doChecks(ctx)) return

        handleInteraction(ctx)
    }

    companion object {
        private val log = LoggerFactory.getLogger(BaseSlashCommand::class.java)

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

            return finalList
        }
    }
}
