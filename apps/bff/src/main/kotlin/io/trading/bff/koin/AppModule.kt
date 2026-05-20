package io.trading.bff.koin

import io.trading.application.auth.LoginUseCase
import io.trading.application.auth.LogoutUseCase
import io.trading.application.auth.PasswordPolicyChecker
import io.trading.application.auth.PasswordHashing
import io.trading.application.auth.PasswordVerifying
import io.trading.application.auth.RefreshRevoker
import io.trading.application.auth.RefreshRotator
import io.trading.application.auth.RefreshUseCase
import io.trading.application.auth.RegisterUseCase
import io.trading.application.auth.TokenIssuer
import io.trading.application.market.GetCandlesUseCase
import io.trading.application.market.GetInstrumentDetailsUseCase
import io.trading.application.market.GetOrderBookUseCase
import io.trading.application.market.GetSparklineUseCase
import io.trading.application.market.InstrumentCache
import io.trading.application.market.ListInstrumentsUseCase
import io.trading.application.orders.CancelOrderUseCase
import io.trading.application.orders.CommissionCalculator
import io.trading.application.orders.GetOrderUseCase
import io.trading.application.orders.LimitOrderMatchingService
import io.trading.application.orders.ListOrdersUseCase
import io.trading.application.orders.PlaceLimitOrderUseCase
import io.trading.application.orders.PlaceMarketOrderUseCase
import io.trading.application.orders.StripedMutex
import io.trading.application.portfolio.GetPortfolioUseCase
import io.trading.application.portfolio.GetTradesUseCase
import io.trading.application.ports.Clock
import io.trading.application.ports.IdempotencyStore
import io.trading.application.ports.InstrumentRepository
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.OrderEventRepository
import io.trading.application.ports.OrderRepository
import io.trading.application.ports.PositionRepository
import io.trading.application.ports.RateLimiter
import io.trading.application.ports.RefreshTokenRepository
import io.trading.application.ports.ReservationRepository
import io.trading.application.ports.TradeRepository
import io.trading.application.ports.TransactionManager
import io.trading.application.ports.UserRepository
import io.trading.api.rest.instruments.CaffeineInstrumentCache
import io.trading.auth.JwtConfig
import io.trading.auth.JwtService
import io.trading.auth.PasswordHasher
import io.trading.auth.PasswordPolicy
import io.trading.auth.RefreshTokenService
import io.trading.bff.config.AppConfig
import io.trading.bff.observability.createMeterRegistry
import io.trading.domain.money.Money
import io.trading.domain.user.UserId
import io.trading.infra.market.grpc.GrpcChannelFactory
import io.trading.infra.market.grpc.GrpcMarketDataAdapter
import io.trading.infra.market.hub.MarketTickHub
import io.trading.infra.messaging.InProcessOrderEventBus
import io.trading.infra.persistence.CursorCodec
import io.trading.infra.persistence.DataSourceFactory
import io.trading.infra.persistence.DatabaseConfig as PdbConfig
import io.trading.infra.persistence.FlywayMigrator
import io.trading.infra.persistence.SystemClock
import io.trading.infra.persistence.TransactionManagerImpl
import io.trading.infra.persistence.repo.InstrumentRepositoryImpl
import io.trading.infra.persistence.repo.OrderEventRepositoryImpl
import io.trading.infra.persistence.repo.OrderRepositoryImpl
import io.trading.infra.persistence.repo.PositionRepositoryImpl
import io.trading.infra.persistence.repo.RefreshTokenRepositoryImpl
import io.trading.infra.persistence.repo.ReservationRepositoryImpl
import io.trading.infra.persistence.repo.TradeRepositoryImpl
import io.trading.infra.persistence.repo.UserRepositoryImpl
import io.trading.infra.redis.LettuceFactory
import io.trading.infra.redis.RateLimitRule
import io.trading.infra.redis.RedisIdempotencyStore
import io.trading.infra.redis.RedisRateLimiter
import io.trading.application.ws.WsSubscriptionService
import io.trading.workers.DailySnapshotWorker
import io.trading.workers.InstrumentSyncWorker
import io.trading.workers.LimitOrderExecutor
import io.trading.workers.OutboxDispatcher
import org.slf4j.LoggerFactory
import org.komapper.jdbc.JdbcDatabase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

