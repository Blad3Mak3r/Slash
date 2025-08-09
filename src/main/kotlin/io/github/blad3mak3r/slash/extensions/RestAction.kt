package io.github.blad3mak3r.slash.extensions

import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

@Suppress("unused")
fun RestAction<*>.asEphemeral(): RestAction<*> {
    when(this) {
        is ReplyCallbackAction -> this.setEphemeral(true)
        is WebhookMessageCreateAction -> this.setEphemeral(true)
    }

    return this
}