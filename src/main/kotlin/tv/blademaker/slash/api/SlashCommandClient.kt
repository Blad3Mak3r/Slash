package tv.blademaker.slash.api

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.sharding.ShardManager
import tv.blademaker.slash.internal.SlashUtils.toHuman

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