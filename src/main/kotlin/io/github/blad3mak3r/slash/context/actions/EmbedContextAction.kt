package io.github.blad3mak3r.slash.context.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import io.github.blad3mak3r.slash.context.SlashCommandContext

@Suppress("unused")
class EmbedContextAction(val ctx: SlashCommandContext, override val original: MessageEmbed) : ContextAction<MessageEmbed> {

    override val configuration: ContextAction.Configuration = ContextAction.Configuration()
    override val interaction: Interaction = ctx.interaction

    override fun send(): WebhookMessageCreateAction<Message> {
        return ctx.hook.sendMessageEmbeds(original).apply {
            this.setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { this.addComponents(it) }
        }
    }

    override fun reply(): ReplyCallbackAction {
        return ctx.event.replyEmbeds(original).apply {
            this.setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { this.addComponents(it) }
        }
    }

    fun editOriginal() = ctx.hook.editOriginalEmbeds(original)
}