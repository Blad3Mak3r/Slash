package io.github.blad3mak3r.slash.registry

/**
 * Service interface implemented by KSP-generated registrar classes.
 *
 * Each class annotated with `@ApplicationCommand` causes the KSP processor to emit
 * a concrete `CommandRegistrar` that wires all handlers into a [HandlerRegistry].
 * The generated `META-INF/services` file makes every registrar discoverable via
 * [java.util.ServiceLoader].
 *
 * @param preconditionProvider Singleton registry used to resolve [Precondition]
 *   instances.  Generated registrars call [PreconditionProvider.bindIfAbsent] to
 *   register zero-arg defaults, then [PreconditionProvider.get] to obtain the
 *   (possibly user-supplied) instance.
 */
interface CommandRegistrar {
    fun register(registry: HandlerRegistry, preconditionProvider: PreconditionProvider)
}
