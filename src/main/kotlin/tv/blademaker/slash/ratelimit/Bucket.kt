package tv.blademaker.slash.ratelimit

interface Bucket {
    val id: String
    val remaining: Int
    val ttl: Long
}