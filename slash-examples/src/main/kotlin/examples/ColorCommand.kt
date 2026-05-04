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
 * Covers: @OnAutoComplete, autocomplete handler providing dynamic choices.
 */
@ApplicationCommand(name = "color")
class ColorCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext, color: String) {
        ctx.replyMessage("You picked: **$color**").queue()
    }

    @OnAutoComplete(option = "color")
    suspend fun completeColor(ctx: AutoCompleteContext) {
        val input = ctx.focusedOption.value
        val choices = listOf("Red", "Green", "Blue", "Yellow", "Purple", "Orange")
            .filter { it.startsWith(input, ignoreCase = true) }
            .map { Command.Choice(it, it) }
        ctx.replyChoices(choices).queue()
    }
}
