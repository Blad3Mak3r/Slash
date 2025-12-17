package io.github.blad3mak3r.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnSlashCommand(

    /**
     * Sub-command name.
     */
    val name: String = "",

    /**
     * Sub-command group name.
     */
    val group: String = "",
    /**
     * The [InteractionTarget] of the [OnSlashCommand] (default is InteractionTarget.GUILD)
     */
    val target: InteractionTarget = InteractionTarget.GUILD,

    /**
     * Whether the command supports being used in a detached context (e.g., DMs).
     */
    val supportDetached: Boolean = false
)
