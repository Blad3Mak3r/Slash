package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction
import tv.blademaker.slash.client.SlashCommandClient

class UserCommandContext(
    override val event: UserContextInteractionEvent, override val client: SlashCommandClient
) : InteractionContext<UserContextInteractionEvent>, UserContextInteraction by event