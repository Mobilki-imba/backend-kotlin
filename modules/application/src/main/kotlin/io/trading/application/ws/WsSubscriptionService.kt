package io.trading.application.ws

import io.trading.application.ports.OrderEventBus
import io.trading.domain.event.OrderEvent
import io.trading.domain.user.UserId
import kotlinx.coroutines.flow.Flow

/**
 * Хелпер-уровень для приватных WS-каналов (events для конкретного userId).
 * Используется api-ws слоем чтобы не дёргать OrderEventBus напрямую.
 */
class WsSubscriptionService(private val bus: OrderEventBus) {
    fun userOrders(userId: UserId): Flow<OrderEvent> = bus.subscribeForUser(userId)
}
