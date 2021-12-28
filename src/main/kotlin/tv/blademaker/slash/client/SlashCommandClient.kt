package tv.blademaker.slash.client

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import tv.blademaker.slash.BaseSlashCommand

@Suppress("unused")
interface SlashCommandClient : EventListener {

    /**
     * The command registry.
     */
    val registry: List<BaseSlashCommand>

    override fun onEvent(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> onSlashCommandEvent(event)
            is CommandAutoCompleteInteractionEvent -> onCommandAutoCompleteEvent(event)
        }
    }

    /**
     * Handled when discord send an [SlashCommandInteractionEvent]
     *
     * @param event The [SlashCommandInteractionEvent] sent by Discord.
     */
    fun onSlashCommandEvent(event: SlashCommandInteractionEvent)

    /**
     * Handled when discord send an [CommandAutoCompleteInteractionEvent]
     *
     * @param event The [CommandAutoCompleteInteractionEvent] sent by Discord.
     */
    fun onCommandAutoCompleteEvent(event: CommandAutoCompleteInteractionEvent)

    fun getCommand(name: String) = registry.firstOrNull { it.commandName.equals(name, true) }
}