package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.reflect.KFunction

interface ContextCreator {

    suspend fun createGuildContext(event: SlashCommandInteractionEvent, function: KFunction<*>): GuildSlashCommandContext {
        return SlashCommandContext.guild(event, function)
    }

    suspend fun createContext(event: SlashCommandInteractionEvent, function: KFunction<*>): SlashCommandContext {
        return SlashCommandContext.impl(event, function)
    }
}