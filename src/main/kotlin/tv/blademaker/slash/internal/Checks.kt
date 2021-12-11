package tv.blademaker.slash.internal

import net.dv8tion.jda.api.entities.Member
import tv.blademaker.slash.api.PermissionTarget
import tv.blademaker.slash.api.SlashCommandContext
import tv.blademaker.slash.api.annotations.Permissions
import tv.blademaker.slash.api.exceptions.PermissionsLackException

typealias CommandExecutionCheck = suspend (ctx: SlashCommandContext) -> Boolean

internal object Checks {
    fun commandPermissions(ctx: SlashCommandContext, permissions: Permissions?) {
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