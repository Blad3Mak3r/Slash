package tv.blademaker.slash.exceptions

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.PermissionTarget
import tv.blademaker.slash.SlashUtils.toHuman
import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.context.SlashCommandContext

class ExceptionHandlerImpl : ExceptionHandler {
    override fun onException(ex: Exception, command: BaseSlashCommand, event: SlashCommandInteractionEvent) {
        val message = "Exception executing handler for slash command `${event.commandPath}`:\n```\n${ex.message}\n```"

        log.error(message, ex)

        if (event.isAcknowledged) event.hook.sendMessage(message).setEphemeral(true).queue()
        else event.reply(message).setEphemeral(true).queue()
    }

    override fun onException(ex: Exception, command: BaseSlashCommand, event: CommandAutoCompleteInteractionEvent) {
        val message = "Exception executing handler for auto-complete interaction `${event.commandPath}`:\n```\n${ex.message}\n```"

        log.error(message, ex)
    }

    override fun onPermissionLackException(ex: PermissionsLackException) {
        when(ex.target) {
            PermissionTarget.BOT -> {
                val perms = ex.permissions.toHuman()
                log.warn("Bot doesn't have the required permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage("\uD83D\uDEAB The bot does not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
            PermissionTarget.USER -> {
                val perms = ex.permissions.toHuman()
                log.warn("User ${ex.context.user} doesn't have the required permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage("\uD83D\uDEAB You do not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
        }.setEphemeral(true).queue()
    }

    override fun onInteractionTargetMismatch(ex: InteractionTargetMismatch) {
        when (ex.target) {
            InteractionTarget.GUILD -> ex.context.replyMessage("This command cannot be used outside of a **space**.").queue()
            InteractionTarget.DM -> ex.context.replyMessage("This command cannot be used on a **space**.").queue()
            else -> throw IllegalStateException("Received InteractionTargetMismatch on a command with target InteractionTarget.ALL, report this to developer.")
        }
    }

    private val log = LoggerFactory.getLogger("ExceptionHandler")
}