package io.github.blad3mak3r.slash.exceptions

import net.dv8tion.jda.api.Permission
import io.github.blad3mak3r.slash.annotations.PermissionTarget
import io.github.blad3mak3r.slash.context.SlashCommandContext

class PermissionsLackException(
    val context: SlashCommandContext,
    val target: PermissionTarget,
    val permissions: Array<Permission>
) : RuntimeException()