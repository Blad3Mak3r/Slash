package tv.blademaker.slash.api

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

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
     * @see tv.blademaker.slash.api.BaseSlashCommand
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
            count = commands.size,
            commands = commands
        )
    }

    @Suppress("unused")
    fun RestAction<*>.asEphemeral(): RestAction<*> {
        when(this) {
            is ReplyAction -> this.setEphemeral(true)
            is WebhookMessageAction<*> -> this.setEphemeral(true)
        }

        return this
    }
}