package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.OnSlashCommand
import io.github.blad3mak3r.slash.context.SlashCommandContext
import net.dv8tion.jda.api.entities.User

/**
 * /stats server info
 * /stats server members
 * /stats user [user:<User>]
 *
 * Covers: subcommands with group+name, subcommand with name only, nullable User param.
 */
@ApplicationCommand(name = "stats")
class StatsCommand {

    @OnSlashCommand(group = "server", name = "info")
    suspend fun serverInfo(ctx: SlashCommandContext) {
        ctx.replyMessage("Server: ${ctx.guild?.name ?: "DM"}").queue()
    }

    @OnSlashCommand(group = "server", name = "members")
    suspend fun serverMembers(ctx: SlashCommandContext) {
        ctx.replyMessage("Members: ${ctx.guild?.memberCount ?: 0}").queue()
    }

    @OnSlashCommand(name = "user")
    suspend fun userStats(ctx: SlashCommandContext, user: User?) {
        val target = user ?: ctx.user
        ctx.replyMessage("Stats for **${target.name}**").queue()
    }
}
