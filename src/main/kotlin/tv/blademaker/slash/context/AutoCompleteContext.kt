package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction
import net.dv8tion.jda.api.interactions.commands.OptionMapping

class AutoCompleteContext(val event: CommandAutoCompleteInteractionEvent) : InteractionContext, CommandAutoCompleteInteraction by event {
    override fun getOptionOrNull(name: String): OptionMapping? {
        return super.getOption(name)
    }
}