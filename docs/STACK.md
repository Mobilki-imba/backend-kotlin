# Trading BFF — Технологический стек

> Зафиксированные технические решения для Kotlin Client BFF.
> Сопровождает: [ARCHITECTURE.md](ARCHITECTURE.md), [README.md](../README.md).

## Принципы выбора

1. **Корутины — первичная модель параллелизма.** Никаких блокирующих JDBC/HTTP-клиентов на горячем пути.
2. **Kotlin-first инструменты, где это разумно.** kotlinx.serialization вместо Jackson, Ktor вместо Spring, Koin вместо Spring DI.
3. **Compile-time над runtime.** Кодоген (protobuf, Komapper), inline value-классы, без рефлексии где можно.
4. **Хорошо ложится в Hexagonal/Clean.** Каждая внешняя зависимость спрятана за port в `application`.
5. **Локально из коробки.** Один `docker compose up` поднимает всю инфраструктуру.

## Версии

| Категория | Технология | Версия (план) |
|---|---|---|
| Язык | Kotlin | 2.1.x |
| JVM | OpenJDK | 21 (LTS, виртуальные потоки доступны) |
| Build | Gradle | 8.10+ Kotlin DSL + version catalog |
| Web | Ktor | 3.0.x (Netty engine, CIO для тестов) |
| Coroutines | kotlinx.coroutines | 1.9.x |
| Serialization | kotlinx.serialization | 1.7.x |
| DI | Koin | 4.0.x |
| DB driver | r2dbc-postgresql | 1.0.x |
| ORM/SQL | Komapper R2DBC | 1.18.x |
| Connection pool | r2dbc-pool | 1.0.x |
| Migrations | Flyway | 10.x (JDBC, миграции отдельным шагом, не на старте) |
| gRPC | grpc-kotlin-stub + grpc-netty-shaded | 1.4.x / 1.68.x |
| Protobuf | protobuf-kotlin | 4.28.x |
| Redis | Lettuce + lettuce-core coroutines | 6.5.x |
| Cache | Caffeine | 3.1.x |
| JWT | Nimbus JOSE+JWT | 9.41.x |
| BCrypt | at.favre.lib:bcrypt | 0.10.x |
| Validation | konform | 0.7.x |
| Rate-limit | Bucket4j (core + redis-lettuce) | 8.10.x |
| Logging | SLF4J + Logback + logstash-logback-encoder | 2.0 / 1.5 / 8.0 |
| Metrics | Micrometer + micrometer-registry-prometheus | 1.14.x |
| Tracing | OpenTelemetry SDK + autoconfigure | 1.43.x |
| Testing | Kotest + MockK + Testcontainers | 5.9.x / 1.13.x / 1.20.x |
| Load | k6 (отдельным скриптом, не в репо JVM) | latest |

Точные версии — в `gradle/libs.versions.toml`, единственный источник правды.

## Обоснование ключевых выборов

### Веб-фреймворк: Ktor 3 (Netty)

**Выбран** потому что нативно корутинный (любой `route` — это `suspend`), у JetBrains, лёгкий runtime, WebSocket-плагин из коробки, удобный testing через `testApplication`. Не тащит за собой ServletContext/реактивный стек как Spring.

**Альтернативы:** Spring WebFlux + Kotlin coroutines — больше батареек (security, actuator, devtools), но громоздкий и иногда anti-Kotlin. http4k — функционально элегантен, но нишевый, слабее экосистема.

### DB: Komapper R2DBC + PostgreSQL

**Выбран** потому что:
- R2DBC — non-blocking I/O, корутинный (`coroutines` модуль идёт в комплекте), не сжирает потоки на 10k RPS.
- Komapper — typesafe DSL, kapt-кодоген маппинга, не требует annotation processing рантайма.
- Postgres — спека требует ACID-транзакций (trade + position + cash), cursor-пагинацию по timestamp, ссылочную целостность.

**Альтернативы:**
- **Exposed + HikariCP** — зрелее, но JDBC блокирующий → нужно гонять через `Dispatchers.IO`. Для 10k RPS требует огромного пула потоков, не подходит.
- **jOOQ + R2DBC** — самый типобезопасный SQL, но кодогенерация поверх живой БД усложняет разработку. Хорош для зрелых проектов.
- **Spring Data R2DBC** — связан с Spring, не нужен здесь.

### DI: Koin

**Выбран** потому что лёгкий, без рефлексии и annotation processing, конфигурация — Kotlin DSL. Идеально для Ktor.

**Альтернативы:** Kodein-DI (тоже хорош), Spring (тяжёлый), Dagger/Hilt (Android-first, оверкилл для бэка).

### Кэш: Caffeine

Лучший in-process LRU/W-TinyLFU кэш для JVM. Используется на двух уровнях:
- L1: справочник инструментов (TTL 10мин, `refresh-after-write` 5мин).
- L1: hot quotes (TTL 200мс) — для эндпоинтов где приемлемая stale-data.

L2 (Redis) — для:
- Distributed rate-limit (Bucket4j-Redis).
- Idempotency keys на POST /orders (TTL 5мин).
- Cross-instance pub/sub (если поднимаем больше одной реплики BFF).

### Redis: Lettuce

**Выбран** потому что: одно мультиплексированное соединение, нативный reactive API (легко обернуть в коретин). Поддержка pub/sub, streams.

**Альтернативы:** Kreds — нативный коретинный, но меньше экосистема. Jedis — блокирующий, не подходит.

### gRPC: grpc-kotlin + grpc-netty-shaded

Стандарт для Kotlin + gRPC. Stub'ы как `suspend fun` для unary, `Flow<T>` для server-stream — родное в корутинах.

### JWT: Nimbus

Поддержка асимметричных подписей (ES256/Ed25519), JWK, key rotation. Безопаснее симметричных секретов из 99% туториалов.

### Логи: structured JSON через logstash-encoder

Один формат для локального dev (pretty-print через консольный appender) и для прода (JSON в stdout → Loki/ELK). MDC хранит `trace_id`, `user_id`, `request_id`.

### Тесты: Kotest + MockK + Testcontainers

- **Kotest** — выразительный DSL (FreeSpec / BehaviorSpec), нативная поддержка корутин.
- **MockK** — единственный нормальный mocking framework под Kotlin (suspend, final-классы).
- **Testcontainers** — реальный Postgres + Redis в Docker для интеграционных тестов. **Не мокаем БД** — мок никогда не воспроизведёт `SERIALIZABLE` deadlock или constraint violation.

## Что НЕ берём и почему

| Технология | Почему нет |
|---|---|
| Spring Boot | Тяжёлый, не Kotlin-first, реактивный стек избыточен |
| Jackson | Рефлексия, медленнее kotlinx.serialization, runtime-конфиги |
| Exposed (с JDBC) | Блокирующий — не масштабируется до 10k RPS без огромного thread pool |
| Hibernate / JPA | Магия, lazy-loading проблемы, плохо для DDD |
| Vert.x | Не нативно Kotlin, callback-стиль |
| Quarkus / Micronaut | Сильны в нативных AOT-сборках, но не корутинно-первые |
| Apollo Kotlin (GraphQL) | REST + WS достаточно по спеке |
| Akka / Pekko | Actor model — оверкилл для CRUD-ориентированного BFF |

## Версионная политика

- Один источник правды — `gradle/libs.versions.toml`.
- Renovate / Dependabot конфиг с группировкой по экосистемам (Ktor, gRPC, Kotest вместе).
- Бамп major-версий — отдельным PR с тест-проверкой.
