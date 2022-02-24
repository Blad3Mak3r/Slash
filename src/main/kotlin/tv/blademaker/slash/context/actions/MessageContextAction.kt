package tv.blademaker.slash.context.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import tv.blademaker.slash.context.SlashCommandContext

@Suppress("unused")
class MessageContextAction(override val ctx: SlashCommandContext, override val original: Message) : ContextAction<Message> {

    override val configuration: ContextAction.Configuration = ContextAction.Configuration()

    override fun send(): WebhookMessageAction<Message> {
        return ctx.hook.sendMessage(original).apply {
            this.setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { this.addActionRows(it) }
        }
    }

    override fun reply(): ReplyCallbackAction {
        return ctx.event.reply(original).apply {
            this.setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { this.addActionRows(it) }
        }
    }

    fun editOriginal() = ctx.hook.editOriginal(original)
}