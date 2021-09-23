package tv.blademaker.slash.api.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SlashCommandOption(
    val name: String = "",
    val group: String = ""
)
