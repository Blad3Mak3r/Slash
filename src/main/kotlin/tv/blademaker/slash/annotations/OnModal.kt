package tv.blademaker.slash.annotations

import java.util.regex.Pattern

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnModal(
    val modalId: String
)

fun OnModal.matcher(input: String) = Pattern.compile(modalId).matcher(input)
