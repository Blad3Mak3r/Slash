package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import kotlin.reflect.KFunction

class AutoCompleteContext(override val event: CommandAutoCompleteInteractionEvent, override val function: KFunction<*>) : InteractionContext<CommandAutoCompleteInteractionEvent> {

    override val interaction: CommandAutoCompleteInteraction
        get() = event.interaction

    fun String.getOptionOrNull(): OptionMapping? {
        return event.getOption(this)
    }

    fun getOption(name: String) = event.getOption(name)
}