package io.trading.domain.quote

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import kotlinx.datetime.Instant

data class OrderBookLevel(
    val price: Money,
    val quantity: Long,
)

data class OrderBook(
    val instrumentId: InstrumentId,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val timestamp: Instant,
)
