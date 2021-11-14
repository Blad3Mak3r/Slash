package tv.blademaker.slash.api

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import java.util.concurrent.atomic.AtomicReference

class SlashCommandContextImpl(override val event: SlashCommandEvent) : SlashCommandContext {
    override var extra: AtomicReference<Any?> = AtomicReference(null)

    fun getExtra(): Any? {
        return extra.get()
    }
}