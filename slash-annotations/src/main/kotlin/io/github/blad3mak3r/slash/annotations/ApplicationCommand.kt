package io.github.blad3mak3r.slash.annotations

/**
 * Marks a class as an Application Command handler.
 *
 * @param name The command name as registered in Discord.
 * @param type The command type. Defaults to [CommandType.SLASH].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ApplicationCommand(
    val name: String,
    val type: CommandType = CommandType.SLASH
)
