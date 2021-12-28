package tv.blademaker.slash.context

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.CommandInteraction

interface InteractionContext<E : GenericInteractionCreateEvent> {

    val event: E

    val interaction: Interaction
        get() = event.interaction

    val jda: JDA
        get() = event.jda

}