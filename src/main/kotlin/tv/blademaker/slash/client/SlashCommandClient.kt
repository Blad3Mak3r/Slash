package tv.blademaker.slash.client

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.sharding.ShardManager
import tv.blademaker.slash.api.BaseSlashCommand
import tv.blademaker.slash.api.Metrics
import tv.blademaker.slash.api.PermissionTarget
import tv.blademaker.slash.api.SlashCommandContext
import tv.blademaker.slash.api.exceptions.PermissionsLackException
import tv.blademaker.slash.api.SlashUtils.toHuman

@Suppress("unused")
interface SlashCommandClient : EventListener {

    /**
     * The command registry.
     */
    val registry: List<BaseSlashCommand>

    override fun onEvent(event: GenericEvent) {
        if (event is SlashCommandEvent) onSlashCommandEvent(event)
    }

    /**
     * Handled when discord send an [SlashCommandEvent]
     *
     * @param event The [SlashCommandEvent] sent by Discord.
     */
    fun onSlashCommandEvent(event: SlashCommandEvent)

    fun getCommand(name: String) = registry.firstOrNull { it.commandName.equals(name, true) }

    /**
     * Executed when a command return an exception
     *
     * @param context The current [SlashCommandContext]
     * @param command The command that throw the exception [BaseSlashCommand]
     * @param ex The threw exception
     */
    fun onGenericException(context: SlashCommandContext, command: BaseSlashCommand, ex: Exception) {
        val message = "Exception executing handler for `${context.event.commandPath}` -> **${ex.message}**"

        if (context.event.isAcknowledged) context.sendMessage(message).setEphemeral(true).queue()
        else context.replyMessage(message).setEphemeral(true).queue()
    }

    /**
     * Executed when an interaction event does not meet the required permissions.
     *
     * @param ex The threw exception [PermissionsLackException], this exception includes
     * the current SlashCommandContext, the permission target (user, bot) and the required permissions.
     */
    fun onPermissionsLackException(ex: PermissionsLackException) {
        when(ex.target) {
            PermissionTarget.BOT -> {
                val perms = ex.permissions.toHuman()
                ex.context.replyMessage("\uD83D\uDEAB The bot does not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
            PermissionTarget.USER -> {
                val perms = ex.permissions.toHuman()
                ex.context.replyMessage("\uD83D\uDEAB You do not have the necessary permissions to carry out this action." +
                        "\nRequired permissions: **${perms}**.")
            }
        }.setEphemeral(true).queue()
    }

    /**
     * Enable prometheus metrics exporters
     */
    fun withMetrics() {
        Metrics.register()
    }

    /**
     * Register the event listener to receive [SlashCommandEvent] from your [ShardManager].
     *
     * @param shardManager The [ShardManager]
     */
    fun withShardManager(shardManager: ShardManager) {
        shardManager.addEventListener(this)
    }

    /**
     * Register the event listener to receive [SlashCommandEvent] from your [JDA] shard.
     *
     * @param jda The [JDA] shard instance.
     */
    fun withJDA(jda: JDA) {
        jda.addEventListener(this)
    }
}