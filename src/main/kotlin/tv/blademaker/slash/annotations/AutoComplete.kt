package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoComplete(
    val name: String = "",
    val group: String = "",
    val optionName: String
)
