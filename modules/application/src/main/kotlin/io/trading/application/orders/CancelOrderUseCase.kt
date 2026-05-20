package io.trading.application.orders

import io.trading.application.errors.OrderNotCancellableException
import io.trading.application.errors.OrderNotFoundException
import io.trading.application.errors.OrderNotOwnedException
import io.trading.application.ports.Clock
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.OrderEventRepository
import io.trading.application.ports.OrderRepository
import io.trading.application.ports.ReservationRepository
import io.trading.application.ports.TransactionManager
import io.trading.application.ports.UserRepository
import io.trading.domain.event.OrderEvent
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.order.Side
import io.trading.domain.user.UserId

class CancelOrderUseCase(
    private val users: UserRepository,
    private val orders: OrderRepository,
    private val reservations: ReservationRepository,
    private val events: OrderEventRepository,
    private val tx: TransactionManager,
    private val clock: Clock,
    private val bus: OrderEventBus,
) {
    suspend operator fun invoke(userId: UserId, orderId: OrderId) {
        val order = orders.findById(orderId) ?: throw OrderNotFoundException(orderId.raw.toString())
        if (order.userId != userId) throw OrderNotOwnedException()
        if (!order.status.isActive) throw OrderNotCancellableException(order.status.name)
        val now = clock.now()

        tx.inTransaction {
            val reservation = reservations.findByOrderId(orderId)
            if (reservation != null && order.side == Side.BUY) {
                val user = users.findById(userId) ?: throw IllegalStateException("user missing")
                users.updateCashBalance(user.id, user.cashBalance + reservation.amount)
            }
            reservations.deleteByOrderId(orderId)
            orders.updateOnCancel(orderId, now)
            events.insert(
                OrderEvent.Cancelled(
                    eventId = java.util.UUID.randomUUID(),
                    userId = userId, orderId = orderId, occurredAt = now,
                ),
            )
        }
        bus.publish(
            OrderEvent.Cancelled(
                eventId = java.util.UUID.randomUUID(),
                userId = userId, orderId = orderId, occurredAt = now,
            ),
        )
    }
}
