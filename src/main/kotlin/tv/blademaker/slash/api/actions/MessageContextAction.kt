package tv.blademaker.slash.api.actions

import net.dv8tion.jda.api.entities.Message
import tv.blademaker.slash.api.SlashCommandContext

class MessageContextAction(override val ctx: SlashCommandContext, override val original: Message) : ContextAction<Message> {

    override fun send() = ctx.hook.sendMessage(original)

    override fun reply() = ctx.event.reply(original)
}