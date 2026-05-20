package io.trading.application.orders

import io.trading.application.errors.OrderNotFoundException
import io.trading.application.errors.OrderNotOwnedException
import io.trading.application.ports.OrderListFilter
import io.trading.application.ports.OrderRepository
import io.trading.domain.common.Cursor
import io.trading.domain.common.Page
import io.trading.domain.order.Order
import io.trading.domain.order.OrderId
import io.trading.domain.user.UserId

class ListOrdersUseCase(private val orders: OrderRepository) {
    suspend operator fun invoke(
        userId: UserId,
        filter: OrderListFilter,
        cursor: Cursor?,
        limit: Int,
    ): Page<Order> = orders.listForUser(userId, filter, cursor, limit.coerceIn(1, 200))
}

class GetOrderUseCase(private val orders: OrderRepository) {
    suspend operator fun invoke(userId: UserId, id: OrderId): Order {
        val o = orders.findById(id) ?: throw OrderNotFoundException(id.raw.toString())
        if (o.userId != userId) throw OrderNotOwnedException()
        return o
    }
}
