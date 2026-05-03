package io.github.blad3mak3r.slash.dsl

/**
 * Singleton that accumulates [CommandDef] instances registered via the [command] DSL function.
 *
 * The Gradle plugin loads compiled slash-def classes via [ClassLoader], which triggers the
 * static initialisers of every top-level `*Kt` class, causing each `command { }` call to
 * register itself here. The plugin then retrieves all defs via [getCommands].
 */
object CommandRegistry {
    private val _commands = mutableListOf<CommandDef>()

    val commands: List<CommandDef> get() = _commands.toList()

    fun register(def: CommandDef): CommandDef {
        _commands.add(def)
        return def
    }

    fun clear() = _commands.clear()
}
