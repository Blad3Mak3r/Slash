package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import tv.blademaker.slash.client.SlashCommandClient
import java.util.concurrent.atomic.AtomicReference

class SlashCommandContextImpl(
    override val commandClient: SlashCommandClient,
    override val event: SlashCommandInteractionEvent
) : SlashCommandContext {

    override var extra: AtomicReference<Any?> = AtomicReference(null)
    override fun getOptionOrNull(name: String): OptionMapping? {
        return event.getOption(name)
    }
}