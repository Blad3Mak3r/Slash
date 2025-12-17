package io.github.blad3mak3r.slash.ratelimit

interface Bucket {
    val id: String
    val remaining: Int
    val ttl: Long
}