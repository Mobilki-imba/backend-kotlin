package io.trading.infra.persistence.mappers

import io.trading.domain.money.Money
import io.trading.domain.order.OrderId
import io.trading.domain.portfolio.Reservation
import io.trading.domain.user.UserId
import io.trading.infra.persistence.rows.CashReservationRow
import java.time.Instant

fun CashReservationRow.toDomain(): Reservation = Reservation(
    orderId = OrderId(orderId),
    userId = UserId(userId),
    amount = Money.ofMicroUnits(amountMicro),
)

fun Reservation.toRow(createdAt: Instant): CashReservationRow = CashReservationRow(
    orderId = orderId.raw,
    userId = userId.raw,
    amountMicro = amount.microUnits,
    createdAt = createdAt,
)
