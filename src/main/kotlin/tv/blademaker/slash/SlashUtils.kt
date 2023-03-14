package tv.blademaker.slash

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import tv.blademaker.slash.annotations.OnAutoComplete
import tv.blademaker.slash.annotations.OnButton
import tv.blademaker.slash.annotations.OnModal
import tv.blademaker.slash.annotations.OnSlashCommand
import tv.blademaker.slash.internal.*
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.*
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

object SlashUtils {

    internal val log = LoggerFactory.getLogger("Slash")

    /**
     * Convert an [Array] of [Permission] in a readable list.
     *
     *  @param jump If jump is true the separator will be ``\n`` instead of ``,``.
     *
     *  @return The readable list as [String]
     */
    fun Array<Permission>.toHuman(jump: Boolean = false): String {
        return this.joinToString(if (jump) "\n" else ", ") { it.getName() }
    }

    private inline fun <reified A : Annotation> Reflections.getKotlinFunctionsAnnotatedWith(annotation: Class<A>): Map<A, KFunction<*>> {
        return getMethodsAnnotatedWith(annotation)
            .mapNotNull { it.kotlinFunction }
            .associateBy { it.findAnnotation()!! }
    }

    private inline fun <reified A : Annotation> Map<A, KFunction<*>>.runFilters(): Map<A, KFunction<*>> {
        for (item in this) {
            require(!item.value.isAbstract) {
                "${item.value.name} is abstract"
            }
            require(item.value.visibility == KVisibility.PUBLIC) {
                "${item.value.name} is not public"
            }
            require(!item.value.isInline) {
                "${item.value.name} is inline"
            }
            require(item.value.isAccessible) {
                "${item.value.name} is not accesible"
            }
        }
        return this
    }

    /**
     * Discover the [Handler] functions inside the package.
     *
     * @param packageName The package to lookup.
     *
     * @throws IllegalStateException When you try to register more than 1 command with the same name,
     * a command that contains a default handler with sub-commands handlers, a command contains more
     * than 1 handler for the same command path (command/group/subcommand) or a command does not contain
     * handlers.
     *
     * @throws NoSuchMethodException When cannot initialize a command class with no empty constructor.
     *
     * @see java.lang.Class.getDeclaredConstructor
     * @see java.lang.reflect.Constructor.newInstance
     *
     * @return A [DiscoveryResult] with the elapsed time to discover commands, the count of commands
     * discovered and the commands itself.
     */
    fun discoverSlashCommands(packageName: String): DiscoveryResult {
        val start = System.nanoTime()

        val init = Reflections(packageName, Scanners.MethodsAnnotated)

        val onAutoCompleteHandlers = init.getKotlinFunctionsAnnotatedWith(OnAutoComplete::class.java)
            .runFilters()
            .compileAutoCompleteHandlers()

        val onSlashCommandsHandlers = init.getKotlinFunctionsAnnotatedWith(OnSlashCommand::class.java)
            .runFilters()
            .compileSlashCommands()

        val onButtonsHandlers = init.getKotlinFunctionsAnnotatedWith(OnButton::class.java)
            .runFilters()

        val onModalsHandlers = init.getKotlinFunctionsAnnotatedWith(OnModal::class.java)
            .runFilters()

        val elapsedTime = (System.nanoTime() - start) / 1_000_000
        log.info("Slash discovery results (${elapsedTime}ms):")
        log.info("@OnAutoComplete   : ${onAutoCompleteHandlers.size}")
        log.info("@OnButton         : ${onButtonsHandlers.size}")
        log.info("@OnModal          : ${onModalsHandlers.size}")
        log.info("@OnSlashCommand   : ${onSlashCommandsHandlers.size}")

        return DiscoveryResult(
            onAutoComplete = onAutoCompleteHandlers,
            onButton = emptyList(),
            onModal = emptyList(),
            onSlashCommand = onSlashCommandsHandlers
        )
    }

    @Suppress("unused")
    fun RestAction<*>.asEphemeral(): RestAction<*> {
        when(this) {
            is ReplyCallbackAction -> this.setEphemeral(true)
            is WebhookMessageCreateAction -> this.setEphemeral(true)
        }

        return this
    }

    private fun Map<OnSlashCommand, KFunction<*>>.compileSlashCommands(): List<SlashCommandHandler> {

        val handlers = mutableListOf<SlashCommandHandler>()

        for (item in this) {
            check(!handlers.any { it.annotation.fullName.equals(item.key.fullName, true) }) {
                "Found more than one SlashCommandHandler for the same command ${item.key.fullName} (${item.value})"
            }
            handlers.add(SlashCommandHandler(item))
        }

        return handlers
    }

    private fun Map<OnAutoComplete, KFunction<*>>.compileAutoCompleteHandlers(): List<AutoCompleteHandler> {
        val handlers = mutableListOf<AutoCompleteHandler>()

        for (item in this) {
            check(!handlers.any { it.annotation.fullName == item.key.fullName && it.optionName == item.key.optionName }) {
                "Found more than one AutocompleteHandler for the same command (${item.key.fullName}) and option (${item.key.optionName})."
            }
            handlers.add(AutoCompleteHandler(item))
        }

        return handlers
    }



    internal fun handlerToString(handler: Handler<*, *>) = buildString {
        append("@")
        append(handler.annotation::class.java.simpleName)
        append("[")
        append(handler.function.name)
        append("(")
        append(handler.function.valueParameters.joinToString(", ") {
            "${if (it.isVararg) "vararg" else ""} ${it.name}: ${it.type}${if (it.isOptional) "?" else ""}".trim()
        })
        append("): ")
        append(handler.function.returnType)
        if (handler.function.returnType.isMarkedNullable) append("?")
        append("]")
    }
}