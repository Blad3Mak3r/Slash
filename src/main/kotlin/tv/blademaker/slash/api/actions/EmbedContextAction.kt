package tv.blademaker.slash.api.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import tv.blademaker.slash.api.SlashCommandContext

@Suppress("unused")
class EmbedContextAction(override val ctx: SlashCommandContext, override val original: MessageEmbed) : ContextAction<MessageEmbed> {

    override fun send(ephemeral: Boolean): WebhookMessageAction<Message> {
        return ctx.hook.sendMessageEmbeds(original).setEphemeral(ephemeral)
    }

    override fun reply(ephemeral: Boolean): ReplyAction {
        return ctx.event.replyEmbeds(original).setEphemeral(ephemeral)
    }

    fun editOriginal() = ctx.hook.editOriginalEmbeds(original)
}