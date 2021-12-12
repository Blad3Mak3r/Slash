package tv.blademaker.slash.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import org.slf4j.LoggerFactory
import tv.blademaker.slash.api.*
import tv.blademaker.slash.api.Metrics
import tv.blademaker.slash.api.exceptions.PermissionsLackException
import tv.blademaker.slash.api.SlashUtils.toHuman
import tv.blademaker.slash.internal.CommandExecutionCheck
import tv.blademaker.slash.internal.newCoroutineDispatcher
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * Extendable coroutine based SlashCommandClient
 *
 * @param packageName The package name where commands are located (me.example.commands)
 *
 * @see SlashCommandClient
 * @see CoroutineScope
 *
 * @see SlashUtils.discoverSlashCommands
 */
open class DefaultSlashCommandClient(packageName: String) : SlashCommandClient, CoroutineScope {

    private val dispatcher = newCoroutineDispatcher("slash-commands-worker-%s", 2, 50)

    private val globalChecks: MutableList<CommandExecutionCheck> = mutableListOf()

    override val coroutineContext: CoroutineContext
        get() = dispatcher + Job()

    override val registry = SlashUtils.discoverSlashCommands(packageName).let {
        logger.info("Discovered a total of ${it.count} commands in ${it.elapsedTime}ms.")
        it.commands
    }

    override fun onSlashCommandEvent(event: SlashCommandEvent) {
        launch { handleSuspend(event) }
    }

    /**
     * Executed when a command return an exception
     *
     * @param context The current [SlashCommandContext]
     * @param command The command that throw the exception [BaseSlashCommand]
     * @param ex The threw exception
     */
    open fun onGenericException(context: SlashCommandContext, command: BaseSlashCommand, ex: Exception) {
        val message = "Exception executing handler for `${context.event.commandPath}` -> **${ex.message}**"

        logger.error(message, ex)

        if (context.event.isAcknowledged) context.sendMessage(message).setEphemeral(true).queue()
        else context.replyMessage(message).setEphemeral(true).queue()
    }

    /**
     * Executed when an interaction event does not meet the required permissions.
     *
     * @param ex The threw exception [PermissionsLackException], this exception includes
     * the current SlashCommandContext, the permission target (user, bot) and the required permissions.
     */
    open fun onPermissionsLackException(ex: PermissionsLackException) {
        when(ex.target) {
            PermissionTarget.BOT -> {
                val perms = ex.permissions.toHuman()
                logger.warn("Bot doesn't have the required permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage("\uD83D\uDEAB The bot does not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
            PermissionTarget.USER -> {
                val perms = ex.permissions.toHuman()
                logger.warn("User ${ex.context.author} doesn't have the required permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage("\uD83D\uDEAB You do not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
        }.setEphemeral(true).queue()
    }

    open suspend fun createContext(event: SlashCommandEvent, command: BaseSlashCommand): SlashCommandContext {
        return SlashCommandContextImpl(this, event)
    }

    fun addGlobalCheck(check: CommandExecutionCheck) {
        if (globalChecks.contains(check)) error("Check already registered.")
        globalChecks.add(check)
    }

    private suspend fun runChecks(ctx: SlashCommandContext): Boolean {
        if (globalChecks.isEmpty()) return true
        return globalChecks.all { it(ctx) }
    }

    private suspend fun handleSuspend(event: SlashCommandEvent) {
        if (!event.isFromGuild)
            return event.reply("This command is not supported outside a guild.").queue()

        val command = getCommand(event.name) ?: return
        val context = createContext(event, command)

        logCommand(context.guild, "${event.user.asTag} uses command \u001B[33m${event.commandString}\u001B[0m")

        if (!runChecks(context)) return

        try {
            val start = System.nanoTime()
            command.execute(context)
            val end = (System.nanoTime() - start) / 1_000_000
            Metrics.incSuccessCommand(event, end)
        } catch (e: PermissionsLackException){
            onPermissionsLackException(e)
            Metrics.incFailedCommand(event)
        } catch (e: Exception) {
            onGenericException(context, command, e)
            Metrics.incFailedCommand(event)
        } finally {
            Metrics.incHandledCommand(event)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DefaultSlashCommandClient::class.java)

        private fun logCommand(guild: Guild, content: String) = logger.info("[\u001b[32m${guild.name}(${guild.id})\u001b[0m] $content")
    }
}