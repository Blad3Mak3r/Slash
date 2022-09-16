package tv.blademaker.slash.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.Member

interface GuildSlashCommandContext : SlashCommandContext {
    override val guild: Guild
        get() = event.guild!!

    override val member: Member
        get() = event.member!!

    val selfMember: Member
        get() = guild.selfMember

    override val channel: GuildMessageChannel
        get() = event.guildChannel
}