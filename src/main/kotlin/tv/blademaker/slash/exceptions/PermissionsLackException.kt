package tv.blademaker.slash.exceptions

import net.dv8tion.jda.api.Permission
import tv.blademaker.slash.PermissionTarget
import tv.blademaker.slash.context.SlashCommandContext
import java.lang.RuntimeException

class PermissionsLackException(
    val context: SlashCommandContext,
    val target: PermissionTarget,
    val permissions: Array<Permission>
) : RuntimeException()