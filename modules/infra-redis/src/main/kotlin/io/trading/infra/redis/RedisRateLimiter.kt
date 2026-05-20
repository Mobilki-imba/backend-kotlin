package io.trading.infra.redis

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.trading.application.ports.RateLimitResult
import io.trading.application.ports.RateLimiter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class RateLimitRule(val capacity: Long, val refillPerWindow: Long, val window: Duration)

/**
 * Простая token-bucket реализация на Redis (без bucket4j зависимостей).
 * Ключ: bucket:<key>, хранит remaining-токены + lastRefill ts.
 */
@OptIn(io.lettuce.core.ExperimentalLettuceCoroutinesApi::class)
class RedisRateLimiter(
    connection: StatefulRedisConnection<String, String>,
    private val rules: Map<String, RateLimitRule>,
    private val defaultRule: RateLimitRule,
) : RateLimiter {

    private val redis: RedisCoroutinesCommands<String, String> = connection.coroutines()

    override suspend fun tryAcquire(bucketKey: String, tokens: Long): RateLimitResult {
        val rule = rulesForKey(bucketKey)
        val redisKey = "rl:$bucketKey"
        val now = System.currentTimeMillis()

        val storedTokens = redis.hget(redisKey, "tokens")?.toLongOrNull() ?: rule.capacity
        val storedTs = redis.hget(redisKey, "ts")?.toLongOrNull() ?: now

        val elapsed = (now - storedTs).coerceAtLeast(0L)
        val refilled = storedTokens + (elapsed.toDouble() / rule.window.inWholeMilliseconds * rule.refillPerWindow).toLong()
        val current = refilled.coerceAtMost(rule.capacity)

        return if (current >= tokens) {
            redis.hset(redisKey, mapOf("tokens" to (current - tokens).toString(), "ts" to now.toString()))
            redis.expire(redisKey, rule.window.inWholeSeconds * 2)
            RateLimitResult.Allowed
        } else {
            val deficit = tokens - current
            val waitMs = (deficit.toDouble() / rule.refillPerWindow * rule.window.inWholeMilliseconds).toLong()
            RateLimitResult.Denied(waitMs.milliseconds)
        }
    }

    private fun rulesForKey(key: String): RateLimitRule {
        val prefix = key.substringBefore(':', missingDelimiterValue = "")
        return rules[prefix] ?: defaultRule
    }
}
