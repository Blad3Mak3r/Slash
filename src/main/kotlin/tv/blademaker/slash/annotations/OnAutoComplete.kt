package tv.blademaker.slash.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnAutoComplete(

    /**
     * The command full name.
     *
     * /ban -> ban
     * /mod bad -> mod ban
     */
    val fullName: String,
    val optionName: String
)
