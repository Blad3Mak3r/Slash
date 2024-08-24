package tv.blademaker.slash.context

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.modals.ModalInteraction
import tv.blademaker.slash.client.SlashCommandClient
import java.util.regex.Matcher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction

class ModalContext(
    override val event: ModalInteractionEvent,
    override val client: SlashCommandClient,
    val matcher: Matcher,
    override val function: KFunction<*>
) : ModalInteraction by event, DeferrableInteraction, InteractionContext<ModalInteractionEvent>, FunctionHandler {

    /**
     * Automatically detect if the interaction is already acknowledge and if not
     * will acknowledge it.
     *
     *
     */
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