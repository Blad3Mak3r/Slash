package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

interface ContextCreator {

    suspend fun createGuildContext(event: SlashCommandInteractionEvent): GuildSlashCommandContext {
        return SlashCommandContext.guild(event)
    }

    suspend fun createContext(event: SlashCommandInteractionEvent): SlashCommandContext {
        return SlashCommandContext.impl(event)
    }
}