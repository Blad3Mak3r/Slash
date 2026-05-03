package io.github.blad3mak3r.slash.context

import io.github.blad3mak3r.slash.client.SlashCommandClient
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.interactions.modals.ModalInteraction
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.coroutines.resume

class ModalContext(
    override val event: ModalInteractionEvent,
    override val client: SlashCommandClient
) : ModalInteraction by event,
    DeferrableInteraction,
    InteractionContext<ModalInteractionEvent> {

    override suspend fun acknowledge(ephemeral: Boolean) {
        if (event.isAcknowledged) return
        suspendCancellableCoroutine { cont ->
            event.deferReply(ephemeral).queue(
                { cont.resume(Unit) },
                { cont.cancel(it) }
            )
        }
    }
}
