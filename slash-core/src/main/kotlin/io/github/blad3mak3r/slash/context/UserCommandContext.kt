package io.github.blad3mak3r.slash.context

import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.context.UserContextInteraction
import io.github.blad3mak3r.slash.client.SlashCommandClient

class UserCommandContext(
    override val event: UserContextInteractionEvent, override val client: SlashCommandClient
) : InteractionContext<UserContextInteractionEvent>, UserContextInteraction by event