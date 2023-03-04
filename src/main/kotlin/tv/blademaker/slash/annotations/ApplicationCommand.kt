package tv.blademaker.slash.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ApplicationCommand(
    val commandName: String,
    val description: String
)
