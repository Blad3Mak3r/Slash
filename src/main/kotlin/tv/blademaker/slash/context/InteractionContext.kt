package tv.blademaker.slash.context

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.InteractionHook
import kotlin.reflect.KFunction

interface InteractionContext<E : GenericInteractionCreateEvent> {

    val event: E

    val function: KFunction<*>

    val interaction: Interaction
        get() = event.interaction

    val isAcknowledged: Boolean
        get() = interaction.isAcknowledged

    val isFromGuild: Boolean
        get() = interaction.isFromGuild

    val guild: Guild?
        get() = event.guild

    val jda: JDA
        get() = event.jda

    val userLocale: DiscordLocale
        get() = event.userLocale

    val guildLocale: DiscordLocale?
        get() = if (event.isFromGuild) event.guildLocale else null

}