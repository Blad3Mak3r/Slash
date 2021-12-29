package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.context.impl.GuildSlashCommandContext

interface ContextCreator {

    suspend fun createGuildContext(event: SlashCommandInteractionEvent): GuildSlashCommandContext

    suspend fun createContext(event: SlashCommandInteractionEvent): SlashCommandContext
}