package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("order_events")
data class OrderEventRow(
    @KomapperId
    val id: UUID,
    val userId: UUID,
    val orderId: UUID,
    val eventType: String,
    val payload: String,
    val createdAt: Instant,
    val dispatchedAt: Instant?,
)
