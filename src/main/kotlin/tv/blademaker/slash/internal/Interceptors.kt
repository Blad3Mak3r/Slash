package tv.blademaker.slash.internal

import net.dv8tion.jda.api.Permission
import tv.blademaker.slash.PermissionTarget
import tv.blademaker.slash.annotations.Permissions
import tv.blademaker.slash.context.*
import tv.blademaker.slash.exceptions.PermissionsLackException

interface Interceptor<C : InteractionContext<*>> {
    suspend fun intercept(ctx: C): Boolean
}

interface SlashCommandInterceptor : Interceptor<SlashCommandContext>

interface MessageCommandInterceptor : Interceptor<MessageCommandContext>

interface UserCommandInterceptor : Interceptor<UserCommandContext>

internal object Interceptors {
    fun handlerPermissions(ctx: GuildSlashCommandContext, permissions: Permissions?) {
        if (!ctx.event.isFromGuild) return
        if (permissions == null || permissions.bot.isEmpty() && permissions.user.isEmpty()) return

        val missingPerms = mutableListOf<Permission>()

        // Check for the user permissions
        missingPerms.addAll(permissions.user.filterNot { ctx.member.hasPermission(it) })
        missingPerms.addAll(permissions.user.filterNot { ctx.member.hasPermission(ctx.channel, it) })

        if (missingPerms.isNotEmpty()) {
            throw PermissionsLackException(ctx, PermissionTarget.USER, missingPerms.toTypedArray())
        }

        // Check for the bot permissions
        missingPerms.addAll(permissions.bot.filterNot { ctx.selfMember.hasPermission(it) })
        missingPerms.addAll(permissions.bot.filterNot { ctx.selfMember.hasPermission(ctx.channel, it) })

        if (missingPerms.isNotEmpty()) {
            throw PermissionsLackException(ctx, PermissionTarget.BOT, missingPerms.toTypedArray())
        }
    }
}