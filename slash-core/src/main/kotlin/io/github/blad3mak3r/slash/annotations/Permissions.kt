package io.github.blad3mak3r.slash.annotations

import net.dv8tion.jda.api.Permission

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Permissions(
    val user: Array<Permission> = [],
    val bot: Array<Permission> = []
)
