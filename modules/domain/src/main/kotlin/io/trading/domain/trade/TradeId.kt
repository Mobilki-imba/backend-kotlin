package io.trading.domain.trade

import java.util.UUID

@JvmInline
value class TradeId(val raw: UUID) {
    override fun toString(): String = raw.toString()

    companion object {
        fun random(): TradeId = TradeId(UUID.randomUUID())
    }
}
