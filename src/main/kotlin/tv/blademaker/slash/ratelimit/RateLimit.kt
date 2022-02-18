package tv.blademaker.slash.ratelimit

import java.util.concurrent.TimeUnit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RateLimit(
    val quota: Int,
    val duration: Long,
    val unit: TimeUnit,
    val target: Target
) {
    enum class Target {
        GUILD, CHANNEL, USER
    }
}
