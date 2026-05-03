package io.github.blad3mak3r.slash.context

import io.github.blad3mak3r.slash.client.SlashCommandClient
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent

interface InteractionContext<E : GenericInteractionCreateEvent> {
    val event: E
    val client: SlashCommandClient
    val jda: JDA get() = event.jda
    val selfUser: User get() = event.jda.selfUser
    val interaction: Interaction get() = event.interaction
}
