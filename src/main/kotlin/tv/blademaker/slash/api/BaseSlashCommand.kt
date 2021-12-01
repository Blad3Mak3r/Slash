package tv.blademaker.slash.api

import org.slf4j.LoggerFactory
import tv.blademaker.slash.api.annotations.Permissions
import tv.blademaker.slash.api.annotations.SlashCommandOption
import tv.blademaker.slash.internal.CommandExecutionCheck
import tv.blademaker.slash.internal.SlashUtils
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

abstract class BaseSlashCommand(val commandName: String) {

    private val checks: MutableList<CommandExecutionCheck> = mutableListOf()

    private val subCommands: List<SubCommand> = this::class.functions
        .filter { it.hasAnnotation<SlashCommandOption>() && it.visibility == KVisibility.PUBLIC && !it.isAbstract }
        .map { SubCommand(it) }

    @Suppress("unused")
    val paths: List<String> by lazy { generatePathForCommand(this) }

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

    private suspend fun handleSubCommand(ctx: SlashCommandContext): Boolean {
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
    }

    open suspend fun execute(ctx: SlashCommandContext) {
        if (!doChecks(ctx)) return
        if (handleSubCommand(ctx)) return

        handle(ctx)
    }

    open suspend fun handle(ctx: SlashCommandContext) {
        ctx.reply("Command not implemented.").setEphemeral(true).queue()
    }

    class SubCommand private constructor(
        private val handler: KFunction<*>,
        private val annotation: SlashCommandOption,
        val permissions: Permissions?
    ) {
        constructor(f: KFunction<*>) : this(f, f.findAnnotation<SlashCommandOption>()!!, f.findAnnotation<Permissions>())

        val name: String
            get() = annotation.name.takeIf { it.isNotBlank() } ?: handler.name

        val groupName: String
            get() = annotation.group

        suspend fun execute(instance: BaseSlashCommand, ctx: SlashCommandContext) = handler.callSuspend(instance, ctx)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BaseSlashCommand::class.java)

        private fun generatePathForCommand(command: BaseSlashCommand): List<String> {
            val list = mutableListOf<String>()

            list.add(command.commandName)

            command.subCommands.map {
                if (it.groupName.isNotBlank()) "${command.commandName}/${it.groupName}/${it.name}" else "${command.commandName}/${it.name}"
            }.forEach { list.add(it) }

            return list.sorted()
        }
    }
}
