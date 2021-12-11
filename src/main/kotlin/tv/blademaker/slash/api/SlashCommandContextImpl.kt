package tv.blademaker.slash.api

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import tv.blademaker.slash.client.SlashCommandClient
import java.util.concurrent.atomic.AtomicReference

class SlashCommandContextImpl(override val commandClient: SlashCommandClient,
                              override val event: SlashCommandEvent) : SlashCommandContext {

    override var extra: AtomicReference<Any?> = AtomicReference(null)
}