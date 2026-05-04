package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.CommandType
import io.github.blad3mak3r.slash.annotations.OnMessageCommand
import io.github.blad3mak3r.slash.context.MessageCommandContext

/**
 * Message context-menu command "Quote".
 *
 * Covers: CommandType.MESSAGE, @OnMessageCommand, MessageCommandContext.
 */
@ApplicationCommand(name = "Quote", type = CommandType.MESSAGE)
class QuoteCommand {

    @OnMessageCommand
    suspend fun handle(ctx: MessageCommandContext) {
        val content = ctx.target.contentRaw
        ctx.reply("📌 *\"$content\"*").setEphemeral(true).queue()
    }
}
