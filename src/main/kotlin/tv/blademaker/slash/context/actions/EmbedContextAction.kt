package tv.blademaker.slash.context.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import tv.blademaker.slash.context.SlashCommandContext

@Suppress("unused")
class EmbedContextAction(override val ctx: SlashCommandContext, override val original: MessageEmbed) : ContextAction<MessageEmbed> {

    override val configuration: ContextAction.Configuration = ContextAction.Configuration()

    override fun send(): WebhookMessageAction<Message> {
        return ctx.hook.sendMessageEmbeds(original).apply {
            setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { setActionRows(it) }
        }
    }

    override fun reply(): ReplyCallbackAction {
        return ctx.event.replyEmbeds(original).apply {
            setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { setActionRows(it) }
        }
    }

    fun editOriginal() = ctx.hook.editOriginalEmbeds(original)
}