package io.github.blad3mak3r.slash.registry

/**
 * Service interface implemented by KSP-generated registrar classes.
 *
 * Each class annotated with `@ApplicationCommand` causes the KSP processor to emit
 * a concrete `CommandRegistrar` that wires all handlers into a [HandlerRegistry].
 * The generated `META-INF/services` file makes every registrar discoverable via
 * [java.util.ServiceLoader].
 */
interface CommandRegistrar {
    fun register(registry: HandlerRegistry)
}
