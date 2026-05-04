package examples.preconditions

import io.github.blad3mak3r.slash.context.SlashCommandContext
import io.github.blad3mak3r.slash.registry.Precondition
import net.dv8tion.jda.api.Permission

/**
 * Example precondition that only lets server administrators through.
 */
class AdminOnly : Precondition {
    override suspend fun check(ctx: SlashCommandContext): Boolean {
        val member = ctx.member ?: return false
        return member.hasPermission(Permission.ADMINISTRATOR)
    }
}
