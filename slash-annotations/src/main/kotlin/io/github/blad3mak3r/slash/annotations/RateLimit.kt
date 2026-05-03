package io.github.blad3mak3r.slash.annotations

/**
 * Applies rate-limiting to the annotated command handler.
 *
 * @param limit  Maximum number of invocations allowed in the time window.
 * @param period Length of the time window in milliseconds.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class RateLimit(
    val limit: Int,
    val period: Long
)
