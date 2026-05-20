package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("trades")
data class TradeRow(
    @KomapperId
    val id: UUID,
    val orderId: UUID,
    val userId: UUID,
    val instrumentId: Int,
    val side: String,
    val priceMicro: Long,
    val quantityLots: Long,
    val commissionMicro: Long,
    val executedAt: Instant,
)
