package tv.blademaker.slash.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.PermissionTarget
import tv.blademaker.slash.SlashUtils
import tv.blademaker.slash.metrics.Metrics
import tv.blademaker.slash.exceptions.PermissionsLackException
import tv.blademaker.slash.SlashUtils.toHuman
import tv.blademaker.slash.annotations.InteractionTarget
import tv.blademaker.slash.context.AutoCompleteContext
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.context.impl.GuildSlashCommandContext
import tv.blademaker.slash.context.impl.SlashCommandContextImpl
import tv.blademaker.slash.exceptions.InteractionTargetMismatch
import tv.blademaker.slash.internal.CommandExecutionCheck
import tv.blademaker.slash.extensions.newCoroutineDispatcher
import tv.blademaker.slash.metrics.MetricsStrategy
import kotlin.coroutines.CoroutineContext

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
open class DefaultSlashCommandClient constructor(
    packageName: String,
    strategy: MetricsStrategy? = null
) : SlashCommandClient, CoroutineScope {

    private val metrics: Metrics? = if (strategy != null) Metrics(strategy) else null

    private val dispatcher = newCoroutineDispatcher("slash-commands-worker-%s", 2, 50)

    private val globalChecks: MutableList<CommandExecutionCheck> = mutableListOf()

    override val coroutineContext: CoroutineContext
        get() = dispatcher + Job()

    private val discoveryResult = SlashUtils.discoverSlashCommands(packageName)

    override val registry = discoveryResult.let {
        logger.info("Discovered a total of ${it.commands.size} commands in ${it.elapsedTime}ms.")
        it.commands
    }

    override fun onSlashCommandEvent(event: SlashCommandInteractionEvent) {
        launch { handleSlashCommandEvent(event) }
    }

    override fun onCommandAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent) {
        launch { handleAutoCompleteEvent(event) }
    }

    fun register(jda: JDA) {
        jda.addEventListener(this)
    }

    fun register(shardManager: ShardManager) {
        shardManager.addEventListener(this)
    }

    fun register(jdaBuilder: JDABuilder) {
        jdaBuilder.addEventListeners(this)
    }

    fun register(shardManagerBuilder: DefaultShardManagerBuilder) {
        shardManagerBuilder.addEventListeners(this)
    }

    /**
     * Executed when a command return an exception
     *
     * @param context The current [SlashCommandContext]
     * @param command The command that throw the exception [BaseSlashCommand]
     * @param ex The threw exception
     */
    open fun onGenericException(context: SlashCommandContext, command: BaseSlashCommand, ex: Exception) {
        val message = "Exception executing handler for `${context.event.commandPath}`:\n```\n${ex.message}\n```"

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
                logger.warn("User ${ex.context.user} doesn't have the required permissions to execute '${ex.context.event.commandString}'.")
                ex.context.replyMessage("\uD83D\uDEAB You do not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
        }.setEphemeral(true).queue()
    }

    open fun onInteractionTargetMismatch(ex: InteractionTargetMismatch) {
        when (ex.target) {
            InteractionTarget.GUILD -> ex.context.replyMessage("This command cannot be used outside of a **space**.").queue()
            InteractionTarget.DM -> ex.context.replyMessage("This command cannot be used on a **space**.").queue()
            else -> throw IllegalStateException("Received InteractionTargetMismatch on a command with target InteractionTarget.ALL, report this to developer.")
        }
    }

    open suspend fun createContext(event: SlashCommandInteractionEvent, command: BaseSlashCommand): SlashCommandContext {
        return SlashCommandContextImpl(this, event)
    }

    open suspend fun createGuildContext(event: SlashCommandInteractionEvent, command: BaseSlashCommand): GuildSlashCommandContext {
        return GuildSlashCommandContext(this, event)
    }

    fun addGlobalCheck(check: CommandExecutionCheck) {
        if (globalChecks.contains(check)) error("Check already registered.")
        globalChecks.add(check)
    }

    private suspend fun runChecks(ctx: SlashCommandContext): Boolean {
        if (globalChecks.isEmpty()) return true
        return globalChecks.all { it(ctx) }
    }

    private suspend fun handleAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent) {
        val command = getCommand(event.name) ?: return
        val context = AutoCompleteContext(event)

        //logCommand(context.guild, "${event.user.asTag} uses command \u001B[33m${event.commandString}\u001B[0m")

        try {
            command.executeAutoComplete(context)
        } catch (e: Exception) {
            logger.error("Exception executing auto-complete handler for command ${command.commandName}: ${e.message}", e)
        }
    }

    private suspend fun handleSlashCommandEvent(event: SlashCommandInteractionEvent) {
        val commandPath = event.commandPath

        val command = getCommand(event.name) ?: return
        val handler = command.handlers.slash.find { it.path == commandPath }
            ?: error("No handler found for slash command path $commandPath")

        val context = when (handler.target) {
            InteractionTarget.GUILD -> createGuildContext(event, command)
            else -> createContext(event, command)
        }

        if (event.isFromGuild) logCommand(event.guild!!, "${event.user.asTag} uses command \u001B[33m${event.commandString}\u001B[0m")
        else logCommand(event.user, "uses command \u001B[33m${event.commandString}\u001B[0m")

        if (!runChecks(context)) return

        try {
            val start = System.nanoTime()
            command.execute(context, handler)
            val end = (System.nanoTime() - start) / 1_000_000
            metrics?.incSuccessCommand(event, end)
        } catch (e: PermissionsLackException) {
            onPermissionsLackException(e)
            metrics?.incFailedCommand(event)
        } catch (e: InteractionTargetMismatch) {
            onInteractionTargetMismatch(e)
        } catch (e: Exception) {
            onGenericException(context, command, e)
            metrics?.incFailedCommand(event)
        } finally {
            metrics?.incHandledCommand(event)
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(DefaultSlashCommandClient::class.java)

        private fun logCommand(guild: Guild, content: String) = logger.info("[\u001b[32m${guild.name}(${guild.id})\u001b[0m] $content")
        private fun logCommand(user: User, content: String) = logger.info("[\u001b[32m${user.asTag}(${user.id})\u001b[0m] $content")
    }
}