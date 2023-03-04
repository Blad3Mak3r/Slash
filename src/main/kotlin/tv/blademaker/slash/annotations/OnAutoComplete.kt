package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OnAutoComplete(
    val name: String = "",
    val group: String = "",
    val optionName: String
)
