package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction
import kotlin.reflect.KFunction

class AutoCompleteContext(
    override val event: CommandAutoCompleteInteractionEvent,
    override val function: KFunction<*>
) : CommandAutoCompleteInteraction by event, InteractionContext<CommandAutoCompleteInteractionEvent>, FunctionHandler