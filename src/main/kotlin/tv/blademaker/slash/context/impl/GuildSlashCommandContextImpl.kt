package tv.blademaker.slash.context.impl

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.context.GuildSlashCommandContext
import java.util.concurrent.atomic.AtomicReference

class GuildSlashCommandContextImpl(
    override val event: SlashCommandInteractionEvent
) : GuildSlashCommandContext {
    override var extra: AtomicReference<Any?> = AtomicReference()
}