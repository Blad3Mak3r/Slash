package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.Interaction
import kotlin.reflect.KFunction

interface InteractionContext<E : GenericInteractionCreateEvent> {

    val event: E

    val function: KFunction<*>

    val interaction: Interaction
        get() = event.interaction

}