package io.trading.bff

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.trading.api.rest.auth.authRoutes
import io.trading.api.rest.auth.installJwt
import io.trading.api.rest.errors.installStatusPages
import io.trading.api.rest.instruments.instrumentRoutes
import io.trading.api.rest.openapi.openApiRoutes
import io.trading.api.rest.orders.orderRoutes
import io.trading.api.rest.portfolio.portfolioRoutes
import io.trading.api.rest.trades.tradesRoutes
import io.trading.api.ws.installWebSockets
import io.trading.api.ws.marketWebSocket
import io.trading.application.auth.LoginUseCase
import io.trading.application.auth.LogoutUseCase
import io.trading.application.auth.RefreshUseCase
import io.trading.application.auth.RegisterUseCase
import io.trading.application.market.GetCandlesUseCase
import io.trading.application.market.GetInstrumentDetailsUseCase
import io.trading.application.market.GetOrderBookUseCase
import io.trading.application.market.GetSparklineUseCase
import io.trading.application.market.ListInstrumentsUseCase
import io.trading.application.orders.CancelOrderUseCase
import io.trading.application.orders.GetOrderUseCase
import io.trading.application.orders.ListOrdersUseCase
import io.trading.application.orders.PlaceLimitOrderUseCase
import io.trading.application.orders.PlaceMarketOrderUseCase
import io.trading.application.portfolio.GetPortfolioUseCase
import io.trading.application.portfolio.GetTradesUseCase
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.OrderEventBus
import io.trading.auth.JwtService
import io.trading.bff.config.AppConfig
import io.trading.bff.config.AppConfigLoader
import io.trading.bff.koin.appModule
import io.trading.bff.observability.installMetrics
import io.trading.bff.observability.metricsRoutes
import io.trading.bff.routes.healthRoutes
import io.trading.infra.market.hub.MarketTickHub
import io.trading.infra.persistence.FlywayMigrator
import io.trading.infra.redis.LettuceFactory
import io.trading.workers.DailySnapshotWorker
import io.trading.workers.InstrumentSyncWorker
import io.trading.workers.LimitOrderExecutor
import io.trading.workers.OutboxDispatcher
import kotlinx.serialization.json.Json
import org.komapper.jdbc.JdbcDatabase
import org.koin.core.Koin
import org.koin.core.context.GlobalContext.startKoin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.util.UUID

private val log = LoggerFactory.getLogger("Application")

fun main() {
    val cfg = AppConfigLoader.load()

    // Flyway миграции синхронно до открытия пула.
    FlywayMigrator(
        jdbcUrl = FlywayMigrator.toJdbcUrl(cfg.database.url),
        user = cfg.database.user, password = cfg.database.password,
    ).migrate()

    // Koin запускаем вручную (koin-ktor plugin несовместим с Ktor 3 — Routing стал interface).
    val koin: Koin = startKoin {
        slf4jLogger()
        modules(appModule(cfg))
    }.koin

    embeddedServer(
        factory = Netty,
        port = cfg.server.port,
        host = cfg.server.host,
        module = { module(cfg, koin) },
    ).start(wait = true)
}

fun Application.module(cfg: AppConfig, koin: Koin) {
    install(DefaultHeaders)
    install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
        header("X-Request-Id")
    }
    install(CallLogging) {
        level = Level.INFO
        mdc("request_id") { call -> call.callId }
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false })
    }
    installStatusPages()
    installMetrics(koin.get<PrometheusMeterRegistry>())

    val jwt = koin.get<JwtService>()
    installJwt(jwt.verifier)
    installWebSockets(cfg.websocket.pingInterval, cfg.websocket.timeout)

    routing {
        healthRoutes(koin.get<JdbcDatabase>(), koin.get<LettuceFactory>())
        metricsRoutes(koin.get<PrometheusMeterRegistry>())
        openApiRoutes()
        authRoutes(
            koin.get<RegisterUseCase>(),
            koin.get<LoginUseCase>(),
            koin.get<RefreshUseCase>(),
            koin.get<LogoutUseCase>(),
        )
        instrumentRoutes(
            koin.get<ListInstrumentsUseCase>(),
            koin.get<GetInstrumentDetailsUseCase>(),
            koin.get<GetCandlesUseCase>(),
            koin.get<GetOrderBookUseCase>(),
            koin.get<GetSparklineUseCase>(),
        )
        orderRoutes(
            koin.get<PlaceMarketOrderUseCase>(),
            koin.get<PlaceLimitOrderUseCase>(),
            koin.get<CancelOrderUseCase>(),
            koin.get<ListOrdersUseCase>(),
            koin.get<GetOrderUseCase>(),
        )
        portfolioRoutes(koin.get<GetPortfolioUseCase>())
        tradesRoutes(koin.get<GetTradesUseCase>())
        marketWebSocket(
            koin.get<MarketTickHub>(),
            koin.get<MarketDataPort>(),
            koin.get<OrderEventBus>(),
        )
    }

    // Воркеры
    koin.get<InstrumentSyncWorker>().start()
    koin.get<LimitOrderExecutor>().start()
    koin.get<OutboxDispatcher>().start()
    koin.get<DailySnapshotWorker>().start()
    log.info("Trading BFF started on port {}", cfg.server.port)
}
