package tv.blademaker.slash.api

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.EventListener

interface SlashCommandClient : EventListener {

    val registry: List<BaseSlashCommand>

    override fun onEvent(event: GenericEvent) {
        if (event is SlashCommandEvent) onSlashCommandEvent(event)
    }

    fun onSlashCommandEvent(event: SlashCommandEvent)

    fun getCommand(name: String) = registry.firstOrNull { it.commandName.equals(name, true) }
}