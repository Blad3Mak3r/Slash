package tv.blademaker.slash.api.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlashCommand(
    val name: String = "",
    val group: String = ""
)
