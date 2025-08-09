package io.github.blad3mak3r.slash.exceptions

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.annotations.InteractionTarget

class InteractionTargetMismatch(
    val event: SlashCommandInteractionEvent,
    val commandPath: String,
    val target: InteractionTarget
) : RuntimeException()