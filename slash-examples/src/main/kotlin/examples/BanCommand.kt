package examples

import examples.preconditions.AdminOnly
import io.github.blad3mak3r.slash.annotations.*
import io.github.blad3mak3r.slash.context.GuildSlashCommandContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member

/**
 * /ban member:<Member> [reason:<String>]
 *
 * Covers: class-level @Permissions, @RateLimit, @Require, GUILD target,
 *         GuildSlashCommandContext, non-null Member param, nullable String param.
 */
@ApplicationCommand(name = "ban")
@Permissions([Permission.BAN_MEMBERS])
@RateLimit(limit = 3, period = 60_000L)
@Require(AdminOnly::class)
class BanCommand {

    @OnSlashCommand(target = InteractionTarget.GUILD)
    suspend fun handle(ctx: GuildSlashCommandContext, member: Member, reason: String?) {
        ctx.replyMessage(
            "Banned **${member.user.name}**. Reason: ${reason ?: "No reason provided."}"
        ).queue()
    }
}
