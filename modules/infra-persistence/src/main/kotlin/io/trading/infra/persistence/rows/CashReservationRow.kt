package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("cash_reservations")
data class CashReservationRow(
    @KomapperId
    val orderId: UUID,
    val userId: UUID,
    val amountMicro: Long,
    val createdAt: Instant,
)
