package io.trading.infra.redis

import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.trading.application.ports.IdempotencyOutcome
import io.trading.application.ports.IdempotencyStore
import kotlin.time.Duration

@OptIn(io.lettuce.core.ExperimentalLettuceCoroutinesApi::class)
class RedisIdempotencyStore(
    connection: StatefulRedisConnection<String, String>,
) : IdempotencyStore {

    private val redis = connection.coroutines()

    override suspend fun checkAndStore(key: String, payload: String, ttl: Duration): IdempotencyOutcome {
        val redisKey = "idem:$key"
        val set = redis.set(redisKey, payload, SetArgs.Builder.nx().ex(ttl.inWholeSeconds))
        return if (set == "OK") {
            IdempotencyOutcome.Fresh
        } else {
            val stored = redis.get(redisKey) ?: return IdempotencyOutcome.Fresh
            IdempotencyOutcome.Replayed(stored)
        }
    }

    override suspend fun get(key: String): String? = redis.get("idem:$key")
}
