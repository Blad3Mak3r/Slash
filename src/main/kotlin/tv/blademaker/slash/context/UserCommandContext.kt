package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction

class UserCommandContext(
    override val event: UserContextInteractionEvent
) : InteractionContext<UserContextInteractionEvent>, UserContextInteraction by event