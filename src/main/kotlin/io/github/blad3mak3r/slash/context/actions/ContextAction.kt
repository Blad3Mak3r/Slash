package io.github.blad3mak3r.slash.context.actions

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import io.github.blad3mak3r.slash.context.SlashCommandContext
import java.awt.Color

interface ContextAction<T> {

    class Configuration {
        var ephemeral: Boolean = false
        var actionRows: Collection<ActionRow>? = null
    }

    val interaction: Interaction
    val original: T

    val configuration: Configuration

    fun applyConfiguration(configuration: Configuration.() -> Unit): ContextAction<T> {
        this.configuration.apply(configuration)
        return this
    }

    fun setEphemeral(ephemeral: Boolean): ContextAction<T> {
        this.configuration.ephemeral = ephemeral
        return this
    }

    fun setActionRows(rows: Collection<ActionRow>): ContextAction<T> {
        this.configuration.actionRows = rows
        return this
    }

    fun setActionRows(vararg rows: ActionRow): ContextAction<T> {
        this.configuration.actionRows = rows.toSet()
        return this
    }

    /**
     * Send a followup message to the interaction.
     *
     * This requires the interaction to be acknowledged.
     *
     * @see net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
     */
    fun send(): WebhookMessageCreateAction<Message>

    /**
     * Send a reply message to the interaction.
     *
     * This not requires the interaction to be acknowledged.
     *
     * @see net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
     */
    fun reply(): ReplyCallbackAction

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
    fun queue() {
        when (interaction.isAcknowledged) {
            true -> send().queue()
            false -> reply().queue()
        }
    }



    companion object {
        internal fun build(ctx: SlashCommandContext, embed: MessageEmbed): EmbedContextAction {
            return EmbedContextAction(ctx, embed)
        }

        internal fun build(ctx: SlashCommandContext, embedBuilder: EmbedBuilder): EmbedContextAction {
            return EmbedContextAction(ctx, embedBuilder.build())
        }

        internal fun build(ctx: SlashCommandContext, embedBuilder: EmbedBuilder.() -> Unit): EmbedContextAction {
            val color = ctx.event.guild?.selfMember?.color ?: Color(44, 47, 51)
            return build(ctx, EmbedBuilder().setColor(color).apply(embedBuilder))
        }

        internal fun build(ctx: SlashCommandContext, message: MessageCreateData): MessageContextAction {
            return MessageContextAction(ctx, message)
        }

        internal fun build(ctx: SlashCommandContext, message: String): MessageContextAction {
            return MessageContextAction(ctx, MessageCreateData.fromContent(message))
        }

        internal fun build(ctx: SlashCommandContext, messageBuilder: MessageCreateBuilder): MessageContextAction {
            return MessageContextAction(ctx, messageBuilder.build())
        }

        internal fun build(ctx: SlashCommandContext, messageBuilder: MessageCreateBuilder.() -> Unit): MessageContextAction {
            return MessageContextAction(ctx, MessageCreateBuilder().apply(messageBuilder).build())
        }
    }
}