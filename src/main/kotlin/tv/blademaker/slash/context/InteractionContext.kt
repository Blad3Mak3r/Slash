package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.Interaction

interface InteractionContext<E : GenericInteractionCreateEvent> {

    val event: E

    val interaction: Interaction
        get() = event.interaction

}