package io.github.blad3mak3r.slash.annotations

import net.dv8tion.jda.api.Permission

/**
 * Declares the Discord permissions required to execute this command.
 * Applied to the command class or to individual handler functions.
 *
 * @param value Required [Permission]s.
 * @param target Whether permissions are checked for the [PermissionTarget.USER] (default)
 *               or the [PermissionTarget.BOT].
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Permissions(
    val value: Array<Permission>,
    val target: PermissionTarget = PermissionTarget.USER
)
