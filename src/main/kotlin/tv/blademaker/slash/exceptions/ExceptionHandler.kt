package tv.blademaker.slash.exceptions

import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.MessageCommand
import tv.blademaker.slash.UserCommand
import kotlin.time.Duration

interface ExceptionHandler {

    fun wrap(e: Throwable, command: BaseSlashCommand, event: SlashCommandInteractionEvent) = when(e) {
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, command, event)
    }

    fun wrap(e: Throwable, command: BaseSlashCommand, event: CommandAutoCompleteInteractionEvent) = when(e) {
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, command, event)
    }

    fun onException(ex: Throwable, command: BaseSlashCommand, event: SlashCommandInteractionEvent)

    fun onException(ex: Throwable, command: BaseSlashCommand, event: CommandAutoCompleteInteractionEvent)

    fun onException(ex: Throwable, command: BaseSlashCommand, event: ModalInteractionEvent)

    fun onException(ex: Throwable, command: BaseSlashCommand, event: ButtonInteractionEvent)

    fun onException(ex: Throwable, command: MessageCommand, event: MessageContextInteractionEvent)

    fun onException(ex: Throwable, command: UserCommand, event: UserContextInteractionEvent)

    fun onPermissionLackException(ex: PermissionsLackException)

    fun onTimeoutCancellationException(ex: TimeoutCancellationException, event: SlashCommandInteractionEvent, timeout: Duration)
}