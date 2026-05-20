package io.trading.application.orders

import io.trading.application.errors.InsufficientFundsException
import io.trading.application.errors.InsufficientPositionException
import io.trading.application.errors.InstrumentNotFoundException
import io.trading.application.market.InstrumentCache
import io.trading.application.ports.Clock
import io.trading.application.ports.IdempotencyOutcome
import io.trading.application.ports.IdempotencyStore
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.OrderEventRepository
import io.trading.application.ports.OrderRepository
import io.trading.application.ports.PositionRepository
import io.trading.application.ports.TradeRepository
import io.trading.application.ports.TransactionManager
import io.trading.application.ports.UserRepository
import io.trading.domain.event.OrderEvent
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.Order
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.order.OrderType
import io.trading.domain.order.Side
import io.trading.domain.portfolio.Position
import io.trading.domain.trade.Trade
import io.trading.domain.trade.TradeId
import io.trading.domain.user.UserId
import kotlin.time.Duration.Companion.minutes

data class PlaceMarketOrderCommand(
    val userId: UserId,
    val instrumentId: InstrumentId,
    val side: Side,
    val quantity: Quantity,
    val idempotencyKey: String?,
)

class PlaceMarketOrderUseCase(
    private val users: UserRepository,
    private val orders: OrderRepository,
    private val positions: PositionRepository,
    private val trades: TradeRepository,
    private val events: OrderEventRepository,
    private val instruments: InstrumentCache,
    private val marketData: MarketDataPort,
    private val tx: TransactionManager,
    private val clock: Clock,
    private val commission: CommissionCalculator,
    private val idempotency: IdempotencyStore,
    private val bus: OrderEventBus,
) {
    suspend operator fun invoke(cmd: PlaceMarketOrderCommand): Order {
        if (cmd.idempotencyKey != null) {
            val key = "order:${cmd.userId}:${cmd.idempotencyKey}"
            val outcome = idempotency.checkAndStore(key, "pending", 5.minutes)
            if (outcome is IdempotencyOutcome.Replayed) {
                orders.findByIdempotencyKey(cmd.userId, cmd.idempotencyKey)?.let { return it }
            }
        }

        val instrument = instruments.get(cmd.instrumentId)
            ?: throw InstrumentNotFoundException(cmd.instrumentId.raw)
        OrderValidator.validate(instrument, OrderType.MARKET, cmd.quantity, null)

        val quote = marketData.getQuote(cmd.instrumentId)
        val fillPrice = if (cmd.side == Side.BUY) quote.ask else quote.bid
        val fee = commission.forFill(fillPrice, cmd.quantity)

        val orderId = OrderId.random()
        val now = clock.now()

        val order = Order.createMarket(
            id = orderId,
            userId = cmd.userId,
            instrument = instrument,
            side = cmd.side,
            quantity = cmd.quantity,
            idempotencyKey = cmd.idempotencyKey,
            createdAt = now,
        )

        var resultOrder: Order = order
        tx.inTransaction {
            val user = users.findById(cmd.userId) ?: throw IllegalStateException("user missing")
            val totalCost = fillPrice * cmd.quantity + fee

            when (cmd.side) {
                Side.BUY -> {
                    if (user.cashBalance < totalCost) throw InsufficientFundsException()
                    users.updateCashBalance(user.id, user.cashBalance - totalCost)
                }
                Side.SELL -> {
                    val pos = positions.find(cmd.userId, cmd.instrumentId)
                        ?: throw InsufficientPositionException()
                    if (pos.quantity < cmd.quantity) throw InsufficientPositionException()
                    users.updateCashBalance(user.id, user.cashBalance + (fillPrice * cmd.quantity - fee))
                }
            }

            val filled = order.copy(
                filledQuantity = cmd.quantity,
                status = OrderStatus.FILLED,
                avgFillPrice = fillPrice,
                commission = fee,
                closedAt = now,
            )
            orders.insert(filled)

            val trade = Trade(
                id = TradeId.random(),
                orderId = orderId,
                userId = cmd.userId,
                instrumentId = cmd.instrumentId,
                side = cmd.side,
                price = fillPrice,
                quantity = cmd.quantity,
                commission = fee,
                executedAt = now,
            )
            trades.insert(trade)

            val current = positions.find(cmd.userId, cmd.instrumentId)
                ?: Position.empty(cmd.userId, cmd.instrumentId)
            positions.upsert(current.applyFill(trade))

            val event = OrderEvent.Filled(
                eventId = java.util.UUID.randomUUID(),
                userId = cmd.userId,
                orderId = orderId,
                occurredAt = now,
                status = OrderStatus.FILLED,
                filledQty = cmd.quantity,
                avgPrice = fillPrice,
            )
            events.insert(event)
            resultOrder = filled
        }

        // best-effort push в in-process bus
        bus.publish(
            OrderEvent.Filled(
                eventId = java.util.UUID.randomUUID(),
                userId = cmd.userId,
                orderId = orderId,
                occurredAt = now,
                status = OrderStatus.FILLED,
                filledQty = cmd.quantity,
                avgPrice = fillPrice,
            ),
        )
        return resultOrder
    }
}
