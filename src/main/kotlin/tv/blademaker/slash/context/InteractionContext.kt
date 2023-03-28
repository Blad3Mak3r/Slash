package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.Interaction
import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.client.SlashCommandClient
import kotlin.time.Duration

interface InteractionContext<E : GenericInteractionCreateEvent> {

    val event: E

    val client: SlashCommandClient

    val interaction: Interaction
        get() = event.interaction

}

suspend inline fun <reified E : GenericEvent> InteractionContext<*>.await(
    timeout: Duration,
    crossinline filter: suspend (event: E) -> Boolean
) = SlashUtils.await(client.events, timeout, filter)