fun appModule(cfg: AppConfig) = module {

    single { cfg }

    // ── Observability ────────────────────────────────────────────────────
    single { createMeterRegistry() }

    // ── Database (Komapper JDBC + HikariCP) ──────────────────────────────
    single<JdbcDatabase> {
        DataSourceFactory.create(
            PdbConfig(
                url = cfg.database.url,
                user = cfg.database.user,
                password = cfg.database.password,
                maximumPoolSize = cfg.database.pool.maxSize,
                minimumIdle = cfg.database.pool.initialSize,
                connectionTimeout = cfg.database.pool.maxAcquireTime.toJavaDuration(),
                idleTimeout = cfg.database.pool.maxIdleTime.toJavaDuration(),
                maxLifetime = cfg.database.pool.maxLifeTime.toJavaDuration(),
            ),
        )
    }
    single<TransactionManager> { TransactionManagerImpl(get()) }
    single<Clock> { SystemClock() }

    // ── Repositories ─────────────────────────────────────────────────────
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<RefreshTokenRepository> { RefreshTokenRepositoryImpl(get()) }
    single<InstrumentRepository> { InstrumentRepositoryImpl(get()) }
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    single<TradeRepository> { TradeRepositoryImpl(get()) }
    single<PositionRepository> { PositionRepositoryImpl(get()) }
    single<ReservationRepository> { ReservationRepositoryImpl(get()) }
    single<OrderEventRepository> { OrderEventRepositoryImpl(get()) }

    // ── Redis ────────────────────────────────────────────────────────────
    single { LettuceFactory(cfg.redis.uri) }
    single<RateLimiter> {
        RedisRateLimiter(
            connection = get<LettuceFactory>().connection,
            rules = mapOf(
                "login" to RateLimitRule(cfg.auth.rateLimit.loginAttempts.toLong(), cfg.auth.rateLimit.loginAttempts.toLong(), cfg.auth.rateLimit.loginWindow),
                "register" to RateLimitRule(cfg.auth.rateLimit.registerRps.toLong(), cfg.auth.rateLimit.registerRps.toLong(), 1.minutes),
            ),
            defaultRule = RateLimitRule(100, 100, 1.minutes),
        )
    }
    single<IdempotencyStore> { RedisIdempotencyStore(get<LettuceFactory>().connection) }

    // ── Auth ─────────────────────────────────────────────────────────────
    single {
        val secret = cfg.auth.jwt.secret ?: "dev-secret-change-me-${System.currentTimeMillis()}".padEnd(32, 'x')
        JwtService(
            JwtConfig(
                issuer = cfg.auth.jwt.issuer,
                audience = cfg.auth.jwt.audience,
                secret = secret,
                accessTtl = cfg.auth.accessTokenTtl,
            ),
            get(),
        )
    }
    single { PasswordHasher(cost = cfg.auth.bcryptCost) }
    single { RefreshTokenService(get(), get(), cfg.auth.refreshTokenTtl) }

    single<PasswordHashing> { object : PasswordHashing {
        private val h = get<PasswordHasher>()
        override suspend fun hash(password: String) = h.hash(password)
    } }
    single<PasswordVerifying> { object : PasswordVerifying {
        private val h = get<PasswordHasher>()
        override suspend fun verify(password: String, hash: String) = h.verify(password, hash)
    } }
    single<PasswordPolicyChecker> { object : PasswordPolicyChecker {
        override fun isValid(password: String) = PasswordPolicy.isValid(password)
    } }
    single<TokenIssuer> { object : TokenIssuer {
        private val jwt = get<JwtService>()
        private val refresh = get<RefreshTokenService>()
        override fun issueAccess(userId: UserId) = jwt.issueAccess(userId)
        override suspend fun issueRefresh(userId: UserId) = refresh.issue(userId)
    } }
    single<RefreshRotator> { object : RefreshRotator {
        private val r = get<RefreshTokenService>()
        override suspend fun rotate(rawToken: String) = r.rotate(rawToken)
    } }
    single<RefreshRevoker> { object : RefreshRevoker {
        private val r = get<RefreshTokenService>()
        override suspend fun revoke(rawToken: String) = r.revoke(rawToken)
    } }

    single {
        RegisterUseCase(
            users = get(), hasher = get(), issuer = get(), tx = get(),
            clock = get(), passwordPolicy = get(),
            startingBalance = Money.ofMicroUnits(cfg.auth.startingBalanceMicro),
        )
    }
    single { LoginUseCase(get(), get(), get(), get()) }
    single { RefreshUseCase(get(), get(), get()) }
    single { LogoutUseCase(get()) }

    // ── Market Data (gRPC к Go-бэку) ─────────────────────────────────────
    single { GrpcChannelFactory(cfg.marketData.grpc.address) }
    single<MarketDataPort> { GrpcMarketDataAdapter(get()) }
    single { MarketTickHub(get()) }
    single<InstrumentCache> { CaffeineInstrumentCache(get(), get()) }
    single { ListInstrumentsUseCase(get()) }
    single { GetInstrumentDetailsUseCase(get(), get()) }
    single { GetCandlesUseCase(get()) }
    single { GetOrderBookUseCase(get()) }
    single { GetSparklineUseCase(get()) }

    // ── Orders / Portfolio ───────────────────────────────────────────────
    single { CommissionCalculator(cfg.orders.market.commissionBps) }
    single<OrderEventBus> { InProcessOrderEventBus() }
    single { StripedMutex(1024) }

    single { PlaceMarketOrderUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PlaceLimitOrderUseCase(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single { CancelOrderUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { ListOrdersUseCase(get()) }
    single { GetOrderUseCase(get()) }
    single { LimitOrderMatchingService(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), cfg.orders.limitExecutor.partialFillProbability) }
    single { GetPortfolioUseCase(get(), get(), get()) }
    single { GetTradesUseCase(get()) }

    // ── WS helpers ───────────────────────────────────────────────────────
    single { WsSubscriptionService(get()) }

    // ── Workers ──────────────────────────────────────────────────────────
    single { InstrumentSyncWorker(get()) }
    single { LimitOrderExecutor(get(), cfg.orders.limitExecutor.tickIntervalMs.milliseconds) }
    single { OutboxDispatcher(get(), get(), get()) }
    single {
        // SnapshotTaker — пока заглушка с логом. Реальная имплементация требует
        // итератора по users + per-user GetPortfolio + insert в portfolio_snapshots.
        // Для MVP остаётся TODO; контракт worker'а готов.
        val dailyLog = LoggerFactory.getLogger("DailySnapshotWorker.Taker")
        DailySnapshotWorker(
            clock = get(),
            snapshotTaker = DailySnapshotWorker.SnapshotTaker { date ->
                dailyLog.info("TODO: snapshot portfolio for date={} (требует user iterator + GetPortfolio)", date)
            },
        )
    }

    // ── Misc ─────────────────────────────────────────────────────────────
    single(named("flyway")) {
        FlywayMigrator(
            jdbcUrl = FlywayMigrator.toJdbcUrl(cfg.database.url),
            user = cfg.database.user, password = cfg.database.password,
        )
    }
}
