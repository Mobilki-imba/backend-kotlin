# trading-bff

Kotlin Client BFF для симулятора биржевой торговли — пользователи, заявки, портфели, прокси котировок к Market Data сервису (Go).

- **Архитектура:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — Hexagonal/Clean, multi-module Gradle.
- **Стек:** [`docs/STACK.md`](docs/STACK.md) — Ktor 3, Komapper R2DBC, Postgres, Redis, Koin, Caffeine, grpc-kotlin.
- **Контракты:**
  - Внешний gRPC к Go: [`GRPC_API.md`](GRPC_API.md)
  - REST/WS к Android: [`trading-app-spec/00-overview-contracts.md`](trading-app-spec/00-overview-contracts.md), Контракт C.
  - Функциональные требования BFF: [`trading-app-spec/03-backend-kotlin.md`](trading-app-spec/03-backend-kotlin.md).

## Быстрый запуск (локально)

Требования: JDK 21, Docker (colima / Docker Desktop).

```bash
# 1. Поднять инфраструктуру (Postgres + Redis)
docker compose -f docker/docker-compose.yml up -d postgres redis

# 2. Запустить BFF
./gradlew :apps:bff:run

# 3. Проверить
curl http://localhost:8080/health/live
# → {"status":"UP","service":"trading-bff"}
```

## Структура

```
apps/bff                 # composition root, main(), Koin
modules/
  domain                 # value-objects, entities, инварианты (без I/O)
  application            # use-cases + ports (interfaces)
  auth                   # JWT, bcrypt, refresh
  infra-persistence      # Komapper R2DBC, Postgres-репозитории
  infra-market-data      # MarketDataPort: gRPC adapter к Go-бэку (localhost:9090) + MarketTickHub
  infra-redis            # Rate-limit, idempotency, pub/sub
  infra-messaging        # In-process OrderEventBus
  api-rest               # Ktor routes, DTO
  api-ws                 # WebSocket gateway
  workers                # Limit-order executor, outbox dispatcher, instrument sync
docker/                  # docker-compose, Dockerfile
trading-app-spec/        # спецификация (общая с другими командами)
docs/                    # ARCHITECTURE.md, STACK.md
```

## Команды разработки

```bash
./gradlew build                     # сборка + тесты всех модулей
./gradlew :modules:domain:test      # тесты конкретного модуля
./gradlew :apps:bff:run             # требует Go Market Data на localhost:9090
```

## Конфигурация

См. `apps/bff/src/main/resources/application.conf`. Ключевые env vars:

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `BFF_PORT` | 8080 | HTTP-порт BFF |
| `DATABASE_URL` | `r2dbc:postgresql://localhost:5432/trading` | R2DBC URL |
| `DATABASE_USER`/`DATABASE_PASSWORD` | `trading`/`trading` | креды БД |
| `REDIS_URI` | `redis://localhost:6379` | Redis |
| `MARKET_GRPC_ADDRESS` | `localhost:9090` | адрес Go Market Data сервиса |
| `LOG_FORMAT` | `text` | `text` или `json` (для прод) |
