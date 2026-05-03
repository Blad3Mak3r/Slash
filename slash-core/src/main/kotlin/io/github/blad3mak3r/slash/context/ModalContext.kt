package io.github.blad3mak3r.slash.context

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.modals.ModalInteraction
import io.github.blad3mak3r.slash.client.SlashCommandClient
import java.util.regex.Matcher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ModalContext(
    override val event: ModalInteractionEvent,
    override val client: SlashCommandClient,
    val matcher: Matcher
) : ModalInteraction by event, DeferrableInteraction, InteractionContext<ModalInteractionEvent> {

    override suspend fun acknowledge(ephemeral: Boolean) = suspendCoroutine { cont ->
        if (isAcknowledged) {
            cont.resume(Unit)
        } else {
            event.deferReply(ephemeral).queue({
                cont.resume(Unit)
            }, {
                cont.resumeWithException(it)
            })
        }
    }

    override suspend fun acknowledgeAsync(ephemeral: Boolean) = coroutineScope { async { acknowledge(ephemeral) } }
}
