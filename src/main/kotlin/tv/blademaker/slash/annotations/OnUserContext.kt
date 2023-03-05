package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnUserContext(
    /**
     * Sub-command name.
     */
    val name: String = "",

    /**
     * Sub-command group name.
     */
    val group: String = ""
)
