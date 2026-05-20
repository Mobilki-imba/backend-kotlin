package io.trading.domain.event

import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant
import java.util.UUID

sealed interface OrderEvent {
    val eventId: UUID
    val userId: UserId
    val orderId: OrderId
    val occurredAt: Instant

    data class Created(
        override val eventId: UUID,
        override val userId: UserId,
        override val orderId: OrderId,
        override val occurredAt: Instant,
    ) : OrderEvent

    data class Filled(
        override val eventId: UUID,
        override val userId: UserId,
        override val orderId: OrderId,
        override val occurredAt: Instant,
        val status: OrderStatus,
        val filledQty: Quantity,
        val avgPrice: Money,
    ) : OrderEvent

    data class Cancelled(
        override val eventId: UUID,
        override val userId: UserId,
        override val orderId: OrderId,
        override val occurredAt: Instant,
    ) : OrderEvent

    data class Rejected(
        override val eventId: UUID,
        override val userId: UserId,
        override val orderId: OrderId,
        override val occurredAt: Instant,
        val reason: String,
    ) : OrderEvent
}
