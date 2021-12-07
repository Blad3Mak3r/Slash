package tv.blademaker.slash.api.exceptions

import net.dv8tion.jda.api.Permission
import tv.blademaker.slash.api.PermissionTarget
import tv.blademaker.slash.api.SlashCommandContext
import java.lang.RuntimeException

class PermissionsLackException(
    val context: SlashCommandContext,
    val target: PermissionTarget,
    val permissions: Array<Permission>
) : RuntimeException()