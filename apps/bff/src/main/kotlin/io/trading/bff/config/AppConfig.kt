package io.trading.bff.config

import com.sksamuel.hoplite.ConfigLoader
import kotlin.time.Duration

data class AppConfig(
    val server: ServerConfig,
    val database: DatabaseConfig,
    val redis: RedisConfig,
    val marketData: MarketDataConfig,
    val auth: AuthConfig,
    val orders: OrdersConfig,
    val websocket: WebSocketConfig,
)

data class ServerConfig(val port: Int, val host: String)

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val pool: DbPoolConfig,
)

data class DbPoolConfig(
    val initialSize: Int,
    val maxSize: Int,
    val maxIdleTime: Duration,
    val maxLifeTime: Duration,
    val maxAcquireTime: Duration,
)

data class RedisConfig(val uri: String)

data class MarketDataConfig(
    val grpc: GrpcConfig,
)

data class GrpcConfig(val address: String)

data class AuthConfig(
    val accessTokenTtl: Duration,
    val refreshTokenTtl: Duration,
    val bcryptCost: Int,
    val jwt: JwtCfg,
    val rateLimit: RateLimitCfg,
    val startingBalanceMicro: Long,
)

data class JwtCfg(val issuer: String, val audience: String, val secret: String? = null)

data class RateLimitCfg(
    val loginAttempts: Int,
    val loginWindow: Duration,
    val registerRps: Int,
)

data class OrdersConfig(
    val market: MarketOrdersCfg,
    val limitExecutor: LimitExecutorCfg,
    val idempotency: IdempotencyCfg,
)

data class MarketOrdersCfg(val commissionBps: Int)
data class LimitExecutorCfg(val tickIntervalMs: Long, val partialFillProbability: Double)
data class IdempotencyCfg(val ttl: Duration)

data class WebSocketConfig(
    val pingInterval: Duration,
    val timeout: Duration,
    val outgoingSendTimeout: Duration,
    val idleSessionTimeout: Duration,
)

object AppConfigLoader {
    fun load(): AppConfig = ConfigLoader().loadConfigOrThrow<AppConfig>("/application.conf")
}
