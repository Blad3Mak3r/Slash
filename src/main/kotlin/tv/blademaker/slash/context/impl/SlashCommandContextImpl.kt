package tv.blademaker.slash.context.impl

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.client.SlashCommandClient
import tv.blademaker.slash.context.SlashCommandContext
import java.util.concurrent.atomic.AtomicReference

class SlashCommandContextImpl(
    override val event: SlashCommandInteractionEvent
) : SlashCommandContext {

    override var extra: AtomicReference<Any?> = AtomicReference(null)
}