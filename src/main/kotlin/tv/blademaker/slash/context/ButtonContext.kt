package tv.blademaker.slash.context

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonInteraction
import tv.blademaker.slash.client.SlashCommandClient
import java.util.regex.Matcher
import kotlin.reflect.KFunction

class ButtonContext(
    override val event: ButtonInteractionEvent,
    override val client: SlashCommandClient,
    val matcher: Matcher,
    override val function: KFunction<*>
) : ButtonInteraction by event, InteractionContext<ButtonInteractionEvent>, FunctionHandler