package io.github.blad3mak3r.slash.exceptions

import io.github.blad3mak3r.slash.SlashUtils.toHuman
import io.github.blad3mak3r.slash.annotations.PermissionTarget
import io.github.blad3mak3r.slash.extensions.commandPath
import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import kotlin.time.Duration

class ExceptionHandlerImpl : ExceptionHandler {

    override fun onException(ex: Throwable, event: ModalInteractionEvent) {
        val message = "Exception executing handler for modal `${event.modalId}`:\n```\n${ex.message}\n```"
        log.error(message, ex)
        if (event.isAcknowledged) event.hook.sendMessage(message).setEphemeral(true).queue()
        else event.reply(message).setEphemeral(true).queue()
    }

    override fun onException(ex: Throwable, event: SlashCommandInteractionEvent) {
        val message = "Exception executing handler for slash command `${event.commandPath}`:\n```\n${ex.message}\n```"
        log.error(message, ex)
        if (event.isAcknowledged) event.hook.sendMessage(message).setEphemeral(true).queue()
        else event.reply(message).setEphemeral(true).queue()
    }

    override fun onException(ex: Throwable, event: CommandAutoCompleteInteractionEvent) {
        log.error("Exception executing handler for auto-complete `${event.commandPath}`", ex)
    }

    override fun onException(ex: Throwable, event: ButtonInteractionEvent) {
        log.error("Exception executing handler for button interaction `${event.button.customId}`", ex)
    }

    override fun onException(ex: Throwable, event: MessageContextInteractionEvent) {
        log.error("Exception executing handler for Message Command `${event.fullCommandName}`", ex)
    }

    override fun onException(ex: Throwable, event: UserContextInteractionEvent) {
        log.error("Exception executing handler for User Command `${event.fullCommandName}`", ex)
    }

    override fun onPermissionLackException(ex: PermissionsLackException) {
        when (ex.target) {
            PermissionTarget.BOT -> {
                val perms = ex.permissions.toHuman()
                log.warn("Bot is missing permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage(
                    "\uD83D\uDEAB The bot does not have the necessary permissions to carry out this action." +
                    "\nRequired permissions: **$perms**."
                )
            }
            PermissionTarget.USER -> {
                val perms = ex.permissions.toHuman()
                log.warn("User ${ex.context.user} is missing permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage(
                    "\uD83D\uDEAB You do not have the necessary permissions to carry out this action." +
                    "\nRequired permissions: **$perms**."
                )
            }
        }.setEphemeral(true).queue()
    }

    override fun onTimeoutCancellationException(
        ex: TimeoutCancellationException,
        event: SlashCommandInteractionEvent,
        timeout: Duration
    ) {
        val message =
            "\uD83D\uDEAB It was not possible to complete the command within ${timeout.inWholeSeconds} seconds."
        when (event.isAcknowledged) {
            true  -> event.hook.sendMessage(message).setEphemeral(true)
            false -> event.reply(message).setEphemeral(true)
        }.queue()
    }

    private val log = LoggerFactory.getLogger("ExceptionHandler")
}
