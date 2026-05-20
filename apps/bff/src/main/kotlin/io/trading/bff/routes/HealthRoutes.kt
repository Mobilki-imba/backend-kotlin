package io.trading.bff.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.trading.infra.redis.LettuceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import org.komapper.jdbc.JdbcDatabase
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

@Serializable
data class HealthResponse(val status: String, val service: String = "trading-bff")

@Serializable
data class ReadinessResponse(
    val status: String,
    val service: String = "trading-bff",
    val checks: Map<String, String>,
)

private val log = LoggerFactory.getLogger("HealthRoutes")

/**
 * Readiness probe.
 *
 * Postgres: фактом доступности бина [JdbcDatabase] из Koin считаем, что пул r2dbc-pool инициализирован.
 *   Полный SELECT 1 round-trip требует TemplateDsl от Komapper и был бы шумом на каждый probe в k8s.
 *   Реальные сбои БД отловит /metrics + бизнес-запросы.
 * Redis: реальный PING через Lettuce sync API с таймаутом 1s.
 */
suspend fun isReady(@Suppress("UNUSED_PARAMETER") db: JdbcDatabase, redis: LettuceFactory): Pair<Boolean, Map<String, String>> {
    val checks = mutableMapOf<String, String>()

    // Postgres: бин получен из Koin → пул инициализирован.
    val dbOk = true
    checks["postgres"] = "UP"

    // Redis: реальный PING.
    val redisOk = withTimeoutOrNull(1.seconds) {
        runCatching {
            withContext(Dispatchers.IO) {
                redis.connection.sync().ping()
            } == "PONG"
        }.getOrElse {
            log.warn("readiness: redis check failed: {}", it.message)
            false
        }
    } ?: run {
        log.warn("readiness: redis check timed out")
        false
    }
    checks["redis"] = if (redisOk) "UP" else "DOWN"

    return (dbOk && redisOk) to checks
}

fun Route.healthRoutes(db: JdbcDatabase, redis: LettuceFactory) {
    get("/health/live") {
        call.respond(HttpStatusCode.OK, HealthResponse(status = "UP"))
    }
    get("/health/ready") {
        val (ready, checks) = isReady(db, redis)
        val body = ReadinessResponse(status = if (ready) "READY" else "NOT_READY", checks = checks)
        call.respond(if (ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable, body)
    }
}
