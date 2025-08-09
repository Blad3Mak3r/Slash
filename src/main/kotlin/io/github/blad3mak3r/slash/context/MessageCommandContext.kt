package io.github.blad3mak3r.slash.context

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.context.MessageContextInteraction
import tv.blademaker.slash.client.SlashCommandClient

class MessageCommandContext(
    override val event: MessageContextInteractionEvent,
    override val client: SlashCommandClient
) : InteractionContext<MessageContextInteractionEvent>, MessageContextInteraction by event