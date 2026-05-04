package io.github.blad3mak3r.slash.annotations

/**
 * Marks a suspend function as the autocomplete handler for a slash command option.
 *
 * @param group Optional subcommand-group name (must match the parent [OnSlashCommand]).
 * @param name  Optional subcommand name (must match the parent [OnSlashCommand]).
 * @param option The option name this handler provides completions for.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OnAutoComplete(
    val group: String = "",
    val name: String = "",
    val option: String
)
