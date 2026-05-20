package io.trading.application.ports

import kotlin.time.Duration

interface IdempotencyStore {
    suspend fun checkAndStore(key: String, payload: String, ttl: Duration): IdempotencyOutcome
    suspend fun get(key: String): String?
}

sealed interface IdempotencyOutcome {
    data object Fresh : IdempotencyOutcome
    data class Replayed(val payload: String) : IdempotencyOutcome
}
