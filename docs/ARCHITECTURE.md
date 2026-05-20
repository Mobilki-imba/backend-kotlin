# Trading BFF — Архитектура

> Подробная архитектура Kotlin Client BFF для симулятора биржевой торговли.
> Сопровождает: [STACK.md](STACK.md), спецификация в [`trading-app-spec/`](../trading-app-spec/), gRPC контракт в [`GRPC_API.md`](../GRPC_API.md).

## 1. Контекст и границы

```
┌──────────┐  REST/WS    ┌─────────────────┐  gRPC      ┌──────────────┐  /dev/quotes  ┌────────┐
│ Android  │ ──────────▶ │  BFF (Kotlin)   │ ─────────▶ │ Market (Go)  │ ────────────▶ │ Driver │
└──────────┘             │                 │            └──────────────┘               └────────┘
                         │  ┌───────────┐  │
                         │  │ Postgres  │  │   ← бизнес-состояние (users, orders, positions, …)
                         │  └───────────┘  │
                         │  ┌───────────┐  │
                         │  │  Redis    │  │   ← rate-limit, idempotency, cross-BFF pub/sub
                         │  └───────────┘  │
                         └─────────────────┘
```

**Зона ответственности BFF:**
- Пользователи и сессии (auth/JWT/refresh).
- Создание, маршрутизация, исполнение и отмена заявок (MARKET немедленно, LIMIT через воркер).
- Виртуальные портфели и P&L.
- Прокси котировок: REST для свечей/стакана, WebSocket для live-стрима + приватных order-update.
- Идемпотентность, валидация бизнес-правил, целостность транзакций trade↔position↔cash.

**Не входит:**
- Генерация котировок (Driver).
- Хранение исторических тиков/свечей (Market на Go + ClickHouse).
- UI (Android).

## 2. Нефункциональные требования (целевые)

| Метрика | Цель |
|---|---|
| Throughput | ≥ 10k RPS на инстанс на read-heavy эндпоинтах |
| POST /orders (MARKET) | P95 < 200 мс |
| GET /portfolio | P95 < 100 мс |
| WS подключений на инстанс | ≥ 1000 |
| WS tick latency (внутри BFF) | P95 < 20 мс |
| Локальный запуск | `docker compose up postgres redis` + Go Market Data на `localhost:9090` + `./gradlew :apps:bff:run` |

## 3. Архитектурный стиль: Hexagonal / Clean

Чёткое разделение по слоям. Зависимости направлены **внутрь** (к домену).

```
       ┌─────────────────────────────────────────────────────────────┐
       │                       apps/bff (main)                       │
       │   Koin composition root + конфигурация + bootstrap          │
       └──┬──────────────────┬──────────────────┬────────────────────┘
          │                  │                  │
   ┌──────▼──────┐     ┌─────▼─────┐     ┌──────▼──────┐
   │  api-rest   │     │  api-ws   │     │   workers   │     ← driving adapters
   │   (Ktor)    │     │  (Ktor)   │     │ (coroutines)│
   └──────┬──────┘     └─────┬─────┘     └──────┬──────┘
          └────────────────┬─┴────────────────────┘
                           │
                ┌──────────▼───────────┐
                │     application      │           ← use-cases + ports
                │  (use-cases, ports)  │
                └──────────┬───────────┘
                           │
                ┌──────────▼───────────┐
                │       domain         │           ← entities, value-objects, invariants
                │   (без зависимостей) │
                └──────────────────────┘
                           ▲
       ┌───────────────────┼───────────────────┐
       │                   │                   │
┌──────┴───────┐  ┌────────┴────────┐  ┌───────┴─────────┐
│   infra-     │  │  infra-market-  │  │  infra-redis,   │   ← driven adapters (impl. ports)
│ persistence  │  │     data        │  │  infra-msg, …   │
│  (Komapper)  │  │     (gRPC)      │  │  (Lettuce)      │
└──────────────┘  └─────────────────┘  └─────────────────┘
```

**Правила зависимостей** (контролируем через Gradle dependency declarations + Konsist-тесты):

