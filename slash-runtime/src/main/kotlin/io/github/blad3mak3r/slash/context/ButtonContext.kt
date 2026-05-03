package io.github.blad3mak3r.slash.context

import io.github.blad3mak3r.slash.client.SlashCommandClient
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class ButtonContext(
    override val event: ButtonInteractionEvent,
    override val client: SlashCommandClient
) : ButtonInteraction by event,
    InteractionContext<ButtonInteractionEvent>
