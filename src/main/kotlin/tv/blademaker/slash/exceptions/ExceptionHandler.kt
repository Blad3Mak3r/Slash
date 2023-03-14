package tv.blademaker.slash.exceptions

import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import tv.blademaker.slash.internal.*
import kotlin.time.Duration

interface ExceptionHandler {

    fun wrap(e: Throwable, handler: SlashCommandHandler, event: SlashCommandInteractionEvent) = when(e) {
        is InteractionTargetMismatch -> onInteractionTargetMismatch(e)
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, handler, event)
    }

    fun wrap(e: Throwable, handler: AutoCompleteHandler, event: CommandAutoCompleteInteractionEvent) = when(e) {
        is InteractionTargetMismatch -> onInteractionTargetMismatch(e)
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, handler, event)
    }

    fun onException(ex: Throwable, handler: SlashCommandHandler, event: SlashCommandInteractionEvent)

    fun onException(ex: Throwable, handler: AutoCompleteHandler, event: CommandAutoCompleteInteractionEvent)

    fun onException(ex: Throwable, handler: ModalHandler, event: ModalInteractionEvent)

    fun onException(ex: Throwable, handler: ButtonHandler, event: ButtonInteractionEvent)

    fun onPermissionLackException(ex: PermissionsLackException)

    fun onInteractionTargetMismatch(ex: InteractionTargetMismatch)

    fun onTimeoutCancellationException(ex: TimeoutCancellationException, event: SlashCommandInteractionEvent, timeout: Duration)
}