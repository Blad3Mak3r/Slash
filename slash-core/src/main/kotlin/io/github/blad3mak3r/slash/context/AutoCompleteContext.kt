package io.github.blad3mak3r.slash.context

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandAutoCompleteInteraction
import io.github.blad3mak3r.slash.client.SlashCommandClient
import kotlin.reflect.KFunction

class AutoCompleteContext(
    override val event: CommandAutoCompleteInteractionEvent,
    override val client: SlashCommandClient,
    override val function: KFunction<*>
) : CommandAutoCompleteInteraction by event, InteractionContext<CommandAutoCompleteInteractionEvent>, FunctionHandler