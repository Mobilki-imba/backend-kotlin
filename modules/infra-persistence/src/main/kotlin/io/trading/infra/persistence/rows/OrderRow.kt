package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("orders")
data class OrderRow(
    @KomapperId
    val id: UUID,
    val userId: UUID,
    val instrumentId: Int,
    val side: String,
    val type: String,
    val limitPriceMicro: Long?,
    val quantityLots: Long,
    val filledQuantityLots: Long,
    val status: String,
    val avgFillPriceMicro: Long?,
    val commissionMicro: Long,
    val idempotencyKey: String?,
    val createdAt: Instant,
    val closedAt: Instant?,
)
