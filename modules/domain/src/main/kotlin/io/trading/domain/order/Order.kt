package io.trading.domain.order

import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

data class Order(
    val id: OrderId,
    val userId: UserId,
    val instrumentId: InstrumentId,
    val side: Side,
    val type: OrderType,
    val limitPrice: Money?,
    val quantity: Quantity,
    val filledQuantity: Quantity,
    val status: OrderStatus,
    val avgFillPrice: Money?,
    val commission: Money,
    val idempotencyKey: String?,
    val createdAt: Instant,
    val closedAt: Instant?,
) {
    init {
        require(quantity.isPositive) { "quantity must be positive" }
        require(filledQuantity <= quantity) { "filled cannot exceed quantity" }
        when (type) {
            OrderType.LIMIT -> require(limitPrice != null && limitPrice.isPositive) {
                "LIMIT order requires positive limitPrice"
            }
            OrderType.MARKET -> require(limitPrice == null) {
                "MARKET order must not have limitPrice"
            }
        }
        if (status.isTerminal) require(closedAt != null) { "terminal status requires closedAt" }
    }

    val remainingQuantity: Quantity get() = quantity - filledQuantity

    companion object {
        fun createMarket(
            id: OrderId,
            userId: UserId,
            instrument: Instrument,
            side: Side,
            quantity: Quantity,
            idempotencyKey: String?,
            createdAt: Instant,
        ): Order {
            require(instrument.isActive) { "instrument is inactive" }
            require(quantity.isMultipleOf(instrument.lotSize)) {
                "quantity must be a multiple of lotSize=${instrument.lotSize}"
            }
            return Order(
                id = id,
                userId = userId,
                instrumentId = instrument.id,
                side = side,
                type = OrderType.MARKET,
                limitPrice = null,
                quantity = quantity,
                filledQuantity = Quantity.ZERO,
                status = OrderStatus.PENDING,
                avgFillPrice = null,
                commission = Money.ZERO,
                idempotencyKey = idempotencyKey,
                createdAt = createdAt,
                closedAt = null,
            )
        }

        fun createLimit(
            id: OrderId,
            userId: UserId,
            instrument: Instrument,
            side: Side,
            quantity: Quantity,
            limitPrice: Money,
            idempotencyKey: String?,
            createdAt: Instant,
        ): Order {
            require(instrument.isActive) { "instrument is inactive" }
            require(quantity.isMultipleOf(instrument.lotSize)) {
                "quantity must be a multiple of lotSize=${instrument.lotSize}"
            }
            require(limitPrice.isPositive) { "limitPrice must be positive" }
            require(limitPrice.microUnits % instrument.priceStep.microUnits == 0L) {
                "limitPrice must be a multiple of priceStep=${instrument.priceStep}"
            }
            return Order(
                id = id,
                userId = userId,
                instrumentId = instrument.id,
                side = side,
                type = OrderType.LIMIT,
                limitPrice = limitPrice,
                quantity = quantity,
                filledQuantity = Quantity.ZERO,
                status = OrderStatus.PENDING,
                avgFillPrice = null,
                commission = Money.ZERO,
                idempotencyKey = idempotencyKey,
                createdAt = createdAt,
                closedAt = null,
            )
        }
    }
}
