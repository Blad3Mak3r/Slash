package tv.blademaker.slash.extensions

import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.Message
import io.sentry.protocol.SentryId
import org.slf4j.Logger

fun captureSentryEvent(logger: Logger? = null, builder: SentryEvent.() -> Unit): SentryId {
    val event = SentryEvent().apply(builder)
    val message = event.message?.message ?: event.throwable?.message
    val throwable = event.throwable ?: event.exceptions?.firstOrNull()

    if (message != null || throwable != null) {
        logger?.error(message, throwable)
    }
    return Sentry.captureEvent(event)
}

fun SentryEvent.message(message: String): SentryEvent {
    this.message = Message().apply { this.message = message }
    return this
}

fun SentryEvent.throwable(throwable: Throwable): SentryEvent {
    this.throwable = throwable
    return this
}