package io.trading.application.ports

import io.trading.domain.common.Cursor
import io.trading.domain.common.Page
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.Order
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

enum class OrderListFilter { ACTIVE, HISTORY, ALL }

interface OrderRepository {
    suspend fun findById(id: OrderId): Order?
    suspend fun findByIdempotencyKey(userId: UserId, key: String): Order?
    suspend fun insert(order: Order)
    suspend fun updateOnFill(
        id: OrderId,
        newFilledQuantity: Quantity,
        newStatus: OrderStatus,
        avgFillPrice: Money,
        commission: Money,
        closedAt: Instant?,
    )
    suspend fun updateOnCancel(id: OrderId, closedAt: Instant)
    suspend fun listForUser(userId: UserId, filter: OrderListFilter, cursor: Cursor?, limit: Int): Page<Order>
    suspend fun listActiveForMatching(limit: Int): List<Order>
}
