package io.trading.infra.messaging

import io.trading.application.ports.OrderEventBus
import io.trading.domain.event.OrderEvent
import io.trading.domain.user.UserId
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter

class InProcessOrderEventBus(
    bufferSize: Int = 1024,
) : OrderEventBus {
    private val flow = MutableSharedFlow<OrderEvent>(
        replay = 0,
        extraBufferCapacity = bufferSize,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )

    override suspend fun publish(event: OrderEvent) {
        flow.emit(event)
    }

    override fun subscribeForUser(userId: UserId): Flow<OrderEvent> =
        flow.asSharedFlow().filter { it.userId == userId }
}
