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

    val registry: List<BaseSlashCommand>

    override fun onEvent(event: GenericEvent) {
        if (event is SlashCommandEvent) onSlashCommandEvent(event)
    }

    fun onSlashCommandEvent(event: SlashCommandEvent)

    fun getCommand(name: String) = registry.firstOrNull { it.commandName.equals(name, true) }

    fun withMetrics() {
        Metrics.register()
    }

    fun withShardManager(shardManager: ShardManager) {
        shardManager.addEventListener(this)
    }

    fun withJDA(jda: JDA) {
        jda.addEventListener(this)
    }
}