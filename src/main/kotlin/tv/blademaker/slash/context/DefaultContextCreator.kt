package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.context.impl.GuildSlashCommandContext
import tv.blademaker.slash.context.impl.SlashCommandContextImpl

class DefaultContextCreator : ContextCreator {
    override suspend fun createGuildContext(event: SlashCommandInteractionEvent): GuildSlashCommandContext {
        return GuildSlashCommandContext(event)
    }

    override suspend fun createContext(event: SlashCommandInteractionEvent): SlashCommandContext {
        return SlashCommandContextImpl(event)
    }
}