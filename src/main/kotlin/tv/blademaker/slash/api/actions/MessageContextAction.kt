package tv.blademaker.slash.api.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import tv.blademaker.slash.api.SlashCommandContext

@Suppress("unused")
class MessageContextAction(override val ctx: SlashCommandContext, override val original: Message) : ContextAction<Message> {

    override fun send(ephemeral: Boolean): WebhookMessageAction<Message> {
        return ctx.hook.sendMessage(original).setEphemeral(ephemeral)
    }

    override fun reply(ephemeral: Boolean): ReplyAction {
        return ctx.event.reply(original).setEphemeral(ephemeral)
    }

    fun editOriginal() = ctx.hook.editOriginal(original)
}