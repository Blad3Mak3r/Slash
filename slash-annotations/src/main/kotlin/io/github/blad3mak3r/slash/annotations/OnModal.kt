package io.github.blad3mak3r.slash.annotations

/**
 * Marks a suspend function as the handler for a modal submission.
 *
 * @param pattern Regex pattern matched against the modal's ID.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OnModal(val pattern: String)
