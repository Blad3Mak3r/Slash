package io.github.blad3mak3r.slash.context

import io.github.blad3mak3r.slash.client.SlashCommandClient
import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.CommandInteraction
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.coroutines.resume

open class SlashCommandContext(
    override val client: SlashCommandClient,
    override val event: SlashCommandInteractionEvent
) : DeferrableInteraction, InteractionContext<SlashCommandInteractionEvent> {

    val isAcknowledged: Boolean get() = event.isAcknowledged
    val isFromGuild: Boolean get() = event.isFromGuild
    val isFromAttachedGuild: Boolean get() = event.guild != null

    override val interaction: CommandInteraction get() = event.interaction
    val hook: InteractionHook get() = event.hook
    val options: List<OptionMapping> get() = event.options
    open val channel: MessageChannel get() = event.channel
    val user: User get() = event.user
    open val member: Member? get() = event.member

    override suspend fun acknowledge(ephemeral: Boolean) {
        if (isAcknowledged) return
        suspendCancellableCoroutine { cont ->
            event.deferReply(ephemeral).queue(
                { cont.resume(Unit) },
                { cont.cancel(it) }
            )
        }
    }

    fun getOption(name: String): OptionMapping? = event.getOption(name)

    // ── Reply helpers ─────────────────────────────────────────────────────────

    fun replyMessage(content: String): ReplyCallbackAction = event.reply(content)

    fun replyMessage(data: MessageCreateData): ReplyCallbackAction = event.reply(data)

    fun replyMessage(builder: MessageCreateBuilder.() -> Unit): ReplyCallbackAction =
        event.reply(MessageCreateBuilder().apply(builder).build())

    fun replyModal(customId: String, title: String, builder: Modal.Builder.() -> Unit) =
        event.replyModal(Modal.create(customId, title).apply(builder).build()).queue()

    // ── Followup helpers ──────────────────────────────────────────────────────

    fun sendMessage(content: String) = hook.sendMessage(content)

    fun sendMessage(data: MessageCreateData) = hook.sendMessage(data)

    fun sendMessage(builder: MessageCreateBuilder.() -> Unit) =
        hook.sendMessage(MessageCreateBuilder().apply(builder).build())
}
