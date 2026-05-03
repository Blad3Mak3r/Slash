package io.github.blad3mak3r.slash.context

import io.github.blad3mak3r.slash.client.SlashCommandClient
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

open class GuildSlashCommandContext(
    client: SlashCommandClient,
    event: SlashCommandInteractionEvent
) : SlashCommandContext(client, event) {

    val guild: Guild get() = event.guild!!
    override val member: Member get() = event.member!!
    val selfMember: Member get() = guild.selfMember
    override val channel: GuildMessageChannel get() = event.guildChannel
}
