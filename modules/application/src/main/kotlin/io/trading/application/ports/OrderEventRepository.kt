package io.trading.application.ports

import io.trading.domain.event.OrderEvent
import kotlinx.datetime.Instant
import java.util.UUID

data class PendingOrderEvent(
    val eventId: UUID,
    val event: OrderEvent,
    val createdAt: Instant,
)

interface OrderEventRepository {
    suspend fun insert(event: OrderEvent)
    suspend fun fetchPendingForDispatch(olderThan: Instant, limit: Int): List<PendingOrderEvent>
    suspend fun markDispatched(eventIds: List<UUID>, dispatchedAt: Instant)
}
