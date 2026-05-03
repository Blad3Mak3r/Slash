package io.github.blad3mak3r.slash.context

import io.github.blad3mak3r.slash.client.SlashCommandClient
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent

class AutoCompleteContext(
    override val event: CommandAutoCompleteInteractionEvent,
    override val client: SlashCommandClient
) : CommandAutoCompleteInteraction by event,
    InteractionContext<CommandAutoCompleteInteractionEvent>
