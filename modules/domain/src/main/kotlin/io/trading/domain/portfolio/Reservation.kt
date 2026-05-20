package io.trading.domain.portfolio

import io.trading.domain.money.Money
import io.trading.domain.order.OrderId
import io.trading.domain.user.UserId

data class Reservation(
    val orderId: OrderId,
    val userId: UserId,
    val amount: Money,
) {
    init {
        require(amount.isPositive) { "reservation amount must be positive" }
    }
}
