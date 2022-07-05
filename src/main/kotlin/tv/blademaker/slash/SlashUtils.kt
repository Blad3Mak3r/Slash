package tv.blademaker.slash

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import tv.blademaker.slash.annotations.AutoComplete
import tv.blademaker.slash.annotations.SlashCommand
import tv.blademaker.slash.internal.AutoCompleteHandler
import tv.blademaker.slash.internal.CommandHandlers
import tv.blademaker.slash.internal.SlashCommandHandler
import java.lang.reflect.Modifier
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

object SlashUtils {

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

    /**
     * Discover the [BaseSlashCommand] inside the package.
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
     * @see tv.blademaker.slash.BaseSlashCommand
     *
     * @return A [DiscoveryResult] with the elapsed time to discover commands, the count of commands
     * discovered and the commands itself.
     */
    fun discoverSlashCommands(packageName: String): DiscoveryResult {
        val start = System.nanoTime()
        val classes = Reflections(packageName, Scanners.SubTypes)
            .getSubTypesOf(BaseSlashCommand::class.java)
            .filter { !Modifier.isAbstract(it.modifiers) && BaseSlashCommand::class.java.isAssignableFrom(it) }

        val commands = mutableListOf<BaseSlashCommand>()

        for (clazz in classes) {
            val instance = clazz.getDeclaredConstructor().newInstance()
            val commandName = instance.commandName.lowercase()

            if (commands.any { it.commandName.equals(commandName, true) }) {
                error("Command with name $commandName is already registered.")
            }

            commands.add(instance)
        }

        return DiscoveryResult(
            elapsedTime = (System.nanoTime() - start) / 1_000_000,
            commands = commands
        )
    }

    @Suppress("unused")
    fun RestAction<*>.asEphemeral(): RestAction<*> {
        when(this) {
            is ReplyCallbackAction -> this.setEphemeral(true)
            is WebhookMessageAction<*> -> this.setEphemeral(true)
        }

        return this
    }

    internal fun compileCommandHandlers(commands: List<BaseSlashCommand>): CommandHandlers {
        val slashCommandHandlers = commands.map { compileSlashCommandHandlers(it) }.let {
            if (it.isEmpty()) emptyList()
            else it.reduce { acc, list -> list + acc }
        }
        val autoCompleteHandlers = commands.map { compileAutoCompleteHandlers(it) }.let {
            if (it.isEmpty()) emptyList()
            else it.reduce { acc, list -> list + acc }
        }
        return CommandHandlers(
            slashCommandHandlers,
            autoCompleteHandlers
        )
    }

    private fun compileSlashCommandHandlers(command: BaseSlashCommand): List<SlashCommandHandler> {
        val handlers = command::class.functions
            .filter { it.hasAnnotation<SlashCommand>() && it.visibility == KVisibility.PUBLIC && !it.isAbstract }
            .map { SlashCommandHandler(command, it) }

        val finalList = mutableListOf<SlashCommandHandler>()

        for (handler in handlers) {
            check(!finalList.any { it.path == handler.path }) {
                "Found more than one InteractionHandler for the same path ${handler.path}"
            }
            finalList.add(handler)
        }

        check(finalList.isNotEmpty()) {
            "SlashCommand ${command.commandName} does not have registered handlers."
        }

        checkDefault(command, finalList) {
            "SlashCommand ${command.commandName} have registered more than 1 handler having a default handler."
        }

        return finalList
    }

    private fun checkDefault(command: BaseSlashCommand, list: List<SlashCommandHandler>, lazyMessage: () -> String) {
        if (list.size <= 1) return
        if (list.any { it.path == command.commandName }) error(lazyMessage())
    }

    private fun compileAutoCompleteHandlers(command: BaseSlashCommand): List<AutoCompleteHandler> {
        val handlers = command::class.functions
            .filter { it.hasAnnotation<AutoComplete>() && it.visibility == KVisibility.PUBLIC && !it.isAbstract }
            .map { AutoCompleteHandler(command, it) }

        val finalList = mutableListOf<AutoCompleteHandler>()

        for (handler in handlers) {
            check(!finalList.any { it.path == handler.path && it.optionName == handler.optionName }) {
                "Found more than one AutocompleteHandler for the same path (${handler.path}) and option (${handler.optionName})."
            }
            finalList.add(handler)
        }

        return finalList
    }
}