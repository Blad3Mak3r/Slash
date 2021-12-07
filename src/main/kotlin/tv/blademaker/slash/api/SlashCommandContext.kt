package tv.blademaker.slash.api

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction
import java.util.concurrent.atomic.AtomicReference

@Suppress("unused")
interface SlashCommandContext {

    val commandClient: SlashCommandClient
    val event: SlashCommandEvent

    val isAcknowledged: Boolean
        get() = event.isAcknowledged

    val jda: JDA
        get() = event.jda

    @Suppress("MemberVisibilityCanBePrivate")
    val hook: InteractionHook
        get() = event.hook

    val options: List<OptionMapping>
        get() = event.options

    val guild: Guild
        get() = event.guild!!

    val member: Member
        get() = event.member!!

    val selfMember: Member
        get() = event.guild!!.selfMember

    val channel: TextChannel
        get() = event.channel as TextChannel

    val author: User
        get() = event.user

    fun acknowledge(ephemeral: Boolean = false): ReplyAction {
        if (isAcknowledged) throw IllegalStateException("Current command is already ack.")
        return event.deferReply(ephemeral)
    }

    fun getOption(name: String) = event.getOption(name)

    // Replies

    fun reply(content: String) = event.reply(content)

    fun reply(builder: MessageBuilder.() -> Unit): ReplyAction {
        val message = MessageBuilder().apply(builder).build()

        return event.reply(message)
    }

    fun replyEmbed(embed: MessageEmbed) = event.replyEmbeds(embed)

    fun replyEmbed(builder: EmbedBuilder.() -> Unit): ReplyAction {
        val embed = EmbedBuilder()
            .apply(builder).build()

        return event.replyEmbeds(embed)
    }

    // Followup messages

    fun sendMessage(content: String) = hook.sendMessage(content)

    fun sendMessage(messageBuilder: MessageBuilder) = hook.sendMessage(messageBuilder.build())

    fun sendMessage(builder: MessageBuilder.() -> Unit): WebhookMessageAction<Message> {
        val message = MessageBuilder().apply(builder).build()

        return hook.sendMessage(message)
    }

    fun sendEmbed(embed: MessageEmbed) = hook.sendMessageEmbeds(embed)

    fun sendEmbed(embedBuilder: EmbedBuilder) = hook.sendMessageEmbeds(embedBuilder.build())

    fun sendEmbed(builder: EmbedBuilder.() -> Unit): WebhookMessageAction<Message> {
        val embed = EmbedBuilder()
            .apply(builder).build()

        return hook.sendMessageEmbeds(embed)
    }

    var extra: AtomicReference<Any?>
}