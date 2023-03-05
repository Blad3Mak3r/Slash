package tv.blademaker.slash.client

import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import tv.blademaker.slash.BaseSlashCommand
import tv.blademaker.slash.exceptions.ExceptionHandler

@Suppress("unused")
interface SlashCommandClient : EventListener {

    /**
     * The command registry.
     */
    val registry: List<BaseSlashCommand>

    val exceptionHandler: ExceptionHandler

    override fun onEvent(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> onSlashCommandEvent(event)
            is CommandAutoCompleteInteractionEvent -> onCommandAutoCompleteEvent(event)
            is ModalInteractionEvent -> onModalInteractionEvent(event)
            is ButtonInteractionEvent -> onButtonInteractionEvent(event)
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

    fun onModalInteractionEvent(event: ModalInteractionEvent)

    fun onButtonInteractionEvent(event: ButtonInteractionEvent)

    fun onUserContextInteractionEvent(event: UserContextInteractionEvent)

    fun onMessageContextInteractionEvent(event: MessageContextInteractionEvent)

    fun getCommand(name: String) = registry.firstOrNull { it.commandName.equals(name, true) }

    companion object {
        fun default(packageName: String) = DefaultSlashCommandBuilder(packageName)
    }
}