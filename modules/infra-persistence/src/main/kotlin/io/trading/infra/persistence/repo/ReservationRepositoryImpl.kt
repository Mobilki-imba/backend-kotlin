package io.trading.infra.persistence.repo

import io.trading.application.ports.ReservationRepository
import io.trading.domain.money.Money
import io.trading.domain.order.OrderId
import io.trading.domain.portfolio.Reservation
import io.trading.domain.user.UserId
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.CashReservationRow
import io.trading.infra.persistence.rows._CashReservationRow
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase
import java.time.Instant

class ReservationRepositoryImpl(private val db: JdbcDatabase) : ReservationRepository {
    private val r = _CashReservationRow.cashReservationRow

    override suspend fun findByOrderId(orderId: OrderId): Reservation? {
        val q = QueryDsl.from(r).where { r.orderId eq orderId.raw }
        val rows: List<CashReservationRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun totalReservedForUser(userId: UserId): Money {
        // sum() через select() сложен с обобщениями — считаем агрегат в коде.
        val q = QueryDsl.from(r).where { r.userId eq userId.raw }
        val rows: List<CashReservationRow> = db.runQuery(q)
        val total = rows.sumOf { it.amountMicro }
        return Money.ofMicroUnits(total)
    }

    override suspend fun insert(reservation: Reservation) {
        val row = reservation.toRow(Instant.now())
        val q = QueryDsl.insert(r).single(row)
        db.runQuery(q)
    }

    override suspend fun deleteByOrderId(orderId: OrderId) {
        val q = QueryDsl.delete(r).where { r.orderId eq orderId.raw }
        db.runQuery(q)
    }
}
