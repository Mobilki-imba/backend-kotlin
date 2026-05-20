package io.trading.application.ports

import io.trading.domain.money.Money
import io.trading.domain.order.OrderId
import io.trading.domain.portfolio.Reservation
import io.trading.domain.user.UserId

interface ReservationRepository {
    suspend fun findByOrderId(orderId: OrderId): Reservation?
    suspend fun totalReservedForUser(userId: UserId): Money
    suspend fun insert(reservation: Reservation)
    suspend fun deleteByOrderId(orderId: OrderId)
}
