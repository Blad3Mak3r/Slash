package tv.blademaker.slash.ratelimit

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.concurrent.*

class RateLimitHandler(configuration: Configuration) {

    private val task: ScheduledFuture<*>

    class Configuration {
        var purgeUnit: TimeUnit = TimeUnit.MINUTES
        var purgeDelay: Long = 10L
        var executor: ScheduledExecutorService? = null
    }

    companion object {
        private val log = LoggerFactory.getLogger(RateLimitHandler::class.java)
    }

    private val registry = ConcurrentHashMap<String, Bucket>()
    private val executor = configuration.executor ?: Executors.newSingleThreadScheduledExecutor(RateLimitThreadFactory())

    init {
        log.info("Initializing RateLimitHandler...")
        task = executor.scheduleAtFixedRate(purgeExpired(), configuration.purgeDelay, configuration.purgeDelay, configuration.purgeUnit)
    }

    private fun purgeExpired() = Runnable {
        log.info("Purging expired RateLimit Buckets...")
        var count = 0
        val now = System.currentTimeMillis()
        for (bucket in registry) {
            val key = bucket.key
            val resetAfter = bucket.value.resetAfter
            if (resetAfter < now) {
                registry.remove(key)
                count++
            }
        }
        log.info("Purge task done with a total of $count buckets expired.")
    }

    private fun createKey(annotation: RateLimit, event: SlashCommandInteractionEvent): String {
        return when (annotation.target) {
            RateLimit.Target.GUILD -> "G:${event.guild?.id ?: "***"}:${event.commandPath}"
            RateLimit.Target.CHANNEL -> "C:${event.channel.id}:${event.commandPath}"
            RateLimit.Target.USER -> "U:${event.user.id}:${event.commandPath}"
        }
    }

    fun acquire(annotation: RateLimit?, event: SlashCommandInteractionEvent): Long? {
        if (annotation == null) return null

        val key = createKey(annotation, event)
        log.debug("Fetching ratelimit for key $key")
        val bucket = registry[key]

        if (bucket == null) {
            log.debug("Ratelimit for $key not found, creating new one.")
            registry[key] = Bucket(annotation)
            return null
        }

        val now = System.currentTimeMillis()
        val remaining = bucket.remaining.getAndDecrement()
        val resetAfter = bucket.resetAfter

        if (resetAfter <= now) {
            registry.remove(key)
            return null
        }

        if (remaining <= 0) {
            return resetAfter - now
        }

        return null
    }
}