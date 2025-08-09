package io.github.blad3mak3r.slash.context.actions

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
import tv.blademaker.slash.context.ModalContext

class ModalContextAction(val ctx: ModalContext, override val original: MessageCreateData) : ContextAction<MessageCreateData> {
    override val configuration: ContextAction.Configuration = ContextAction.Configuration()
    override val interaction: Interaction = ctx.interaction
    override fun send(): WebhookMessageCreateAction<Message> {
        return ctx.hook.sendMessage(original).apply {
            this.setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { this.addComponents(it) }
        }
    }

    override fun reply(): ReplyCallbackAction {
        return ctx.event.reply(original).apply {
            this.setEphemeral(configuration.ephemeral)
            configuration.actionRows?.let { this.addComponents(it) }
        }
    }

    fun editOriginal() = ctx.hook.editOriginal(MessageEditData.fromCreateData(original))
}