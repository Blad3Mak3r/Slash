package tv.blademaker.slash.internal

import net.dv8tion.jda.api.entities.Member
import tv.blademaker.slash.PermissionTarget
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.annotations.Permissions
import tv.blademaker.slash.context.GuildSlashCommandContext
import tv.blademaker.slash.exceptions.PermissionsLackException

typealias ExecutionInterceptor = suspend (ctx: SlashCommandContext) -> Boolean

internal object Interceptors {
    fun handlerPermissions(ctx: GuildSlashCommandContext, permissions: Permissions?) {
        if (!ctx.event.isFromGuild) return
        if (permissions == null || permissions.bot.isEmpty() && permissions.user.isEmpty()) return

        var member: Member = ctx.member

        // Check for the user permissions
        var guildPerms = member.hasPermission(permissions.user.toList())
        var channelPerms = member.hasPermission(ctx.channel, permissions.user.toList())

        if (!(guildPerms && channelPerms)) {
            throw PermissionsLackException(ctx, PermissionTarget.USER, permissions.user)
        }

        // Check for the bot permissions
        member = ctx.selfMember
        guildPerms = member.hasPermission(permissions.bot.toList())
        channelPerms = member.hasPermission(ctx.channel, permissions.bot.toList())

        if (!(guildPerms && channelPerms)) {
            throw PermissionsLackException(ctx, PermissionTarget.BOT, permissions.bot)
        }
    }
}