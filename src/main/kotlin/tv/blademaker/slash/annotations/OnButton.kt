package tv.blademaker.slash.annotations

import java.util.regex.Matcher
import java.util.regex.Pattern

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnButton(
    val buttonId: String
)

fun OnButton.matcher(input: String): Matcher = Pattern.compile(buttonId).matcher(input)
