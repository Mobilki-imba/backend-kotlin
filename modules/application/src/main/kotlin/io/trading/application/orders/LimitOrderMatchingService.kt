package io.trading.application.orders

import io.trading.application.ports.Clock
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.OrderEventRepository
import io.trading.application.ports.OrderRepository
import io.trading.application.ports.PositionRepository
import io.trading.application.ports.ReservationRepository
import io.trading.application.ports.TradeRepository
import io.trading.application.ports.TransactionManager
import io.trading.application.ports.UserRepository
import io.trading.domain.event.OrderEvent
import io.trading.domain.money.Quantity
import io.trading.domain.order.Order
import io.trading.domain.order.OrderStatus
import io.trading.domain.order.Side
import io.trading.domain.portfolio.Position
import io.trading.domain.trade.Trade
import io.trading.domain.trade.TradeId
import org.slf4j.LoggerFactory
import kotlin.random.Random

class LimitOrderMatchingService(
    private val users: UserRepository,
    private val orders: OrderRepository,
    private val positions: PositionRepository,
    private val trades: TradeRepository,
    private val reservations: ReservationRepository,
    private val events: OrderEventRepository,
    private val marketData: MarketDataPort,
    private val tx: TransactionManager,
    private val clock: Clock,
    private val commission: CommissionCalculator,
    private val locks: StripedMutex,
    private val bus: OrderEventBus,
    private val partialFillProbability: Double = 0.3,
) {
    private val log = LoggerFactory.getLogger(LimitOrderMatchingService::class.java)

    suspend fun matchBatch(limit: Int = 200) {
        val active = orders.listActiveForMatching(limit)
        if (active.isEmpty()) return
        active.groupBy { it.userId }.forEach { (uid, group) ->
            locks.forKey(uid).lock()
            try {
                group.forEach { tryMatch(it) }
            } finally {
                locks.forKey(uid).unlock()
            }
        }
    }

    private suspend fun tryMatch(order: Order) {
        val limitPrice = order.limitPrice ?: return
        val quote = runCatching { marketData.getQuote(order.instrumentId) }.getOrNull() ?: return
        val crosses = when (order.side) {
            Side.BUY -> quote.ask <= limitPrice
            Side.SELL -> quote.bid >= limitPrice
        }
        if (!crosses) return

        val remaining = order.remainingQuantity
        val fillQty = if (Random.nextDouble() < partialFillProbability && remaining.lots > order.quantity.lots / 2) {
            Quantity(remaining.lots / 2)
        } else remaining
        val fillPrice = limitPrice
        val fee = commission.forFill(fillPrice, fillQty)
        val now = clock.now()

        tx.inTransaction {
            val user = users.findById(order.userId) ?: return@inTransaction
            val newFilled = Quantity(order.filledQuantity.lots + fillQty.lots)
            val newStatus = if (newFilled == order.quantity) OrderStatus.FILLED else OrderStatus.PARTIAL
            val cumNotional = fillPrice.microUnits * newFilled.lots
            val avg = io.trading.domain.money.Money.ofMicroUnits(cumNotional / newFilled.lots)
            orders.updateOnFill(
                id = order.id,
                newFilledQuantity = newFilled,
                newStatus = newStatus,
                avgFillPrice = avg,
                commission = order.commission + fee,
                closedAt = if (newStatus == OrderStatus.FILLED) now else null,
            )
            trades.insert(
                Trade(
                    id = TradeId.random(),
                    orderId = order.id,
                    userId = order.userId,
                    instrumentId = order.instrumentId,
                    side = order.side,
                    price = fillPrice,
                    quantity = fillQty,
                    commission = fee,
                    executedAt = now,
                ),
            )
            val current = positions.find(order.userId, order.instrumentId)
                ?: Position.empty(order.userId, order.instrumentId)
            positions.upsert(
                current.applyFill(
                    Trade(
                        id = TradeId.random(),
                        orderId = order.id,
                        userId = order.userId,
                        instrumentId = order.instrumentId,
                        side = order.side,
                        price = fillPrice,
                        quantity = fillQty,
                        commission = fee,
                        executedAt = now,
                    ),
                ),
            )
            // Резерв освобождаем только при FILLED (упрощённо для MVP)
            if (newStatus == OrderStatus.FILLED) {
                reservations.deleteByOrderId(order.id)
                // BUY: возвращаем пользователю избыток (резерв - фактическая стоимость) — упрощённо игнорируем,
                // т.к. для MARKET цены могут отличаться от лимита. Здесь fillPrice == limit, поэтому ничего не возвращаем.
            }
            events.insert(
                OrderEvent.Filled(
                    eventId = java.util.UUID.randomUUID(),
                    userId = order.userId, orderId = order.id, occurredAt = now,
                    status = newStatus, filledQty = newFilled, avgPrice = avg,
                ),
            )
        }

        bus.publish(
            OrderEvent.Filled(
                eventId = java.util.UUID.randomUUID(),
                userId = order.userId, orderId = order.id, occurredAt = now,
                status = if (Quantity(order.filledQuantity.lots + fillQty.lots) == order.quantity) OrderStatus.FILLED else OrderStatus.PARTIAL,
                filledQty = Quantity(order.filledQuantity.lots + fillQty.lots),
                avgPrice = fillPrice,
            ),
        )
        log.debug("matched order {} qty={} price={}", order.id, fillQty.lots, fillPrice)
    }
}
