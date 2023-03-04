package tv.blademaker.slash.exceptions

import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.PermissionTarget
import tv.blademaker.slash.SlashUtils.toHuman
import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.extensions.commandPath
import kotlin.time.Duration

class ExceptionHandlerImpl : ExceptionHandler {

    override fun onException(ex: Throwable, command: BaseSlashCommand, event: ModalInteractionEvent) {
        val message = "Exception executing handler for modal `${event.modalId}`:\n```\n${ex.message}\n```"

        log.error(message, ex)

        if (event.isAcknowledged) event.hook.sendMessage(message).setEphemeral(true).queue()
        else event.reply(message).setEphemeral(true).queue()
    }

    override fun onException(ex: Throwable, command: BaseSlashCommand, event: SlashCommandInteractionEvent) {
        val message = "Exception executing handler for slash command `${event.commandPath}`:\n```\n${ex.message}\n```"

        log.error(message, ex)

        if (event.isAcknowledged) event.hook.sendMessage(message).setEphemeral(true).queue()
        else event.reply(message).setEphemeral(true).queue()
    }

    override fun onException(ex: Throwable, command: BaseSlashCommand, event: CommandAutoCompleteInteractionEvent) {
        val message = "Exception executing handler for auto-complete interaction `${event.commandPath}`:\n```\n${ex.message}\n```"

        log.error(message, ex)
    }

    override fun onException(ex: Throwable, command: BaseSlashCommand, event: ButtonInteractionEvent) {
        val message = "Exception executing handler for button interaction `${event.button.id}`:\n```\n${ex.message}\n```"

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
            InteractionTarget.GUILD -> ex.context.replyMessage("This command cannot be used outside of a **Guild**.").queue()
            InteractionTarget.DM -> ex.context.replyMessage("This command cannot be used on a **Guild**.").queue()
            else -> throw IllegalStateException("Received InteractionTargetMismatch on a command with target InteractionTarget.ALL, report this to developer.")
        }
    }

    override fun onTimeoutCancellationException(
        ex: TimeoutCancellationException,
        event: SlashCommandInteractionEvent,
        timeout: Duration
    ) {
        val message = "\uD83D\uDEAB It has not been possible to complete the execution of the command in the estimated time of ${timeout.inWholeSeconds} seconds."

        when (event.isAcknowledged) {
            true -> event.hook.sendMessage(message).setEphemeral(true)
            false -> event.reply(message).setEphemeral(true)
        }.queue()
    }

    private val log = LoggerFactory.getLogger("ExceptionHandler")
}