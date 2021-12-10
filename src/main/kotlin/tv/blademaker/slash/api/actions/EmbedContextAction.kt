package tv.blademaker.slash.api.actions

import net.dv8tion.jda.api.entities.MessageEmbed
import tv.blademaker.slash.api.SlashCommandContext

class EmbedContextAction(override val ctx: SlashCommandContext, override val original: MessageEmbed) : ContextAction<MessageEmbed> {

    override fun send() = ctx.hook.sendMessageEmbeds(original)

    override fun reply() = ctx.event.replyEmbeds(original)

}