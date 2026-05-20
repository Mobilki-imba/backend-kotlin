package io.trading.domain.trade

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderId
import io.trading.domain.order.Side
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

data class Trade(
    val id: TradeId,
    val orderId: OrderId,
    val userId: UserId,
    val instrumentId: InstrumentId,
    val side: Side,
    val price: Money,
    val quantity: Quantity,
    val commission: Money,
    val executedAt: Instant,
) {
    init {
        require(price.isPositive) { "price must be positive" }
        require(quantity.isPositive) { "quantity must be positive" }
        require(!commission.isNegative) { "commission must be non-negative" }
    }

    val notional: Money get() = price * quantity
}
