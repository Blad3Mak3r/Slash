package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnSlashCommand(

    /**
     * The command full name.
     *
     * /ban -> ban
     * /mod bad -> mod ban
     */
    val fullName: String,
    /**
     * The [InteractionTarget] of the [OnSlashCommand] (default is InteractionTarget.GUILD)
     */
    val target: InteractionTarget = InteractionTarget.GUILD
)
