package tv.blademaker.slash.context

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.modals.ModalInteraction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.util.regex.Matcher
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction

class ModalContext(override val event: ModalInteractionEvent, val matcher: Matcher, override val function: KFunction<*>) : InteractionContext<ModalInteractionEvent> {
    val hook: InteractionHook
        get() = event.hook

    override val interaction: ModalInteraction
        get() = event.interaction

    fun tryAcknowledge(ephemeral: Boolean = false): ReplyCallbackAction {
        if (isAcknowledged) throw IllegalStateException("Current command is already ack.")
        return event.deferReply(ephemeral)
    }

    /**
     * Automatically detect if the interaction is already acknowledge and if not
     * will acknowledge it.
     *
     *
     */
    suspend fun acknowledge(ephemeral: Boolean = false) = suspendCoroutine<Unit> { cont ->
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

    suspend fun acknowledgeAsync(ephemeral: Boolean) = coroutineScope { async { acknowledge(ephemeral) } }
}