package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.OnAutoComplete
import io.github.blad3mak3r.slash.annotations.OnSlashCommand
import io.github.blad3mak3r.slash.context.AutoCompleteContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import net.dv8tion.jda.api.interactions.commands.Command

/**
 * /color color:<String>
 *
 * Covers: @OnAutoComplete with an injected String parameter (mirrors
 * real-world usage where the focused option value is passed as a param
 * instead of reading ctx.focusedOption.value manually).
 */
@ApplicationCommand(name = "color")
class ColorCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext, color: String) {
        ctx.replyMessage("You picked: **$color**").queue()
    }

    /**
     * The processor resolves `color` from ctx.getOption("color")!!.asString
     * and passes it as an argument — same pattern as MemeSlashCommand.complete().
     */
    @OnAutoComplete(option = "color")
    suspend fun completeColor(ctx: AutoCompleteContext, color: String) {
        val choices = listOf("Red", "Green", "Blue", "Yellow", "Purple", "Orange")
            .filter { it.startsWith(color, ignoreCase = true) }
            .map { Command.Choice(it, it) }
        ctx.replyChoices(choices).queue()
    }
}
