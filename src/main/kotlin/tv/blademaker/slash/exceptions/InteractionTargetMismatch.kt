package tv.blademaker.slash.exceptions

import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.context.SlashCommandContext
import java.lang.RuntimeException

class InteractionTargetMismatch(
    val context: SlashCommandContext,
    val commandPath: String,
    val target: InteractionTarget
) : RuntimeException()