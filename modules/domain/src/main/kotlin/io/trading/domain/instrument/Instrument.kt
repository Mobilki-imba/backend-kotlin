package io.trading.domain.instrument

import io.trading.domain.money.Currency
import io.trading.domain.money.Money

data class Instrument(
    val id: InstrumentId,
    val ticker: Ticker,
    val name: String,
    val currency: Currency,
    val lotSize: Int,
    val priceStep: Money,
    val isActive: Boolean,
) {
    init {
        require(lotSize > 0) { "lotSize must be positive, got $lotSize" }
        require(priceStep.isPositive) { "priceStep must be positive" }
    }
}
