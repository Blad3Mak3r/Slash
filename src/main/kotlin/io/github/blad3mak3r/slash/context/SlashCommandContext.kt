package io.github.blad3mak3r.slash.context

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.CommandInteraction
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import tv.blademaker.slash.client.SlashCommandClient
import tv.blademaker.slash.context.actions.ContextAction
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction

@Suppress("unused")
open class SlashCommandContext(
    override val client: SlashCommandClient,
    override val event: SlashCommandInteractionEvent,
    override val function: KFunction<*>
) : DeferrableInteraction, InteractionContext<SlashCommandInteractionEvent>, FunctionHandler {

    val isAcknowledged: Boolean
        get() = event.isAcknowledged

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

    /**
     * Reply to the event with the given content.
     *
     * @param content The content for the message.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyMessage(content: String) = ContextAction.build(this, content).reply()

    /**
     * Reply to the event with the given content.
     *
     * @param message The message to reply with.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyMessage(message: Message) = ContextAction.build(this, MessageCreateData.fromMessage(message)).reply()

    /**
     * Reply to the event with the given content.
     *
     * @param builder The message builder.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyMessage(builder: MessageCreateBuilder) = ContextAction.build(this, builder).reply()

    /**
     * SReply to the event with the given content.
     *
     * @param builder The message builder function.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyMessage(builder: MessageCreateBuilder.() -> Unit) = ContextAction.build(this, builder).reply()

    // Embed actions

    /**
     * Reply to the event with the given embed.
     *
     * @param embed The embed.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyEmbed(embed: MessageEmbed) = ContextAction.build(this, embed).reply()

    /**
     * Reply to the event with the given embed.
     *
     * @param builder The embed builder.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyEmbed(builder: EmbedBuilder) = ContextAction.build(this, builder).reply()

    /**
     * Reply to the event with the given embed.
     *
     * @param builder The embed builder function.
     *
     * @return A [ReplyCallbackAction]
     */
    fun replyEmbed(builder: EmbedBuilder.() -> Unit) = ContextAction.build(this, builder).reply()

    // Followup messages

    /**
     * Send a follow-up message with the given content.
     *
     * @param content The content for the message.
     *
     * @return A [ReplyCallbackAction]
     */
    fun sendMessage(content: String) = ContextAction.build(this, content).send()

    /**
     * Send a follow-up message with the given content.
     *
     * @param message The message to send.
     *
     * @return [net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction]
     */
    fun sendMessage(message: Message) = ContextAction.build(this, MessageCreateData.fromMessage(message)).send()

    /**
     * Send a follow-up message with the given content.
     *
     * @param builder The message builder.
     *
     * @return A [net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction]
     */
    fun sendMessage(builder: MessageCreateBuilder) = ContextAction.build(this, builder).send()

    /**
     * Send a follow-up message with the given content.
     *
     * @param builder The message builder function.
     *
     * @return A [net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction]
     */
    fun sendMessage(builder: MessageCreateBuilder.() -> Unit) = ContextAction.build(this, builder).send()

    // Embed actions

    /**
     * Send a follow-up message with the given embed.
     *
     * @param embed The embed.
     *
     * @return A [net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction]
     */
    fun sendEmbed(embed: MessageEmbed) = ContextAction.build(this, embed).send()

    /**
     * Send a follow-up message with the given embed.
     *
     * @param builder The embed builder.
     *
     * @return A [net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction]
     */
    fun sendEmbed(builder: EmbedBuilder) = ContextAction.build(this, builder).send()

    /**
     * Send a follow-up message with the given embed.
     *
     * @param builder The embed builder function.
     *
     * @return A [net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction]
     */
    fun sendEmbed(builder: EmbedBuilder.() -> Unit) = ContextAction.build(this, builder).send()

    // DSL Builders

    /**
     * Build a [ContextAction] using DSL.
     *
     * @param builder An embed builder function.
     *
     * @return The context action for the embed.
     */
    fun embed(builder: EmbedBuilder.() -> Unit) = ContextAction.build(this, builder)

    /**
     * Build a [ContextAction] using DSL.
     *
     * @param builder An message builder function.
     *
     * @return The context action for the message.
     */
    fun message(builder: MessageCreateBuilder.() -> Unit) = ContextAction.build(this, builder)

    fun message(content: String) = message { setContent(content) }

    fun replyModal(customId: String, title: String, builder: Modal.Builder.() -> Unit) =
        event.replyModal(Modal.create(customId, title).apply(builder).build())

    /**
     * A map where you can define anything
     */
    var extra = HashMap<String, Any>()
}