package io.github.blad3mak3r.slash.client

import io.github.blad3mak3r.slash.SlashRegistry
import io.github.blad3mak3r.slash.context.AutoCompleteContext
import io.github.blad3mak3r.slash.context.ButtonContext
import io.github.blad3mak3r.slash.context.GuildSlashCommandContext
import io.github.blad3mak3r.slash.context.ModalContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SlashCommandClient internal constructor(
    private val registry: SlashRegistry
) : EventListener, CoroutineScope {

    private val threadIndex = AtomicInteger(0)
    private val executor = ThreadPoolExecutor(
        8, 50, 60L, TimeUnit.SECONDS,
        LinkedBlockingQueue(),
        { r -> Thread(r, "slash-worker-${threadIndex.incrementAndGet()}").also { it.isDaemon = true } }
    )
    override val coroutineContext = SupervisorJob() + executor.asCoroutineDispatcher()

    override fun onEvent(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> launch { handleSlash(event) }
            is ButtonInteractionEvent -> launch { handleButton(event) }
            is ModalInteractionEvent -> launch { handleModal(event) }
            is CommandAutoCompleteInteractionEvent -> launch { handleAutoComplete(event) }
        }
    }

    private suspend fun handleSlash(event: SlashCommandInteractionEvent) {
        val handler = registry.handlers.find { it.getCommandName() == event.name } ?: return
        val ctx = if (event.isFromGuild) GuildSlashCommandContext(this, event)
                  else SlashCommandContext(this, event)
        try {
            handler.dispatch(ctx)
        } catch (e: Exception) {
            log.error("Uncaught exception in command '${event.name}'", e)
        }
    }

    private suspend fun handleButton(event: ButtonInteractionEvent) {
        val ctx = ButtonContext(event, this)
        for (handler in registry.handlers) {
            try {
                if (handler.dispatchButton(ctx)) break
            } catch (e: Exception) {
                log.error("Uncaught exception in button '${event.componentId}'", e)
                break
            }
        }
    }

    private suspend fun handleModal(event: ModalInteractionEvent) {
        val ctx = ModalContext(event, this)
        for (handler in registry.handlers) {
            try {
                if (handler.dispatchModal(ctx)) break
            } catch (e: Exception) {
                log.error("Uncaught exception in modal '${event.modalId}'", e)
                break
            }
        }
    }

    private suspend fun handleAutoComplete(event: CommandAutoCompleteInteractionEvent) {
        val handler = registry.handlers.find { it.getCommandName() == event.name } ?: return
        val ctx = AutoCompleteContext(event, this)
        try {
            handler.dispatchAutoComplete(ctx)
        } catch (e: Exception) {
            log.error("Uncaught exception in autocomplete '${event.name}'", e)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SlashCommandClient::class.java)

        fun builder(registry: SlashRegistry): SlashCommandClientBuilder =
            SlashCommandClientBuilder(registry)
    }
}

// ── Extension helpers ─────────────────────────────────────────────────────────

fun SlashCommandClient.registerCommandsWith(jda: JDA, guildId: Long? = null) {
    // Convenience: call directly from the registry via SlashRegistry extension below
}

fun SlashRegistry.registerCommandsWith(jda: JDA, guildId: Long? = null) {
    val commandData = handlers.map { it.buildCommandData() }
    if (guildId != null) {
        jda.getGuildById(guildId)?.updateCommands()?.addCommands(commandData)?.queue()
    } else {
        jda.updateCommands().addCommands(commandData).queue()
    }
}

fun SlashRegistry.registerCommandsWith(shardManager: ShardManager, guildId: Long? = null) {
    val commandData = handlers.map { it.buildCommandData() }
    if (guildId != null) {
        shardManager.getGuildById(guildId)?.updateCommands()?.addCommands(commandData)?.queue()
    } else {
        shardManager.shards.forEach { jda ->
            jda.updateCommands().addCommands(commandData).queue()
        }
    }
}