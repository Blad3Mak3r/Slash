package tv.blademaker.slash.api

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.api.annotations.Permissions
import tv.blademaker.slash.api.annotations.SlashCommandOption
import tv.blademaker.slash.internal.SlashUtils
import java.util.function.Predicate
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

abstract class BaseSlashCommand(val commandName: String) {

    private val checks: MutableList<Predicate<SlashCommandContext>> = mutableListOf()

    private val subCommands: List<SubCommand> = this::class.functions
        .filter { it.hasAnnotation<SlashCommandOption>() && it.visibility == KVisibility.PUBLIC && !it.isAbstract }
        .map { SubCommand(it) }

    private suspend fun doChecks(ctx: SlashCommandContext): Boolean {
        if (checks.isEmpty()) return true
        return checks.all { it.test(ctx) }
    }

    fun addChecks(check: Predicate<SlashCommandContext>) {
        if (checks.contains(check)) error("Check already registered.")
        checks.add(check)
    }

    fun removeChecks(check: Predicate<SlashCommandContext>) {
        if (!checks.contains(check)) error("Check is not registered.")
        checks.remove(check)
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
    }
}
