package io.github.blad3mak3r.slash.registry

/**
 * Resolves [Precondition] instances by type, acting as a singleton registry.
 *
 * The provider guarantees that for each [Precondition] type there is at most one
 * instance, enabling dependency injection and shared state across commands.
 *
 * Usage — simple (no dependencies):
 * ```kotlin
 * SlashCommandClient.builder().buildWith(jda)
 * ```
 *
 * Usage — with custom instances / DI:
 * ```kotlin
 * SlashCommandClient.builder()
 *     .preconditions {
 *         bind<AdminOnly>(AdminOnly(database))
 *         bind<NotOnCooldown>(NotOnCooldown(redis))
 *     }
 *     .buildWith(jda)
 * ```
 */
interface PreconditionProvider {

    /**
     * Returns the registered instance for [type].
     * @throws IllegalStateException if [type] has not been registered.
     */
    fun <T : Precondition> get(type: Class<T>): T

    /** Registers [instance] for [type], replacing any previous registration. */
    fun <T : Precondition> bind(type: Class<T>, instance: T)

    /**
     * Registers an instance produced by [factory] for [type] **only if** no
     * instance has been registered yet.  Used by generated registrars to provide
     * zero-arg defaults while letting the user override them beforehand.
     */
    fun <T : Precondition> bindIfAbsent(type: Class<T>, factory: () -> T)
}

// ── Reified extension helpers ─────────────────────────────────────────────────

inline fun <reified T : Precondition> PreconditionProvider.bind(instance: T) =
    bind(T::class.java, instance)

inline fun <reified T : Precondition> PreconditionProvider.bindIfAbsent(noinline factory: () -> T) =
    bindIfAbsent(T::class.java, factory)
