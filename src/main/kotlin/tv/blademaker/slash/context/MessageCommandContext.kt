package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction

class MessageCommandContext(
    override val event: MessageContextInteractionEvent
) : InteractionContext<MessageContextInteractionEvent>, MessageContextInteraction by event