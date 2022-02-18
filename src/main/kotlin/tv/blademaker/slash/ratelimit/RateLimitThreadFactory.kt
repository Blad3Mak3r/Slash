package tv.blademaker.slash.ratelimit

import org.slf4j.LoggerFactory
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class RateLimitThreadFactory : ThreadFactory {

    companion object {
        private val threadId = AtomicInteger(1)
        private val log = LoggerFactory.getLogger(RateLimitThreadFactory::class.java)
    }

    override fun newThread(r: Runnable): Thread {
        val thread = Thread(r)
        thread.name = String.format("slash-ratelimit-%d", threadId.getAndIncrement())
        thread.isDaemon = true
        thread.uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { t, e ->
            log.error("Caught exception in thread $t", e)
        }
        return thread
    }


}