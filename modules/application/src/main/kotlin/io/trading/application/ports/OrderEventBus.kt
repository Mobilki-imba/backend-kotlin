package io.trading.application.ports

import io.trading.domain.event.OrderEvent
import io.trading.domain.user.UserId
import kotlinx.coroutines.flow.Flow

interface OrderEventBus {
    suspend fun publish(event: OrderEvent)
    fun subscribeForUser(userId: UserId): Flow<OrderEvent>
}
