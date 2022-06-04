package tv.blademaker.slash.client

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import tv.blademaker.slash.context.ContextCreator
import tv.blademaker.slash.context.impl.ContextCreatorImpl
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.exceptions.ExceptionHandlerImpl
import tv.blademaker.slash.internal.CommandExecutionCheck
import tv.blademaker.slash.metrics.MetricsStrategy
import tv.blademaker.slash.ratelimit.RateLimitClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DefaultSlashCommandBuilder(
    private val packageName: String
) {
    private var metrics: MetricsStrategy? = null

    private var contextCreator: ContextCreator? = null

    private var exceptionHandler: ExceptionHandler? = null

    private val checks = mutableSetOf<CommandExecutionCheck>()

    private var rateLimitClient: RateLimitClient? = null

    private var duration: Duration = 1.minutes

    fun enableMetrics(): DefaultSlashCommandBuilder {
        this.metrics = MetricsStrategy()
        return this
    }

    fun enableMetrics(builder: MetricsStrategy.() -> Unit): DefaultSlashCommandBuilder {
        this.metrics = MetricsStrategy().apply(builder)
        return this
    }

    fun contextCreator(contextCreator: ContextCreator): DefaultSlashCommandBuilder {
        this.contextCreator = contextCreator
        return this
    }

    fun addCheck(check: CommandExecutionCheck): DefaultSlashCommandBuilder {
        if (checks.contains(check)) error("check already registered.")
        checks.add(check)
        return this
    }

    fun setRateLimitClient(client: RateLimitClient?): DefaultSlashCommandBuilder {
        rateLimitClient = client
        return this
    }

    fun withTimeout(duration: Duration): DefaultSlashCommandBuilder {
        this.duration = duration
        return this
    }

    private fun build(): DefaultSlashCommandClient {
        return DefaultSlashCommandClient(
            packageName,
            exceptionHandler ?: ExceptionHandlerImpl(),
            contextCreator ?: ContextCreatorImpl(),
            checks,
            duration,
            rateLimitClient,
            metrics
        )
    }

    fun buildWith(jda: JDA): DefaultSlashCommandClient {
        val client = build()

        jda.addEventListener(client)

        return client
    }

    fun buildWith(shardManager: ShardManager): DefaultSlashCommandClient {
        val client = build()

        shardManager.addEventListener(client)

        return client
    }

}