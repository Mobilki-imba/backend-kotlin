package io.trading.domain.quote

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.instrument.Ticker
import io.trading.domain.money.Money
import kotlinx.datetime.Instant

data class Tick(
    val instrumentId: InstrumentId,
    val ticker: Ticker,
    val timestamp: Instant,
    val price: Money,
    val volume: Long,
    val bid: Money,
    val ask: Money,
)
