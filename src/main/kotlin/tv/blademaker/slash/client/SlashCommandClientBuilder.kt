package tv.blademaker.slash.client

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import tv.blademaker.slash.exceptions.ExceptionHandler
import tv.blademaker.slash.exceptions.ExceptionHandlerImpl
import tv.blademaker.slash.internal.Interceptor
import tv.blademaker.slash.internal.MessageCommandInterceptor
import tv.blademaker.slash.internal.SlashCommandInterceptor
import tv.blademaker.slash.internal.UserCommandInterceptor
import tv.blademaker.slash.metrics.MetricsStrategy
import tv.blademaker.slash.ratelimit.RateLimitClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SlashCommandClientBuilder internal constructor(
    private val packageName: String
) {
    private var metrics: MetricsStrategy? = null

    private var exceptionHandler: ExceptionHandler? = null

    private val interceptors = mutableSetOf<Interceptor<*>>()

    private var rateLimitClient: RateLimitClient? = null

    private var duration: Duration = 1.minutes

    fun enableMetrics(): SlashCommandClientBuilder {
        this.metrics = MetricsStrategy()
        return this
    }

    fun enableMetrics(builder: MetricsStrategy.() -> Unit): SlashCommandClientBuilder {
        this.metrics = MetricsStrategy().apply(builder)
        return this
    }

    fun addSlashInterceptor(interceptor: SlashCommandInterceptor): SlashCommandClientBuilder {
        if (interceptors.contains(interceptor)) error("SlashCommandInterceptor already registered.")
        interceptors.add(interceptor)
        return this
    }

    fun addUserInterceptor(interceptor: UserCommandInterceptor): SlashCommandClientBuilder {
        if (interceptors.contains(interceptor)) error("UserCommandInterceptor already registered.")
        interceptors.add(interceptor)
        return this
    }

    fun addMessageInterceptor(interceptor: MessageCommandInterceptor): SlashCommandClientBuilder {
        if (interceptors.contains(interceptor)) error("MessageCommandInterceptor already registered.")
        interceptors.add(interceptor)
        return this
    }

    fun addGlobalInterceptor(interceptor: Interceptor<*>) : SlashCommandClientBuilder {
        if (interceptors.contains(interceptor)) error("${interceptor::class.java.simpleName} already registered.")
        interceptors.add(interceptor)
        return this
    }

    fun setRateLimitClient(client: RateLimitClient?): SlashCommandClientBuilder {
        rateLimitClient = client
        return this
    }

    fun withTimeout(duration: Duration): SlashCommandClientBuilder {
        this.duration = duration
        return this
    }

    private fun build(): SlashCommandClient {
        return SlashCommandClient(
            packageName,
            exceptionHandler ?: ExceptionHandlerImpl(),
            interceptors,
            duration,
            rateLimitClient,
            metrics
        )
    }

    fun buildWith(jda: JDA): SlashCommandClient {
        val client = build()

        jda.addEventListener(client)

        return client
    }

    fun buildWith(shardManager: ShardManager): SlashCommandClient {
        val client = build()

        shardManager.addEventListener(client)

        return client
    }

}