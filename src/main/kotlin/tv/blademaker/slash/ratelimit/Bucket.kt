package tv.blademaker.slash.ratelimit

import java.util.concurrent.atomic.AtomicInteger

data class Bucket(
    val remaining: AtomicInteger,
    val resetAfter: Long
) {
    constructor(annotation: RateLimit) : this(
        remaining = AtomicInteger(annotation.quota - 1),
        resetAfter = System.currentTimeMillis() + annotation.unit.toMillis(annotation.duration)
    )
}
