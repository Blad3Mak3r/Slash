package tv.blademaker.slash.api.actions

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import tv.blademaker.slash.api.SlashCommandContext

interface ContextAction<T> {

    val ctx: SlashCommandContext
    val original: T

    /**
     * Send a followup message to the interaction.
     *
     * This requires the interaction to be acknowledged.
     *
     * @see net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
     */
    fun send(ephemeral: Boolean = false): WebhookMessageAction<Message>

    /**
     * Send a reply message to the interaction.
     *
     * This not requires the interaction to be acknowledged.
     *
     * @see net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
     */
    fun reply(ephemeral: Boolean = false): ReplyAction

    /**
     * Queue the request and don't wait for the response.
     *
     * Automatically check if the interaction was acknowledged.
     *
     * If not ACK: reply(ephemeral: Boolean).queue()
     * If ACK: send(ephemeral: Boolean).queue()
     *
     * @param ephemeral Set if the message is ephemeral.
     *
     * @see ContextAction.send
     * @see ContextAction.reply
     */
    fun queue(ephemeral: Boolean = false) {
        when (ctx.event.isAcknowledged) {
            true -> send(ephemeral).queue()
            false -> reply(ephemeral).queue()
        }
    }

    @Suppress
    companion object {
        internal fun build(ctx: SlashCommandContext, embed: MessageEmbed): EmbedContextAction {
            return EmbedContextAction(ctx, embed)
        }

        internal fun build(ctx: SlashCommandContext, embedBuilder: EmbedBuilder): EmbedContextAction {
            return EmbedContextAction(ctx, embedBuilder.build())
        }

        internal fun build(ctx: SlashCommandContext, embedBuilder: EmbedBuilder.() -> Unit): EmbedContextAction {
            return EmbedContextAction(ctx, EmbedBuilder().apply(embedBuilder).build())
        }

        internal fun build(ctx: SlashCommandContext, message: Message): MessageContextAction {
            return MessageContextAction(ctx, message)
        }

        internal fun build(ctx: SlashCommandContext, message: String): MessageContextAction {
            return MessageContextAction(ctx, MessageBuilder().append(message).build())
        }

        internal fun build(ctx: SlashCommandContext, messageBuilder: MessageBuilder): MessageContextAction {
            return MessageContextAction(ctx, messageBuilder.build())
        }

        internal fun build(ctx: SlashCommandContext, messageBuilder: MessageBuilder.() -> Unit): MessageContextAction {
            return MessageContextAction(ctx, MessageBuilder().apply(messageBuilder).build())
        }
    }
}