package tv.blademaker.slash.api.annotations

/**
 * Define a custom name for a command event option, this means you can use a different variable name
 * to get an option with different name.
 *
 * @param value The custom name for the option
 *
 * @since 0.4.2
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class OptionName(val value: String)
