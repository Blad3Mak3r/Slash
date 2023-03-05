package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction
import kotlin.reflect.KFunction

class UserContext(
    override val event: UserContextInteractionEvent,
    override val function: KFunction<*>
) : InteractionContext<UserContextInteractionEvent>, UserContextInteraction by event