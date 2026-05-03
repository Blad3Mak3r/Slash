package io.github.blad3mak3r.slash.annotations

/**
 * Marks a suspend function as the handler for a [CommandType.MESSAGE] context-menu command.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class OnMessageCommand
