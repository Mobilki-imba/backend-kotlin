package io.trading.application.orders

import io.trading.application.errors.InsufficientFundsException
import io.trading.application.errors.InsufficientPositionException
import io.trading.application.errors.InstrumentNotFoundException
import io.trading.application.market.InstrumentCache
import io.trading.application.ports.Clock
import io.trading.application.ports.IdempotencyOutcome
import io.trading.application.ports.IdempotencyStore
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.OrderEventRepository
import io.trading.application.ports.OrderRepository
import io.trading.application.ports.PositionRepository
import io.trading.application.ports.ReservationRepository
import io.trading.application.ports.TransactionManager
import io.trading.application.ports.UserRepository
import io.trading.domain.event.OrderEvent
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.Order
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderType
import io.trading.domain.order.Side
import io.trading.domain.portfolio.Reservation
import io.trading.domain.user.UserId
import kotlin.time.Duration.Companion.minutes

data class PlaceLimitOrderCommand(
    val userId: UserId,
    val instrumentId: InstrumentId,
    val side: Side,
    val quantity: Quantity,
    val limitPrice: Money,
    val idempotencyKey: String?,
)

class PlaceLimitOrderUseCase(
    private val users: UserRepository,
    private val orders: OrderRepository,
    private val positions: PositionRepository,
    private val reservations: ReservationRepository,
    private val events: OrderEventRepository,
    private val instruments: InstrumentCache,
    private val tx: TransactionManager,
    private val clock: Clock,
    private val commission: CommissionCalculator,
    private val idempotency: IdempotencyStore,
    private val bus: OrderEventBus,
) {
    suspend operator fun invoke(cmd: PlaceLimitOrderCommand): Order {
        if (cmd.idempotencyKey != null) {
            val key = "order:${cmd.userId}:${cmd.idempotencyKey}"
            val outcome = idempotency.checkAndStore(key, "pending", 5.minutes)
            if (outcome is IdempotencyOutcome.Replayed) {
                orders.findByIdempotencyKey(cmd.userId, cmd.idempotencyKey)?.let { return it }
            }
        }

        val instrument = instruments.get(cmd.instrumentId)
            ?: throw InstrumentNotFoundException(cmd.instrumentId.raw)
        OrderValidator.validate(instrument, OrderType.LIMIT, cmd.quantity, cmd.limitPrice)

        val orderId = OrderId.random()
        val now = clock.now()
        val order = Order.createLimit(
            id = orderId,
            userId = cmd.userId,
            instrument = instrument,
            side = cmd.side,
            quantity = cmd.quantity,
            limitPrice = cmd.limitPrice,
            idempotencyKey = cmd.idempotencyKey,
            createdAt = now,
        )
        val fee = commission.forFill(cmd.limitPrice, cmd.quantity)
        val notional = cmd.limitPrice * cmd.quantity

        tx.inTransaction {
            val user = users.findById(cmd.userId) ?: throw IllegalStateException("user missing")
            when (cmd.side) {
                Side.BUY -> {
                    val freeze = notional + fee
                    if (user.cashBalance < freeze) throw InsufficientFundsException()
                    users.updateCashBalance(user.id, user.cashBalance - freeze)
                    orders.insert(order)
                    reservations.insert(Reservation(orderId, cmd.userId, freeze))
                }
                Side.SELL -> {
                    val pos = positions.find(cmd.userId, cmd.instrumentId)
                        ?: throw InsufficientPositionException()
                    if (pos.quantity < cmd.quantity) throw InsufficientPositionException()
                    orders.insert(order)
                    // Замораживаем cash-эквивалент (на случай партиал)
                    reservations.insert(Reservation(orderId, cmd.userId, notional))
                }
            }
            events.insert(
                OrderEvent.Created(
                    eventId = java.util.UUID.randomUUID(),
                    userId = cmd.userId, orderId = orderId, occurredAt = now,
                ),
            )
        }

        bus.publish(
            OrderEvent.Created(
                eventId = java.util.UUID.randomUUID(),
                userId = cmd.userId, orderId = orderId, occurredAt = now,
            ),
        )
        return order
    }
}
