package io.trading.domain.quote

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.instrument.Ticker
import io.trading.domain.money.Money
import kotlinx.datetime.Instant

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
    val changeBps: Int,
    val timestamp: Instant,
)
