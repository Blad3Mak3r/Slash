package io.github.blad3mak3r.slash.exceptions

import kotlinx.coroutines.TimeoutCancellationException
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import kotlin.time.Duration

interface ExceptionHandler {

    fun wrap(e: Throwable, event: SlashCommandInteractionEvent) = when (e) {
        is PermissionsLackException -> onPermissionLackException(e)
        else -> onException(e, event)
    }

    fun onException(ex: Throwable, event: SlashCommandInteractionEvent)

    fun onException(ex: Throwable, event: CommandAutoCompleteInteractionEvent)

    fun onException(ex: Throwable, event: ModalInteractionEvent)

    fun onException(ex: Throwable, event: ButtonInteractionEvent)

    fun onException(ex: Throwable, event: MessageContextInteractionEvent)

    fun onException(ex: Throwable, event: UserContextInteractionEvent)

    fun onPermissionLackException(ex: PermissionsLackException)

    fun onTimeoutCancellationException(
        ex: TimeoutCancellationException,
        event: SlashCommandInteractionEvent,
        timeout: Duration
    )
}
