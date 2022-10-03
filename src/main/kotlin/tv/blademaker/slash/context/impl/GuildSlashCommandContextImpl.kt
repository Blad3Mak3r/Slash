package tv.blademaker.slash.context.impl

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.context.GuildSlashCommandContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KFunction

class GuildSlashCommandContextImpl(
    override val event: SlashCommandInteractionEvent,
    override val function: KFunction<*>
) : GuildSlashCommandContext {
    override var extra: AtomicReference<Any?> = AtomicReference()
}