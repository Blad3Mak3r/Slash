package io.github.blad3mak3r.slash.annotations

/**
 * Marks a suspend function as the handler for a button interaction.
 *
 * @param pattern Regex pattern matched against the button's custom ID.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OnButton(val pattern: String)
