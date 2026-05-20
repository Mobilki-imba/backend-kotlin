package io.trading.domain.portfolio

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.user.UserId

data class Portfolio(
    val userId: UserId,
    val cashBalance: Money,
    val positions: List<PositionWithMarket>,
) {
    val totalValue: Money
        get() = positions.fold(cashBalance) { acc, pos -> acc + pos.marketValue }
}

data class PositionWithMarket(
    val instrumentId: InstrumentId,
    val position: Position,
    val currentPrice: Money,
) {
    val marketValue: Money get() = currentPrice * position.quantity
    val unrealizedPnl: Money get() = (currentPrice - position.avgPrice) * position.quantity
    val unrealizedPnlBps: Int
        get() {
            if (position.avgPrice.isZero) return 0
            val diff = currentPrice.microUnits - position.avgPrice.microUnits
            return ((diff * 10_000L) / position.avgPrice.microUnits).toInt()
        }
}
