# RUNBOOK — Trading BFF

Операционная книга для локального и dev окружений.

## Запуск с нуля

```bash
docker compose -f docker/docker-compose.yml up -d postgres redis
./gradlew :apps:bff:run
```

Health: `curl http://localhost:8080/health/live`. OpenAPI: `http://localhost:8080/docs`.

## Reset БД полностью

```bash
docker compose -f docker/docker-compose.yml down -v
docker compose -f docker/docker-compose.yml up -d postgres redis
./gradlew :apps:bff:run    # Flyway применит миграции на чистой БД
```

## Подключиться к Postgres

```bash
docker exec -it trading-postgres psql -U trading -d trading
```

Полезные запросы:
```sql
SELECT count(*) FROM users;
SELECT id, side, type, status, quantity_lots FROM orders ORDER BY created_at DESC LIMIT 20;
SELECT * FROM cash_reservations;
SELECT * FROM order_events WHERE dispatched_at IS NULL;  -- незаполненная outbox
```

## Подключиться к Redis

```bash
docker exec -it trading-redis redis-cli
```

Полезное:
```
KEYS rl:*          # бакеты rate-limit
KEYS idem:*        # активные idempotency keys
```

## Профили запуска

| Профиль | Что включается |
|---|---|
| `MARKET_DATA_DRIVER=mock` (default) | In-process GBM-генератор тиков |
| `MARKET_DATA_DRIVER=grpc` (когда Go-бэк готов) | Реальный gRPC клиент на `MARKET_GRPC_ADDRESS` |
| `LOG_FORMAT=json` | JSON-логи (для прод) |
| `LOG_FORMAT=text` (default) | Pretty-print для dev |

## Сценарий smoke-теста вручную

```bash
# 1. Регистрация
curl -X POST localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"Strong1Pass"}'

# 2. Сохранить accessToken из ответа
TOKEN="<accessToken>"

# 3. Список инструментов
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/instruments

# 4. Создать MARKET BUY
curl -X POST localhost:8080/api/v1/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" \
  -H 'Content-Type: application/json' \
  -d '{"instrumentId":1,"side":"BUY","type":"MARKET","quantity":10}'

# 5. Портфель
curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/v1/portfolio

# 6. WebSocket
wscat -c "ws://localhost:8080/api/v1/ws/market" \
  -H "Authorization: Bearer $TOKEN"
# > {"type":"subscribe","channels":["quote:1"]}
```

## Метрики

- `curl localhost:8080/metrics` — Prometheus scrape endpoint.
- Ключевые: `ktor_http_server_requests_seconds_*`, `orders_placed_total`, `ws_connections_active`.

## Известные грабли

- **Komapper R2DBC + транзакции**: внутри `TransactionManager.inTransaction { ... }` нельзя использовать `launch`/`async` — дочерние корутины не унаследуют `CoroutineContext` с подключением. Только последовательный suspend-код. См. KDoc `TransactionManager`.
- **bcrypt cost 12** на event-loop — `PasswordHasher` использует выделенный `Dispatchers.Default.limitedParallelism(cores-2)` чтобы не блокировать Netty.
- **WS медленный клиент** — `outgoing.send` оборачивается в `withTimeout(1000)`. При таймауте сессия закрывается.
- **Outbox dispatcher** — использует `SELECT ... FOR UPDATE SKIP LOCKED` для мульти-инстанс безопасности.

## Регенерация JWT-секрета

В dev — секрет генерируется автоматически при старте (см. `apps/bff/src/main/kotlin/io/trading/bff/koin/AppModule.kt`). Для повторяемого запуска — задать `JWT_SECRET` env.

## Чистка зависших background процессов

```bash
pkill -f "io.trading.bff"
pkill -f GradleWorkerMain
lsof -i :8080  # должен быть пустым
```
