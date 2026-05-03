package io.github.blad3mak3r.slash.internal

import net.dv8tion.jda.api.Permission
import io.github.blad3mak3r.slash.annotations.PermissionTarget
import io.github.blad3mak3r.slash.context.*
import io.github.blad3mak3r.slash.exceptions.PermissionsLackException
import io.github.blad3mak3r.slash.registry.PermissionsConfig

interface Interceptor<C : InteractionContext<*>> {
    suspend fun intercept(ctx: C): Boolean
}

interface SlashCommandInterceptor : Interceptor<SlashCommandContext>

interface MessageCommandInterceptor : Interceptor<MessageCommandContext>

interface UserCommandInterceptor : Interceptor<UserCommandContext>

internal object Interceptors {

    fun handlerPermissions(ctx: GuildSlashCommandContext, permissions: PermissionsConfig?) {
        if (!ctx.event.isFromGuild) return
        if (permissions == null) return

        val missingPerms = mutableListOf<Permission>()

        when (permissions.target) {
            PermissionTarget.USER -> {
                missingPerms.addAll(permissions.permissions.filterNot { ctx.member.hasPermission(it) })
                missingPerms.addAll(permissions.permissions.filterNot { ctx.member.hasPermission(ctx.channel, it) })
                if (missingPerms.isNotEmpty())
                    throw PermissionsLackException(ctx, PermissionTarget.USER, missingPerms.toTypedArray())
            }
            PermissionTarget.BOT -> {
                missingPerms.addAll(permissions.permissions.filterNot { ctx.selfMember.hasPermission(it) })
                missingPerms.addAll(permissions.permissions.filterNot { ctx.selfMember.hasPermission(ctx.channel, it) })
                if (missingPerms.isNotEmpty())
                    throw PermissionsLackException(ctx, PermissionTarget.BOT, missingPerms.toTypedArray())
            }
        }
    }
}
