package tv.blademaker.slash.context.impl

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.client.SlashCommandClient
import tv.blademaker.slash.context.SlashCommandContext
import java.util.concurrent.atomic.AtomicReference

class GuildSlashCommandContext(
    override val event: SlashCommandInteractionEvent
) : SlashCommandContext {
    override var extra: AtomicReference<Any?> = AtomicReference()

    override val guild: Guild
        get() = event.guild!!

    override val member: Member
        get() = event.member!!

    val selfMember: Member
        get() = guild.selfMember
}