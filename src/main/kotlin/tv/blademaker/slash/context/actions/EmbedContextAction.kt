package tv.blademaker.slash.context.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import tv.blademaker.slash.context.SlashCommandContext

@Suppress("unused")
class EmbedContextAction(override val ctx: SlashCommandContext, override val original: MessageEmbed) : ContextAction<MessageEmbed> {

    override fun send(ephemeral: Boolean): WebhookMessageAction<Message> {
        return ctx.hook.sendMessageEmbeds(original).setEphemeral(ephemeral)
    }

    override fun reply(ephemeral: Boolean): ReplyCallbackAction {
        return ctx.event.replyEmbeds(original).setEphemeral(ephemeral)
    }

    fun editOriginal() = ctx.hook.editOriginalEmbeds(original)
}