package io.github.blad3mak3r.slash.client

import kotlinx.coroutines.flow.MutableSharedFlow
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.sharding.ShardManager
import io.github.blad3mak3r.slash.context.MessageCommandContext
import io.github.blad3mak3r.slash.context.SlashCommandContext
import io.github.blad3mak3r.slash.context.UserCommandContext
import io.github.blad3mak3r.slash.exceptions.ExceptionHandler
import io.github.blad3mak3r.slash.exceptions.ExceptionHandlerImpl
import io.github.blad3mak3r.slash.internal.Interceptor
import io.github.blad3mak3r.slash.internal.MessageCommandInterceptor
import io.github.blad3mak3r.slash.internal.SlashCommandInterceptor
import io.github.blad3mak3r.slash.internal.UserCommandInterceptor
import io.github.blad3mak3r.slash.metrics.MetricsStrategy
import io.github.blad3mak3r.slash.ratelimit.RateLimitClient
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SlashCommandClientBuilder internal constructor() {

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

    fun setExceptionHandler(handler: ExceptionHandler): SlashCommandClientBuilder {
        this.exceptionHandler = handler
        return this
    }

    fun addSlashInterceptor(interceptor: SlashCommandInterceptor): SlashCommandClientBuilder {
        check(!interceptors.contains(interceptor)) { "SlashCommandInterceptor already registered." }
        interceptors.add(interceptor)
        return this
    }

    fun addSlashInterceptor(builder: suspend (ctx: SlashCommandContext) -> Boolean): SlashCommandClientBuilder =
        addSlashInterceptor(object : SlashCommandInterceptor {
            override suspend fun intercept(ctx: SlashCommandContext) = builder(ctx)
        })

    fun addUserInterceptor(interceptor: UserCommandInterceptor): SlashCommandClientBuilder {
        check(!interceptors.contains(interceptor)) { "UserCommandInterceptor already registered." }
        interceptors.add(interceptor)
        return this
    }

    fun addUserInterceptor(builder: suspend (ctx: UserCommandContext) -> Boolean): SlashCommandClientBuilder =
        addUserInterceptor(object : UserCommandInterceptor {
            override suspend fun intercept(ctx: UserCommandContext) = builder(ctx)
        })

    fun addMessageInterceptor(interceptor: MessageCommandInterceptor): SlashCommandClientBuilder {
        check(!interceptors.contains(interceptor)) { "MessageCommandInterceptor already registered." }
        interceptors.add(interceptor)
        return this
    }

    fun addMessageInterceptor(builder: suspend (ctx: MessageCommandContext) -> Boolean): SlashCommandClientBuilder =
        addMessageInterceptor(object : MessageCommandInterceptor {
            override suspend fun intercept(ctx: MessageCommandContext) = builder(ctx)
        })

    fun addInterceptor(interceptor: Interceptor<*>): SlashCommandClientBuilder {
        check(!interceptors.contains(interceptor)) { "${interceptor::class.java.simpleName} already registered." }
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

    fun build(): SlashCommandClient = SlashCommandClient(
        eventsFlow ?: MutableSharedFlow(replay = 0),
        exceptionHandler ?: ExceptionHandlerImpl(),
        interceptors,
        duration,
        rateLimitClient,
        metrics
    )

    fun buildWith(jda: JDA): SlashCommandClient {
        check(eventsFlow == null) {
            "Cannot use buildWith() when you have set a custom eventsFlow; use build() instead."
        }
        return build().also { jda.addEventListener(it) }
    }

    fun buildWith(shardManager: ShardManager): SlashCommandClient {
        check(eventsFlow == null) {
            "Cannot use buildWith() when you have set a custom eventsFlow; use build() instead."
        }
        return build().also { shardManager.addEventListener(it) }
    }
}
