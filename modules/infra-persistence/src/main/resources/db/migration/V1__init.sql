-- Trading BFF — initial schema.
-- Все денежные значения хранятся в micro-units (BIGINT, цена × 1_000_000).

CREATE EXTENSION IF NOT EXISTS citext;

-- users
CREATE TABLE users (
    id                  UUID PRIMARY KEY,
    email               CITEXT UNIQUE NOT NULL,
    password_hash       TEXT NOT NULL,
    display_name        TEXT,
    cash_balance_micro  BIGINT NOT NULL CHECK (cash_balance_micro >= 0),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- refresh_tokens — храним только sha256 hash токена
CREATE TABLE refresh_tokens (
    token_hash      BYTEA PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_user_active
    ON refresh_tokens(user_id)
    WHERE revoked_at IS NULL;

-- instruments (синкается из Market Data сервиса)
CREATE TABLE instruments (
    id                  INTEGER PRIMARY KEY,
    ticker              TEXT UNIQUE NOT NULL,
    name                TEXT NOT NULL,
    currency            TEXT NOT NULL,
    lot_size            INTEGER NOT NULL CHECK (lot_size > 0),
    price_step_micro    BIGINT NOT NULL CHECK (price_step_micro > 0),
    is_active           BOOLEAN NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- orders
CREATE TABLE orders (
    id                      UUID PRIMARY KEY,
    user_id                 UUID NOT NULL REFERENCES users(id),
    instrument_id           INTEGER NOT NULL REFERENCES instruments(id),
    side                    TEXT NOT NULL CHECK (side IN ('BUY','SELL')),
    type                    TEXT NOT NULL CHECK (type IN ('LIMIT','MARKET')),
    limit_price_micro       BIGINT,
    quantity_lots           BIGINT NOT NULL CHECK (quantity_lots > 0),
    filled_quantity_lots    BIGINT NOT NULL DEFAULT 0 CHECK (filled_quantity_lots >= 0),
    status                  TEXT NOT NULL CHECK (status IN ('PENDING','PARTIAL','FILLED','CANCELLED','REJECTED')),
    avg_fill_price_micro    BIGINT,
    commission_micro        BIGINT NOT NULL DEFAULT 0 CHECK (commission_micro >= 0),
    idempotency_key         TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at               TIMESTAMPTZ,
    CONSTRAINT order_limit_price_when_limit
        CHECK ((type = 'MARKET' AND limit_price_micro IS NULL)
            OR (type = 'LIMIT'  AND limit_price_micro IS NOT NULL AND limit_price_micro > 0)),
    CONSTRAINT order_fill_le_quantity CHECK (filled_quantity_lots <= quantity_lots)
);
CREATE INDEX idx_orders_user_created  ON orders(user_id, created_at DESC, id);
CREATE INDEX idx_orders_active_match  ON orders(instrument_id, status) WHERE status IN ('PENDING','PARTIAL');
CREATE UNIQUE INDEX idx_orders_idem_unique
    ON orders(user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- trades
CREATE TABLE trades (
    id                  UUID PRIMARY KEY,
    order_id            UUID NOT NULL REFERENCES orders(id),
    user_id             UUID NOT NULL REFERENCES users(id),
    instrument_id       INTEGER NOT NULL REFERENCES instruments(id),
    side                TEXT NOT NULL CHECK (side IN ('BUY','SELL')),
    price_micro         BIGINT NOT NULL CHECK (price_micro > 0),
    quantity_lots       BIGINT NOT NULL CHECK (quantity_lots > 0),
    commission_micro    BIGINT NOT NULL CHECK (commission_micro >= 0),
    executed_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trades_user_time       ON trades(user_id, executed_at DESC, id);
CREATE INDEX idx_trades_user_inst_time  ON trades(user_id, instrument_id, executed_at DESC, id);

-- positions
CREATE TABLE positions (
    user_id             UUID NOT NULL REFERENCES users(id),
    instrument_id       INTEGER NOT NULL REFERENCES instruments(id),
    quantity_lots       BIGINT NOT NULL CHECK (quantity_lots >= 0),
    avg_price_micro     BIGINT NOT NULL CHECK (avg_price_micro >= 0),
    PRIMARY KEY (user_id, instrument_id)
);

-- cash_reservations: замороженные средства под LIMIT BUY (или quantity под LIMIT SELL — храним cash-эквивалент)
CREATE TABLE cash_reservations (
    order_id            UUID PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE,
    user_id             UUID NOT NULL REFERENCES users(id),
    amount_micro        BIGINT NOT NULL CHECK (amount_micro > 0),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_reservations_user ON cash_reservations(user_id);

-- portfolio_snapshots: для расчёта dayPnl (totalValue на 00:00 UTC)
CREATE TABLE portfolio_snapshots (
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    snapshot_date       DATE NOT NULL,
    total_value_micro   BIGINT NOT NULL,
    PRIMARY KEY (user_id, snapshot_date)
);

-- order_events: outbox для WS-уведомлений
CREATE TABLE order_events (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    order_id        UUID NOT NULL,
    event_type      TEXT NOT NULL CHECK (event_type IN ('CREATED','FILLED','CANCELLED','REJECTED')),
    payload         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    dispatched_at   TIMESTAMPTZ
);
CREATE INDEX idx_order_events_pending
    ON order_events(created_at)
    WHERE dispatched_at IS NULL;
