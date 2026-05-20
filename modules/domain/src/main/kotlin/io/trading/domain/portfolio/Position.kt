package io.trading.domain.portfolio

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.Side
import io.trading.domain.trade.Trade
import io.trading.domain.user.UserId

data class Position(
    val userId: UserId,
    val instrumentId: InstrumentId,
    val quantity: Quantity,
    val avgPrice: Money,
) {
    init {
        if (quantity.isZero) require(avgPrice.isZero) { "zero quantity must have zero avgPrice" }
        else require(avgPrice.isPositive) { "non-zero position requires positive avgPrice" }
    }

    fun applyFill(trade: Trade): Position {
        require(trade.userId == userId) { "trade userId mismatch" }
        require(trade.instrumentId == instrumentId) { "trade instrumentId mismatch" }
        return when (trade.side) {
            Side.BUY -> {
                val newQty = quantity + trade.quantity
                val newAvgMicro = if (newQty.isZero) 0L
                else (quantity.lots * avgPrice.microUnits + trade.quantity.lots * trade.price.microUnits) / newQty.lots
                copy(quantity = newQty, avgPrice = Money.ofMicroUnits(newAvgMicro))
            }
            Side.SELL -> {
                require(quantity >= trade.quantity) { "insufficient position to sell" }
                val newQty = quantity - trade.quantity
                copy(
                    quantity = newQty,
                    avgPrice = if (newQty.isZero) Money.ZERO else avgPrice,
                )
            }
        }
    }

    companion object {
        fun empty(userId: UserId, instrumentId: InstrumentId): Position =
            Position(userId, instrumentId, Quantity.ZERO, Money.ZERO)
    }
}
