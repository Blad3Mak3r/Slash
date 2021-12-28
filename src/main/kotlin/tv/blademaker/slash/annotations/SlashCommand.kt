package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlashCommand(
    val name: String = "",
    val group: String = "",
    val target: InteractionTarget = InteractionTarget.ALL
)
