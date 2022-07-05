package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlashCommand(

    /**
     * Sub-command name.
     */
    val name: String = "",

    /**
     * Sub-command group name.
     */
    val group: String = "",
    /**
     * The [InteractionTarget] of the [SlashCommand] (default is InteractionTarget.GUILD)
     */
    val target: InteractionTarget = InteractionTarget.GUILD
)
