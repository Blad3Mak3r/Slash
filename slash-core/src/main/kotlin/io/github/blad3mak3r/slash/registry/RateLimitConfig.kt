package io.github.blad3mak3r.slash.registry

/**
 * Holds the compiled rate-limit configuration for a command handler.
 *
 * Generated from the `@RateLimit` annotation at compile time.
 *
 * @param limit  Maximum number of invocations allowed inside the [period].
 * @param period Window length in milliseconds.
 */
data class RateLimitConfig(
    val limit: Int,
    val period: Long
)
