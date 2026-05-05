package io.github.blad3mak3r.slash.registry

/**
 * Default in-memory implementation of [PreconditionProvider].
 *
 * Stores one instance per [Precondition] type in a plain [HashMap].
 * Thread-safety is intentionally omitted: registration happens once at startup
 * (inside [io.github.blad3mak3r.slash.client.SlashCommandClient] construction)
 * and all reads occur afterwards, so no concurrent writes can take place.
 */
class DefaultPreconditionProvider : PreconditionProvider {

    private val instances = HashMap<Class<out Precondition>, Precondition>()

    override fun <T : Precondition> bind(type: Class<T>, instance: T) {
        instances[type] = instance
    }

    override fun <T : Precondition> bindIfAbsent(type: Class<T>, factory: () -> T) {
        if (type !in instances) instances[type] = factory()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Precondition> get(type: Class<T>): T =
        instances[type] as? T
            ?: error("Precondition ${type.simpleName} is not registered in the PreconditionProvider.")
}
