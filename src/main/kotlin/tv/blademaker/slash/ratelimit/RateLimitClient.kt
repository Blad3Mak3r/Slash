package tv.blademaker.slash.ratelimit

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.utils.appendCodeBlock
import tv.blademaker.slash.utils.appendLine

interface RateLimitClient {

    fun onRateLimitHit(ctx: SlashCommandContext, rateLimit: RateLimit, waitFor: Long) {
        val seconds = waitFor / 1000 % 60
        ctx.message {
            appendLine("\uD83D\uDED1 **RATE LIMIT**: Wait for **$seconds seconds** to execute this command.")
            appendLine()
            appendLine("Rate Limit bucket information:")
            appendCodeBlock("Bucket {\n\tQuota: ${rateLimit.quota}\n\tWindow: ${rateLimit.duration}\n\tUnit: ${rateLimit.unit}\n\tTarget: ${rateLimit.target}\n}", "hocon")
        }.setEphemeral(true).queue()
    }

    fun createBucketKey(annotation: RateLimit, event: SlashCommandInteractionEvent): String {
        return when (annotation.target) {
            RateLimit.Target.GUILD -> "limit:guild:${event.guild?.id ?: "unknown"}:${event.commandPath}"
            RateLimit.Target.CHANNEL -> "limit:channel:${event.channel.id}:${event.commandPath}"
            RateLimit.Target.USER -> "limit:user:${event.user.id}:${event.commandPath}"
        }
    }

    suspend fun acquire(annotation: RateLimit, event: SlashCommandInteractionEvent): Long?
}