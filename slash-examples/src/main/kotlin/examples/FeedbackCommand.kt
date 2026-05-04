package examples

import io.github.blad3mak3r.slash.annotations.ApplicationCommand
import io.github.blad3mak3r.slash.annotations.OnButton
import io.github.blad3mak3r.slash.annotations.OnModal
import io.github.blad3mak3r.slash.annotations.OnSlashCommand
import io.github.blad3mak3r.slash.context.ButtonContext
import io.github.blad3mak3r.slash.context.ModalContext
import io.github.blad3mak3r.slash.context.SlashCommandContext

/**
 * /feedback  →  replies with a "Leave Feedback" button.
 * Button click  →  opens a modal.
 * Modal submit  →  confirms submission.
 *
 * Covers: @OnButton (pattern-matched), @OnModal (pattern-matched).
 */
@ApplicationCommand(name = "feedback")
class FeedbackCommand {

    @OnSlashCommand
    suspend fun handle(ctx: SlashCommandContext) {
        ctx.replyMessage("Click the button below to leave your feedback!").queue()
    }

    @OnButton(pattern = "feedback:submit")
    suspend fun onButton(ctx: ButtonContext) {
        ctx.reply("Button clicked — modal would open here.").setEphemeral(true).queue()
    }

    @OnModal(pattern = "feedback:modal:[0-9]+")
    suspend fun onModal(ctx: ModalContext) {
        val text = ctx.getValue("content")?.asString ?: "(empty)"
        ctx.reply("Thanks for your feedback: $text").setEphemeral(true).queue()
    }
}
