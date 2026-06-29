package io.github.blad3mak3r.slash.annotations

/**
 * Marks a suspend function as the handler for a [CommandType.SLASH] command (or subcommand).
 *
 * @param name  Optional subcommand name.
 * @param group Optional subcommand-group name.
 * @param target The [InteractionTarget] this handler accepts.
 * @param supportDetached Whether the command supports detached guild messages.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OnSlashCommand(
    val name: String = "",
    val group: String = "",
    val target: InteractionTarget = InteractionTarget.GUILD,
    val supportDetached: Boolean = false
)
