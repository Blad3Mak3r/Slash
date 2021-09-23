package tv.blademaker.slash.extensions

import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyAction

@Suppress("unused")
fun RestAction<*>.asEphemeral(): RestAction<*> {
    when(this) {
        is ReplyAction -> this.setEphemeral(true)
        is WebhookMessageAction<*> -> this.setEphemeral(true)
    }

    return this
}