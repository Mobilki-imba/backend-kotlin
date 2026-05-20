package io.trading.application.ports

import kotlin.time.Duration

interface RateLimiter {
    suspend fun tryAcquire(bucketKey: String, tokens: Long = 1L): RateLimitResult
}

sealed interface RateLimitResult {
    data object Allowed : RateLimitResult
    data class Denied(val retryAfter: Duration) : RateLimitResult
}
