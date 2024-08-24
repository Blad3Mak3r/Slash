package tv.blademaker.slash.annotations

import java.util.regex.Matcher
import java.util.regex.Pattern

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnModal(
    val modalId: String
)

fun OnModal.matcher(input: String): Matcher = Pattern.compile(modalId).matcher(input)
