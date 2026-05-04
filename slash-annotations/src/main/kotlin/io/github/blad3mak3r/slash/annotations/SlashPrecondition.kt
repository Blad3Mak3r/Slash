package io.github.blad3mak3r.slash.annotations

/**
 * Marker interface for precondition classes.
 *
 * Implement this interface in `slash-core` via [io.github.blad3mak3r.slash.registry.Precondition]
 * which adds the actual `suspend fun check(ctx)` contract.
 *
 * Kept in `slash-annotations` so that [@Require] can reference it without
 * pulling in `slash-core` as a compile-time dependency.
 */
interface SlashPrecondition
