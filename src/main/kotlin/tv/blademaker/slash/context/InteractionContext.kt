package tv.blademaker.slash.context

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction

interface InteractionContext<E : GenericInteractionCreateEvent> {

    val event: E

    val interaction: Interaction
        get() = event.interaction

    val jda: JDA
        get() = event.jda

    val userLocale: DiscordLocale
        get() = event.userLocale

    val guildLocale: DiscordLocale?
        get() = if (event.isFromGuild) event.guildLocale else null

}