package io.github.blad3mak3r.slash.extensions

import io.sentry.Sentry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object UncaughtExceptionHandler : Thread.UncaughtExceptionHandler {

    private val logger = LoggerFactory.getLogger(UncaughtExceptionHandler::class.java)

    override fun uncaughtException(t: Thread, e: Throwable) {
        logger.error("Caught exception in thread $t", e)
        Sentry.captureException(e)
    }

}

internal fun newThreadFactory(name: String,
                     corePoolSize: Int,
                     maximumPoolSize: Int,
                     keepAliveTime: Long = 5L,
                     unit: TimeUnit = TimeUnit.MINUTES,
                     daemon: Boolean = true
): ThreadPoolExecutor {
    return ThreadPoolExecutor(
        corePoolSize, maximumPoolSize,
        keepAliveTime, unit,
        LinkedBlockingQueue(),
        CustomThreadFactory(name, daemon)
    )
}

internal fun newCoroutineDispatcher(name: String,
                           corePoolSize: Int,
                           maximumPoolSize: Int,
                           keepAliveTime: Long = 5L,
                           unit: TimeUnit = TimeUnit.MINUTES,
                           daemon: Boolean = true
): CoroutineDispatcher {
    return newThreadFactory(name, corePoolSize, maximumPoolSize, keepAliveTime, unit, daemon).asCoroutineDispatcher()
}

internal class CustomThreadFactory(
    private val name: String,
    private val daemon: Boolean = false
) : ThreadFactory {
    private val threadNumber = AtomicInteger(1)

    override fun newThread(r: Runnable): Thread {
        val t = Thread(r)
        t.name = String.format(name, threadNumber.getAndIncrement())
        t.isDaemon = daemon
        t.uncaughtExceptionHandler = UncaughtExceptionHandler
        return t
    }
}