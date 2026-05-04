package io.github.blad3mak3r.slash.ratelimit

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import io.github.blad3mak3r.slash.context.SlashCommandContext
import io.github.blad3mak3r.slash.extensions.commandPath
import io.github.blad3mak3r.slash.registry.RateLimitConfig
import io.github.blad3mak3r.slash.utils.appendCodeBlock
import io.github.blad3mak3r.slash.utils.appendLine

interface RateLimitClient {

    fun onRateLimitHit(ctx: SlashCommandContext, rateLimit: RateLimitConfig, waitFor: Long) {
        val seconds = waitFor / 1000 % 60
        ctx.message {
            appendLine("\uD83D\uDED1 **RATE LIMIT**: Wait for **$seconds seconds** to execute this command.")
            appendLine()
            appendLine("Rate Limit bucket information:")
            appendCodeBlock("Bucket {\n\tLimit: ${rateLimit.limit}\n\tPeriod: ${rateLimit.period}ms\n}", "hocon")
        }.setEphemeral(true).queue()
    }

    fun createBucketKey(rateLimit: RateLimitConfig, event: SlashCommandInteractionEvent): String {
        return "limit:user:${event.user.id}:${event.commandPath}"
    }

    suspend fun acquire(rateLimit: RateLimitConfig, event: SlashCommandInteractionEvent): Long?
}
