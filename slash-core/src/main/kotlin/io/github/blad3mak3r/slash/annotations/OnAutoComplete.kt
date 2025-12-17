package io.github.blad3mak3r.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnAutoComplete(
    val name: String = "",
    val group: String = "",
    val optionName: String
)
