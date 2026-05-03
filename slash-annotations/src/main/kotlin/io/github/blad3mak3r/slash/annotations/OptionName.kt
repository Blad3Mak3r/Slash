package io.github.blad3mak3r.slash.annotations

/**
 * Overrides the Discord option name used for a slash-command parameter.
 * By default the parameter's Kotlin name is used.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class OptionName(val value: String)
