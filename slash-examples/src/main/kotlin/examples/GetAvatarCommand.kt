package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.CommandType
import io.github.blad3mak3r.slash.annotations.OnUserCommand
import io.github.blad3mak3r.slash.context.UserCommandContext

/**
 * User context-menu command "Get Avatar".
 *
 * Covers: CommandType.USER, @OnUserCommand, UserCommandContext.
 */
@ApplicationCommand(name = "Get Avatar", type = CommandType.USER)
class GetAvatarCommand {

    @OnUserCommand
    suspend fun handle(ctx: UserCommandContext) {
        val url = ctx.target.effectiveAvatarUrl
        ctx.reply("Avatar: $url").setEphemeral(true).queue()
    }
}
