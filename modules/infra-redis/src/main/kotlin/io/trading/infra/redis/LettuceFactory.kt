package io.trading.infra.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection

class LettuceFactory(private val uri: String) {
    private val client: RedisClient = RedisClient.create(RedisURI.create(uri))
    val connection: StatefulRedisConnection<String, String> = client.connect()

    fun close() {
        connection.close()
        client.shutdown()
    }
}
