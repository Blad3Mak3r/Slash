package tv.blademaker.slash.exceptions

import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.BaseSlashCommand
import kotlin.time.Duration

interface ExceptionHandler {

    fun wrap(e: Throwable, command: BaseSlashCommand, event: SlashCommandInteractionEvent) = when(e) {
        is InteractionTargetMismatch -> onInteractionTargetMismatch(e)
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, command, event)
    }

    fun wrap(e: Throwable, command: BaseSlashCommand, event: CommandAutoCompleteInteractionEvent) = when(e) {
        is InteractionTargetMismatch -> onInteractionTargetMismatch(e)
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, command, event)
    }

    fun onException(ex: Throwable, command: BaseSlashCommand, event: SlashCommandInteractionEvent)

    fun onException(ex: Throwable, command: BaseSlashCommand, event: CommandAutoCompleteInteractionEvent)

    fun onException(ex: Throwable, command: BaseSlashCommand, event: ModalInteractionEvent)

    fun onPermissionLackException(ex: PermissionsLackException)

    fun onInteractionTargetMismatch(ex: InteractionTargetMismatch)

    fun onTimeoutCancellationException(ex: TimeoutCancellationException, event: SlashCommandInteractionEvent, timeout: Duration)
}