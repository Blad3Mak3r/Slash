package io.github.blad3mak3r.slash.context

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.CommandInteraction
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import io.github.blad3mak3r.slash.client.SlashCommandClient
import io.github.blad3mak3r.slash.context.actions.ContextAction
import net.dv8tion.jda.api.modals.Modal
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("unused")
open class SlashCommandContext(
    override val client: SlashCommandClient,
    override val event: SlashCommandInteractionEvent
) : DeferrableInteraction, InteractionContext<SlashCommandInteractionEvent> {

    val isAcknowledged: Boolean
        get() = event.isAcknowledged

    val isFromAttachedGuild: Boolean
        get() = event.isFromAttachedGuild

    val isFromGuild: Boolean
        get() = event.isFromGuild

    open val guild: Guild?
        get() = event.guild

    override val interaction: CommandInteraction
        get() = event.interaction

    val hook: InteractionHook
        get() = event.hook

    val options: List<OptionMapping>
        get() = event.options

    open val channel: MessageChannel
        get() = event.channel

    val user: User
        get() = event.user

    open val member: Member?
        get() = event.member

    override suspend fun acknowledge(ephemeral: Boolean) = suspendCoroutine { cont ->
        if (isAcknowledged) {
            cont.resume(Unit)
        } else {
            event.deferReply(ephemeral).queue({
                cont.resume(Unit)
            }, {
                cont.resumeWithException(it)
            })
        }
    }

    fun getOption(name: String) = event.getOption(name)

    // Replies

    fun replyMessage(content: String) = ContextAction.build(this, content).reply()

    fun replyMessage(message: Message) = ContextAction.build(this, MessageCreateData.fromMessage(message)).reply()

    fun replyMessage(builder: MessageCreateBuilder) = ContextAction.build(this, builder).reply()

    fun replyMessage(builder: MessageCreateBuilder.() -> Unit) = ContextAction.build(this, builder).reply()

    // Embed actions

    fun replyEmbed(embed: MessageEmbed) = ContextAction.build(this, embed).reply()

    fun replyEmbed(builder: EmbedBuilder) = ContextAction.build(this, builder).reply()

    fun replyEmbed(builder: EmbedBuilder.() -> Unit) = ContextAction.build(this, builder).reply()

    // Follow-up messages

    fun sendMessage(content: String) = ContextAction.build(this, content).send()

    fun sendMessage(message: Message) = ContextAction.build(this, MessageCreateData.fromMessage(message)).send()

    fun sendMessage(builder: MessageCreateBuilder) = ContextAction.build(this, builder).send()

    fun sendMessage(builder: MessageCreateBuilder.() -> Unit) = ContextAction.build(this, builder).send()

    // Embed send

    fun sendEmbed(embed: MessageEmbed) = ContextAction.build(this, embed).send()

    fun sendEmbed(builder: EmbedBuilder) = ContextAction.build(this, builder).send()

    fun sendEmbed(builder: EmbedBuilder.() -> Unit) = ContextAction.build(this, builder).send()

    // DSL builders

    fun embed(builder: EmbedBuilder.() -> Unit) = ContextAction.build(this, builder)

    fun message(builder: MessageCreateBuilder.() -> Unit) = ContextAction.build(this, builder)

    fun message(content: String) = message { setContent(content) }

    fun replyModal(customId: String, title: String, builder: Modal.Builder.() -> Unit) =
        event.replyModal(Modal.create(customId, title).apply(builder).build())

    /** Arbitrary extra data for the lifetime of this interaction. */
    var extra = HashMap<String, Any>()
}
