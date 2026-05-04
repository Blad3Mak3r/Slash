package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.OnSlashCommand
import io.github.blad3mak3r.slash.context.SlashCommandContext

/**
 * /greet name:<String> [times:<Long>]
 *
 * Covers: simple slash command, non-null String param, nullable Long param.
 */
@ApplicationCommand(name = "greet")
class GreetCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext, name: String, times: Long?) {
        val t = times ?: 1L
        ctx.replyMessage("Hello, $name! (×$t)").queue()
    }
}
