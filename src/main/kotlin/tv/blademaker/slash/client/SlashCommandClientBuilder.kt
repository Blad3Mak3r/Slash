package tv.blademaker.slash.client

import kotlinx.coroutines.flow.MutableSharedFlow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.sharding.ShardManager
import tv.blademaker.slash.context.MessageCommandContext
import tv.blademaker.slash.context.SlashCommandContext
import tv.blademaker.slash.context.UserCommandContext
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

    private var eventsFlow: MutableSharedFlow<GenericEvent>? = null

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

    fun addSlashInterceptor(builder: suspend (ctx: SlashCommandContext) -> Boolean): SlashCommandClientBuilder {
        return addSlashInterceptor(object : SlashCommandInterceptor {
            override suspend fun intercept(ctx: SlashCommandContext) = builder(ctx)
        })
    }

    fun addUserInterceptor(interceptor: UserCommandInterceptor): SlashCommandClientBuilder {
        if (interceptors.contains(interceptor)) error("UserCommandInterceptor already registered.")
        interceptors.add(interceptor)
        return this
    }

    fun addUserInterceptor(builder: suspend (ctx: UserCommandContext) -> Boolean): SlashCommandClientBuilder {
        return addUserInterceptor(object : UserCommandInterceptor {
            override suspend fun intercept(ctx: UserCommandContext) = builder(ctx)
        })
    }

    fun addMessageInterceptor(interceptor: MessageCommandInterceptor): SlashCommandClientBuilder {
        if (interceptors.contains(interceptor)) error("MessageCommandInterceptor already registered.")
        interceptors.add(interceptor)
        return this
    }

    fun addMessageInterceptor(builder: suspend (ctx: MessageCommandContext) -> Boolean): SlashCommandClientBuilder {
        return addMessageInterceptor(object : MessageCommandInterceptor {
            override suspend fun intercept(ctx: MessageCommandContext) = builder(ctx)
        })
    }

    fun addInterceptor(interceptor: Interceptor<*>) : SlashCommandClientBuilder {
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

    fun withEventFlow(eventsFlow: MutableSharedFlow<GenericEvent>): SlashCommandClientBuilder {
        this.eventsFlow = eventsFlow
        return this
    }

    fun build(): SlashCommandClient {
        return SlashCommandClient(
            packageName,
            eventsFlow ?: MutableSharedFlow(replay = 0),
            exceptionHandler ?: ExceptionHandlerImpl(),
            interceptors,
            duration,
            rateLimitClient,
            metrics
        )
    }

    fun buildWith(jda: JDA): SlashCommandClient {
        if (eventsFlow != null)
            error("Cannot use buildWith() when you set your own eventsFlow, use build() instead.")

        val client = build()

        jda.addEventListener(client)

        return client
    }

    fun buildWith(shardManager: ShardManager): SlashCommandClient {
        if (eventsFlow != null)
            error("Cannot use buildWith() when you set your own eventsFlow, use build() instead.")

        val client = build()

        shardManager.addEventListener(client)

        return client
    }

}