- `domain` зависит **ни от чего** кроме stdlib (и opt-in от kotlinx.datetime).
- `application` зависит только от `domain`.
- `infra-*` зависит от `application` (импортирует port'ы) и от своих библиотек (Komapper, Lettuce, grpc-kotlin).
- `api-*` зависит от `application` и от Ktor/сериализации.
- `apps/bff` зависит от всего — это композиционный корень.

Обратные стрелки запрещены. Domain никогда не знает о Postgres, Redis, gRPC, Ktor.

## 4. Структура модулей Gradle

```
backend-kotlin/
├── settings.gradle.kts
├── build.gradle.kts                     # convention plugins, общие версии
├── buildSrc/                            # или includeBuild, для kotlin-jvm/library convention
├── gradle/
│   └── libs.versions.toml
│
├── apps/
│   └── bff/                             # main() — собирает всё, поднимает Ktor + Koin
│       ├── src/main/kotlin/.../Application.kt
│       ├── src/main/resources/application.conf
│       └── src/main/resources/logback.xml
│
├── modules/
│   ├── domain/
│   │   └── src/main/kotlin/.../domain/
│   │       ├── money/      Money, Currency, Quantity, Price
│   │       ├── user/       User, UserId, Email
│   │       ├── instrument/ Instrument, InstrumentId, Ticker, LotSize, PriceStep
│   │       ├── order/      Order, OrderId, OrderStatus, OrderType, Side
│   │       ├── trade/      Trade, TradeId
│   │       ├── portfolio/  Position, CashBalance, Reservation, Portfolio
│   │       └── quote/      Quote, OrderBook, OrderBookLevel, Candle, Tick
│   │
│   ├── application/
│   │   └── src/main/kotlin/.../application/
│   │       ├── ports/      MarketDataPort, UserRepository, OrderRepository,
│   │       │               PositionRepository, TradeRepository, ReservationRepository,
│   │       │               TransactionManager, IdempotencyStore, RateLimiter, Clock
│   │       ├── auth/       RegisterUseCase, LoginUseCase, RefreshUseCase, LogoutUseCase
│   │       ├── orders/     PlaceMarketOrderUseCase, PlaceLimitOrderUseCase, CancelOrderUseCase,
│   │       │               ListOrdersUseCase, GetOrderUseCase, LimitOrderMatchingService
│   │       ├── portfolio/  GetPortfolioUseCase, GetTradesUseCase
│   │       ├── market/     GetInstrumentDetailsUseCase, ListInstrumentsUseCase,
│   │       │               GetCandlesUseCase, GetOrderBookUseCase, GetSparklineUseCase
│   │       └── ws/         WsSubscriptionService, OrderEventBus (publish)
│   │
│   ├── infra-persistence/
│   │   └── Komapper definitions, Postgres adapters реализующие *Repository
│   │
│   ├── infra-market-data/
│   │   ├── grpc/           GrpcMarketDataAdapter (real)
│   │   ├── grpc/           GrpcMarketDataAdapter, ProtoMappers, GrpcChannelFactory
│   │   └── hub/            MarketTickHub (fan-in/fan-out)
│   │
│   ├── infra-redis/
│   │   ├── ratelimit/      RedisRateLimiter (Bucket4j-redis)
│   │   ├── idempotency/    RedisIdempotencyStore
│   │   └── pubsub/         CrossInstanceOrderBus (multi-replica)
│   │
│   ├── infra-messaging/
│   │   └── InProcessOrderEventBus (Flow-based)
│   │
│   ├── api-rest/
│   │   ├── routes/         AuthRoutes, InstrumentRoutes, OrderRoutes, PortfolioRoutes, TradesRoutes
│   │   ├── dto/            Request/Response DTO с kotlinx.serialization
│   │   ├── errors/         StatusPages config, доменные → HTTP маппинг
│   │   ├── auth/           Ktor Auth feature, JWT validator
│   │   └── openapi/        OpenAPI spec (статичный YAML или генерация)
│   │
│   ├── api-ws/
│   │   ├── WebSocketRoutes.kt
│   │   ├── WsSession.kt    хранит подписки, last_seen_ts per channel
│   │   ├── WsProtocol.kt   DTO для subscribe/unsubscribe/ping/event
│   │   └── WsRouter.kt     маршрутизация в MarketTickHub / OrderEventBus
│   │
│   ├── auth/
│   │   ├── JwtService.kt   issue / verify, асимметричная подпись
│   │   ├── PasswordHasher.kt
│   │   └── RefreshTokenService.kt — ротация + revoke
│   │
│   └── workers/
│       ├── LimitOrderExecutor.kt    тикер ≤ 1с, сопоставление PENDING/PARTIAL с текущей ценой
│       ├── InstrumentSyncWorker.kt  раз в N минут синкает справочник из Go в БД + Caffeine
│       └── DailySnapshotWorker.kt   00:00 UTC — снапшот portfolio для dayPnl
│
├── proto/                                # .proto vendored из общего репо
│   └── marketdata/v1/marketdata.proto
│
├── docker/
│   ├── docker-compose.yml                # Postgres, Redis, BFF (Go Market Data — внешний процесс на :9090)
│   ├── Dockerfile                        # multi-stage build (Gradle → JRE-21)
│   └── prometheus.yml, grafana/          # опционально
│
└── docs/
    ├── ARCHITECTURE.md  (этот файл)
    ├── STACK.md
    ├── RUNBOOK.md       (после реализации)
    └── API.md           (OpenAPI экспорт)
```

## 5. Domain модель — ключевые типы

```kotlin
// Money — inline value class, носит micro-units (Long), не теряет точности
@JvmInline
value class Money internal constructor(val microUnits: Long) {
    operator fun plus(other: Money): Money = Money(microUnits + other.microUnits)
    operator fun minus(other: Money): Money = Money(microUnits - other.microUnits)
    operator fun times(qty: Quantity): Money = Money(microUnits * qty.lots)
    fun toDecimalString(): String = "${microUnits / 1_000_000}.${(microUnits % 1_000_000).toString().padStart(6, '0')}"
    companion object {
        fun ofMicroUnits(v: Long) = Money(v)
        fun ofDecimal(s: String): Money { /* парсинг с валидацией */ }
    }
}

@JvmInline value class Quantity(val lots: Long)
@JvmInline value class UserId(val raw: java.util.UUID)
@JvmInline value class OrderId(val raw: java.util.UUID)
@JvmInline value class InstrumentId(val raw: Int)
@JvmInline value class Ticker(val symbol: String)

enum class Side { BUY, SELL }
enum class OrderType { LIMIT, MARKET }
enum class OrderStatus { PENDING, PARTIAL, FILLED, CANCELLED, REJECTED }

data class Instrument(
    val id: InstrumentId,
    val ticker: Ticker,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val priceStep: Money,
    val isActive: Boolean,
)

data class Quote(
    val instrumentId: InstrumentId,
    val ticker: Ticker,
    val price: Money,
    val bid: Money,
    val ask: Money,
    val dayOpen: Money,
    val dayHigh: Money,
    val dayLow: Money,
    val dayVolume: Long,
    val changeBps: Int,         // 1 bps = 0.01%
    val timestamp: Instant,
)

data class Order(
    val id: OrderId,
    val userId: UserId,
    val instrumentId: InstrumentId,
    val side: Side,
    val type: OrderType,
    val limitPrice: Money?,        // только для LIMIT
    val quantity: Quantity,
    val filledQuantity: Quantity,
    val status: OrderStatus,
    val avgFillPrice: Money?,
    val commission: Money,
    val createdAt: Instant,
    val closedAt: Instant?,
) {
    fun remainingQuantity(): Quantity = Quantity(quantity.lots - filledQuantity.lots)
    fun isActive(): Boolean = status == OrderStatus.PENDING || status == OrderStatus.PARTIAL
}

data class Position(
    val userId: UserId,
    val instrumentId: InstrumentId,
    val quantity: Quantity,
    val avgPrice: Money,
)
```

**Инварианты** живут в фабриках/методах сущностей, не в use-case'ах:
- `Money.ofDecimal` валидирует формат и неотрицательность для цен.
- `Order.Companion.createLimit(...)` отказывает при `price == null`, `qty <= 0`, `qty % lotSize != 0`.
- `Position.applyFill(fill: Trade)` пересчитывает `avgPrice` с использованием весов.

## 6. Application — ports & use-cases

### Ports (driven, реализуются в `infra-*`)

```kotlin
interface MarketDataPort {
    suspend fun listInstruments(): List<Instrument>
    suspend fun getInstrument(id: InstrumentId): Instrument
    suspend fun getQuote(id: InstrumentId): Quote
    suspend fun getCandles(req: CandlesRequest): List<Candle>
    suspend fun getOrderBook(id: InstrumentId): OrderBook
    fun streamTicks(ids: Set<InstrumentId>): Flow<Tick>
    fun streamQuotesRange(req: QuotesRangeRequest): Flow<Tick>
}

interface UserRepository { /* find / insert / updateBalance */ }
interface OrderRepository { /* CRUD + listActive + listByUser cursor-paged */ }
interface PositionRepository
interface TradeRepository
interface ReservationRepository

interface TransactionManager {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

interface IdempotencyStore {
    suspend fun checkAndStore(key: String, ttl: Duration): IdempotencyOutcome
}

interface RateLimiter {
    suspend fun tryAcquire(bucketKey: String): RateLimitResult
}

interface Clock { fun now(): Instant }
```

### Use-cases (driving, вызываются adapter'ами `api-rest` / `workers`)

Каждый use-case = `class` с инжектируемыми port'ами и одним `suspend operator fun invoke(cmd: …): …`. Без анемичных «сервисов».

| Use-case | Что делает | Транзакция? |
|---|---|---|
| `RegisterUserUseCase` | bcrypt, insert user, дать starting balance, выпустить токены | да |
| `LoginUseCase` | rate-limit, проверка пароля, выпустить токены | нет |
| `RefreshUseCase` | взять refresh, провалидировать, ротация, выдать новый | да (отзыв старого + insert нового атомарно) |
| `LogoutUseCase` | revoke refresh | да |
| `ListInstrumentsUseCase` | из Caffeine cache, fallback на БД | нет |
| `GetInstrumentDetailsUseCase` | инструмент из кэша + `MarketDataPort.getQuote` | нет |
| `GetCandlesUseCase` | прокси в `MarketDataPort.getCandles` | нет |
| `GetOrderBookUseCase` | прокси в `MarketDataPort.getOrderBook` | нет |
| `GetSparklineUseCase` | `getCandles(interval=1m, limit=30)` + кэш ~5s | нет |
| `PlaceMarketOrderUseCase` | idempotency → валидации → `getQuote` → fill → транзакция (trade+position+cash) → outbox event | да |
| `PlaceLimitOrderUseCase` | idempotency → валидации → reserve cash/qty → insert PENDING → outbox event | да |
| `CancelOrderUseCase` | проверить владельца и статус → CANCELLED → release reservation → outbox event | да |
| `LimitOrderMatchingService` | вызывается из `LimitOrderExecutor` worker'а, исполняет PENDING/PARTIAL по текущей цене | да на каждый fill |
| `ListOrdersUseCase` | cursor-pagination, фильтр active/history | нет |
| `GetPortfolioUseCase` | cash + positions + параллельно `getQuote` для каждого инструмента | нет |
| `GetTradesUseCase` | cursor-pagination с фильтрами | нет |

### Outbox-pattern для WS-уведомлений

Чтобы НЕ делать 2PC между Postgres-транзакцией и WS-публикацией:

1. В транзакции с `trade/position/cash` пишем запись в таблицу `order_events` (`id, user_id, order_id, payload jsonb, status='pending'`).
2. После коммита транзакции — пушим event в `OrderEventBus` (in-process `MutableSharedFlow`). WS-router фильтрует по `user_id` и шлёт клиенту.
3. Отдельный лёгкий воркер «outbox dispatcher» периодически проверяет `pending` события старше 5 секунд (т.е. те, которые in-process bus не успел разослать из-за рестарта) и публикует их повторно. Idempotent на стороне клиента (event_id).
4. При мульти-инстансе BFF — `CrossInstanceOrderBus` (Redis pub/sub) транслирует событие во все инстансы, тот у кого WS-сессия пользователя — отдаёт клиенту.

## 7. Infra adapters

### infra-persistence (Komapper R2DBC)

Маппинг доменных типов на таблицы через явные DTO + extension `toDomain()` / `toRow()`. Domain-классы не аннотируются Komapper'ом — отдельные `*Row` data-классы со схемой.

Транзакции: `TransactionManager.inTransaction { … }` поверх `R2dbcDatabase.withTransaction { … }`. Уровень изоляции `READ COMMITTED` дефолтно, `SERIALIZABLE` для матчинга лимиток.

Connection pool: `r2dbc-pool` 50–100 соединений (точную цифру откалибруем k6-нагрузкой). `maxAcquireTime = 2s` — fail-fast вместо очереди на минуту.

### infra-market-data

**`MarketTickHub`** — сердце live-стрима, singleton:

```
                   ┌────────────────────────────────────────┐
                   │            MarketTickHub               │
                   │                                        │
   gRPC StreamTicks│  ┌─ instrument 1 ── SharedFlow<Tick> ──┼──▶ WS sessions
   (один upstream  │  ├─ instrument 2 ── SharedFlow<Tick> ──┼──▶ WS sessions
    на список ids) │  ├─ …                                  │
                   │  └─ instrument N ── SharedFlow<Tick> ──┼──▶ WS sessions
                   │                                        │
                   │  refCount per instrument →             │
                   │  при 0 подписчиков — отписываемся      │
                   └────────────────────────────────────────┘
```

- `MutableSharedFlow(replay = 1, extraBufferCapacity = 256, onBufferOverflow = DROP_OLDEST)` — клиент не успевает → теряет промежуточные, получает свежий.
- Один gRPC `StreamTicks` поднимается на список **всех подписанных** инструментов; пересоздаётся при изменении списка (с грейс-периодом 500 мс, чтобы не дёргать на каждом subscribe).
- Reconnect при разрыве gRPC — экспоненциальный backoff (100мс → 1s → 5s → 30s, jitter).

**Единственный адаптер `MarketDataPort`:**
- `GrpcMarketDataAdapter` — обёртка над сгенерированным grpc-kotlin stub'ом. Маппинг proto ↔ domain (`ProtoMappers.kt`), классификация ошибок (`GrpcErrorClassifier`: gRPC Status → доменные exceptions).
- `GrpcChannelFactory` — singleton `ManagedChannel` с `usePlaintext()` + keepalive 30s.

Конфигурация:
```hocon
market-data {
  grpc.address = "localhost:9090"   # переопределяется ENV MARKET_GRPC_ADDRESS
}
```

В Koin-модуле:
```kotlin
single { GrpcChannelFactory(cfg.marketData.grpc.address) }
single<MarketDataPort> { GrpcMarketDataAdapter(get()) }
```

### infra-redis

- `RedisRateLimiter` — Bucket4j с distributed lock на Redis. Бакеты: `login:{ip}+{email}` (5/15мин), `orders:{userId}` (50/min), `api:{userId}` (100 RPS soft cap).
- `RedisIdempotencyStore` — `SET key NX EX 300` с payload `{outcome_hash}`. На повторный запрос возвращает кэшированный результат вместо повторного исполнения.
- `CrossInstanceOrderBus` — pub/sub канал `orders:{userId}`, для случая когда WS-сессия пользователя живёт на другом инстансе.

## 8. Конкурентность под 10k RPS

**1. Полностью non-blocking pipeline.** Ktor (suspend) → use-case (suspend) → Komapper R2DBC (suspend) → r2dbc-postgresql (Netty). Ни одного `Thread.sleep`, ни одного блокирующего JDBC.

**2. Dispatcher'ы:**
- Default — для бизнес-логики и IO (R2DBC, gRPC, Lettuce уже асинхронны).
- `Dispatchers.IO.limitedParallelism(…)` — только для блокирующих кусков: bcrypt verification (CPU-bound), JWT signing с асимметричным ключом. Иначе один медленный bcrypt подвесит весь event loop.

**3. Stripe-locks для матчинга лимиток.** 256 (или 1024) `Mutex`ов, выбор по `userId.hashCode() % N`. Внутри одной полоски все ордера одного пользователя обрабатываются последовательно — без `SELECT FOR UPDATE` на горячих позициях. Лимиточный воркер берёт батч PENDING'ов и группирует по пользователям.

**4. Backpressure:**
- gRPC upstream → `MarketTickHub`: `flowOn(Dispatchers.Default) + buffer(256) + onBufferOverflow=DROP_OLDEST` для тиков (стейл-тик не нужен), `SUSPEND` для order events (терять нельзя).
- WS downstream: `outgoing.send` + `withTimeout(1000)` — если клиент не успевает за секунду, рвём соединение, пусть переподключается.

**5. Кэширование на чтении:**
- L1 (Caffeine): `instruments` справочник, `quotes` (TTL 200мс), `sparkline` (TTL 5s).
- L1: portfolio dayOpen snapshot — раз в день из БД.
- Cache stampede protection: Caffeine `AsyncLoadingCache` с одним loader-fight'ом на ключ.

**6. Connection pools:**
- Postgres: r2dbc-pool 50–100, целевая утилизация ≤ 70% под нагрузкой.
- Redis: одно мультиплексированное Lettuce-соединение.
- gRPC: один ManagedChannel, HTTP/2 мультиплексирует stream'ы.

**7. Минимум аллокаций на горячем пути:** value-классы (`Money`, `Quantity`), серилизация через kotlinx.serialization (без рефлексии), без рамок Jackson-ObjectMapper.

## 9. WebSocket: протокол и backfill

### Подключение

`GET /api/v1/ws/market` с `Authorization: Bearer <accessToken>` в upgrade-headers. После handshake:

- Каждая WS-сессия = объект `WsSession(userId, channels: MutableMap<Channel, Subscription>)`.
- Heartbeat: ping каждые 30с, разрыв при отсутствии pong 60с.

### Расширенный subscribe (расширение Контракта C — согласовать с Android)

```json
{
  "type": "subscribe",
  "channels": ["quote:1", "orderbook:1"],
  "last_seen_ts": 1715900400123    // optional, для backfill
}
```

**Алгоритм при subscribe с `last_seen_ts`:**

1. Сразу подписать сессию на `MarketTickHub` для нужных `instrumentId` (чтобы не пропустить live-тики во время backfill).
2. Параллельно запросить `MarketDataPort.streamQuotesRange(ids, from = last_seen_ts, to = now)` — стрим донабивки.
3. Тики из backfill-стрима отправляются клиенту первыми, помечены `"backfill": true`.
4. После `to_ns` Go закрывает stream — клиент получает свежие тики из `MarketTickHub` бесшовно.
5. Дедупликация на стороне BFF: храним `lastSentTs` per channel, не шлём тик с `ts < lastSentTs`.

### Каналы

| Канал | Источник | Видимость |
|---|---|---|
| `quote:{id}` | `MarketTickHub` (gRPC StreamTicks) | публично |
| `orderbook:{id}` | poll `MarketDataPort.getOrderBook` раз в 500мс + распостить подписчикам | публично |
| `orders` | `OrderEventBus` фильтр по `userId` | приватно |

`orderbook` через poll, потому что в gRPC контракте нет стрима стакана. Если Go добавит — переключим на стрим.

### Heartbeat и таймауты

- Idle WS без активности — закрыть через 5 мин (защита от утечки сессий).
- При `outgoing` блокировке > 1с — закрыть сессию (медленный клиент).

## 10. Безопасность

- **Пароли:** bcrypt cost 12, никогда не логируются.
- **JWT:** ES256 (NIST P-256) или EdDSA (Ed25519). Ключи хранятся как файлы (для локального dev) или из Vault (для прод-аналога). Кid в header, поддержка ротации.
- **Access token:** 1 час, claims: `sub=userId, iat, exp, jti`.
- **Refresh token:** 30 дней, **хранится в БД как hash**, ротация при использовании, отзыв единичный и каскадный (`POST /auth/logout-all`).
- **Rate-limit:** Bucket4j-Redis на login/register/orders. На WS-handshake — отдельный bucket per IP.
- **Idempotency:** `Idempotency-Key` header на POST /orders, TTL 5мин.
- **Маскирование в логах:** email, токены, password — через `MdcMasker` / `StructuredArgument.kv("email", maskEmail(e))`.
- **CORS:** white-list origins из конфига.
- **HTTPS:** TLS-терминация на reverse proxy (Caddy/nginx) — BFF слушает на :8080 в private сети.
- **Безопасность HTTP-заголовков:** Ktor `defaultHeaders` + `xContentTypeOptions`, `xFrameOptions`.

## 11. Observability

### Логи

- SLF4J + Logback + `logstash-logback-encoder` для JSON-вывода в stdout.
- В dev — pretty-print консоль; в Docker — JSON.
- MDC: `trace_id`, `span_id`, `user_id`, `request_id`, `correlation_id`. Заполняются Ktor-плагином `CallId` + OpenTelemetry MDC-инжектором.
- Структурно логировать только мутирующие операции (`POST /orders`, `DELETE /orders/{id}`, auth-операции).

### Метрики (Micrometer → Prometheus, `/metrics`)

- **HTTP:** `http_server_requests_seconds{method,uri,status}` гистограмма.
- **WS:** `ws_connections_active`, `ws_messages_sent_total{channel}`, `ws_subscriptions{channel}`.
- **gRPC client:** `grpc_client_calls_seconds{method,status}`, `grpc_client_active_streams`.
- **Orders:** `orders_placed_total{type,side,status}`, `orders_filled_seconds` (P50/P95/P99), `orders_pending_active`.
- **DB:** `r2dbc_pool_acquired`, `r2dbc_pool_pending`, `r2dbc_query_seconds`.
- **Cache:** Caffeine stats — `cache_hits_total`, `cache_misses_total`, `cache_size`.
- **JVM:** стандартные через Micrometer (`jvm_memory`, `jvm_gc_pause`, `system_cpu`).

### Трассировка

OpenTelemetry SDK с auto-instrumentation для Ktor, gRPC, R2DBC. Экспорт в Tempo/Jaeger через OTLP (опционально в локальной composе).

### Health

- `/health/live` — всегда 200, если процесс жив.
- `/health/ready` — 200 если: Postgres `SELECT 1` < 100мс, Redis `PING` < 50мс, MarketDataPort.listInstruments < 500мс.

## 12. Локальный запуск

```
docker compose up
```

Сервисы в compose:

| Service | Порт | Назначение |
|---|---|---|
| postgres | 5432 | основная БД |
| redis | 6379 | cache + rate-limit + pub/sub |
| bff | 8080 | сам сервис |
| (опц.) prometheus | 9090 | metrics scrape |
| (опц.) grafana | 3000 | дашборды |

Mock Go-бэка — **in-process** в BFF (`market-data.driver = mock` в `application.conf`). Никакого отдельного контейнера. Когда Go появится — меняется один параметр конфига.

Миграции Flyway применяются init-контейнером перед стартом BFF (`docker compose --profile migrate run flyway-migrate`).

## 13. Схема БД (Postgres)

```sql
-- users
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           CITEXT UNIQUE NOT NULL,
    password_hash   TEXT NOT NULL,
    display_name    TEXT,
    cash_balance_micro BIGINT NOT NULL CHECK (cash_balance_micro >= 0),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- refresh_tokens
CREATE TABLE refresh_tokens (
    token_hash      BYTEA PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user ON refresh_tokens(user_id) WHERE revoked_at IS NULL;

-- instruments (cинкается из Go раз в N мин)
CREATE TABLE instruments (
    id              INTEGER PRIMARY KEY,
    ticker          TEXT UNIQUE NOT NULL,
    name            TEXT NOT NULL,
    currency        TEXT NOT NULL,
    lot_size        INTEGER NOT NULL,
    price_step_micro BIGINT NOT NULL,
    is_active       BOOLEAN NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- orders
CREATE TABLE orders (
    id                  UUID PRIMARY KEY,
    user_id             UUID NOT NULL REFERENCES users(id),
    instrument_id       INTEGER NOT NULL REFERENCES instruments(id),
    side                TEXT NOT NULL CHECK (side IN ('BUY','SELL')),
    type                TEXT NOT NULL CHECK (type IN ('LIMIT','MARKET')),
    limit_price_micro   BIGINT,
    quantity_lots       BIGINT NOT NULL CHECK (quantity_lots > 0),
    filled_quantity_lots BIGINT NOT NULL DEFAULT 0,
    status              TEXT NOT NULL CHECK (status IN ('PENDING','PARTIAL','FILLED','CANCELLED','REJECTED')),
    avg_fill_price_micro BIGINT,
    commission_micro    BIGINT NOT NULL DEFAULT 0,
    idempotency_key     TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ
);
CREATE INDEX idx_orders_user_created ON orders(user_id, created_at DESC);
CREATE INDEX idx_orders_active ON orders(instrument_id, status) WHERE status IN ('PENDING','PARTIAL');
CREATE UNIQUE INDEX idx_orders_idem ON orders(user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- trades
CREATE TABLE trades (
    id              UUID PRIMARY KEY,
    order_id        UUID NOT NULL REFERENCES orders(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    instrument_id   INTEGER NOT NULL REFERENCES instruments(id),
    side            TEXT NOT NULL,
    price_micro     BIGINT NOT NULL,
    quantity_lots   BIGINT NOT NULL,
    commission_micro BIGINT NOT NULL,
    executed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trades_user_time ON trades(user_id, executed_at DESC);
CREATE INDEX idx_trades_user_inst_time ON trades(user_id, instrument_id, executed_at DESC);

-- positions
CREATE TABLE positions (
    user_id         UUID NOT NULL REFERENCES users(id),
    instrument_id   INTEGER NOT NULL REFERENCES instruments(id),
    quantity_lots   BIGINT NOT NULL,
    avg_price_micro BIGINT NOT NULL,
    PRIMARY KEY (user_id, instrument_id)
);

-- cash_reservations
CREATE TABLE cash_reservations (
    order_id        UUID PRIMARY KEY REFERENCES orders(id),
    user_id         UUID NOT NULL REFERENCES users(id),
    amount_micro    BIGINT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reservations_user ON cash_reservations(user_id);

-- daily snapshots для dayPnl
CREATE TABLE portfolio_snapshots (
    user_id         UUID NOT NULL REFERENCES users(id),
    snapshot_date   DATE NOT NULL,
    total_value_micro BIGINT NOT NULL,
    PRIMARY KEY (user_id, snapshot_date)
);

-- outbox для WS-уведомлений
CREATE TABLE order_events (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    order_id        UUID NOT NULL,
    event_type      TEXT NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    dispatched_at   TIMESTAMPTZ
);
CREATE INDEX idx_order_events_pending ON order_events(created_at) WHERE dispatched_at IS NULL;
```

## 14. План реализации (этапы)

| Этап | Что делаем | DoD |
|---|---|---|
| 0. Bootstrap | Gradle multi-module, version catalog, базовый Ktor app с `/health`, Logback, docker-compose с Postgres + Redis | `curl localhost:8080/health/live` → 200 |
| 1. Domain + application skeleton | Value-objects, entities, ports, пустые use-cases | Конкретные интерфейсы compile, заглушки. |
| 2. Persistence | Flyway-миграции, Komapper-репозитории, TransactionManager, Testcontainers тесты | Тесты CRUD по всем таблицам зелёные |
| 3. Auth | Register/Login/Refresh/Logout, JWT, bcrypt, rate-limit | e2e: register → login → /me ↔ access token |
| 4. Market Data + Instruments | GrpcMarketDataAdapter (к Go на :9090), MarketTickHub, REST `/instruments*`, Caffeine cache | `/instruments/{id}` отдаёт metadata + quote |
| 5. Orders + Portfolio | PlaceMarketOrder, PlaceLimitOrder, CancelOrder, GetPortfolio, LimitOrderExecutor worker | e2e: register → market order → portfolio показывает позицию |
| 6. WebSocket gateway | Sub/unsub, MarketTickHub fan-out, приватный канал orders через outbox, backfill через streamQuotesRange | Реконнект с last_seen_ts не теряет тики |
| 7. ~~Real gRPC adapter~~ | объединён с этапом 4 — mock удалён, есть только gRPC | done |
| 8. Observability | Micrometer + Prometheus, OpenTelemetry, structured logs | Дашборд показывает throughput и P95 |
| 9. OpenAPI + e2e | OpenAPI yaml, Postman-коллекция, e2e сценарий из DoD | Полный сценарий «register→login→order→portfolio» зелёный |
| 10. Load test | k6 сценарий, профилирование, тюнинг пулов | 10k RPS на read-эндпоинтах, P95 укладывается |
| 11. Docs | RUNBOOK.md, README, итоговый ARCHITECTURE update | Третий человек может поднять локально по README |

## 15. Открытые вопросы

- **WS-протокол backfill** — расширение Контракта C, требует согласования с Android-командой (`last_seen_ts` в subscribe).
- **`change_bps` → REST** — отдавать клиенту как `"changePct": "1.23"` (string decimal) или как int bps? Решить с Android.
- **Daily snapshot для P&L** — момент snapshot'а 00:00 UTC vs local time пользователя. Пока ставим UTC.
- **Авторизация на gRPC между BFF и Go** — спека говорит «потом mTLS», на старте plaintext по localhost.
- **Retry-policy на gRPC** — exponential backoff + circuit breaker (Resilience4j). Включаем после появления реального Go-бэка.
