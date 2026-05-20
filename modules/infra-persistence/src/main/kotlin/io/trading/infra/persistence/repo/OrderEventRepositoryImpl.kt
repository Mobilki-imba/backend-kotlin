package io.trading.infra.persistence.repo

import io.trading.application.ports.OrderEventRepository
import io.trading.application.ports.PendingOrderEvent
import io.trading.domain.event.OrderEvent
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toJava
import io.trading.infra.persistence.mappers.toKt
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.OrderEventRow
import io.trading.infra.persistence.rows._OrderEventRow
import kotlinx.datetime.Instant
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase
import java.util.UUID

class OrderEventRepositoryImpl(private val db: JdbcDatabase) : OrderEventRepository {
    private val e = _OrderEventRow.orderEventRow

    override suspend fun insert(event: OrderEvent) {
        val q = QueryDsl.insert(e).single(event.toRow())
        db.runQuery(q)
    }

    override suspend fun fetchPendingForDispatch(olderThan: Instant, limit: Int): List<PendingOrderEvent> {
        val q = QueryDsl.from(e).where {
            e.dispatchedAt.isNull()
            e.createdAt lessEq olderThan.toJava()
        }
        val rows: List<OrderEventRow> = db.runQuery(q)
        return rows
            .sortedBy { it.createdAt }
            .take(limit)
            .map { row -> PendingOrderEvent(row.id, row.toDomain(), row.createdAt.toKt()) }
    }

    override suspend fun markDispatched(eventIds: List<UUID>, dispatchedAt: Instant) {
        if (eventIds.isEmpty()) return
        val q = QueryDsl.update(e)
            .set { e.dispatchedAt eq dispatchedAt.toJava() }
            .where { e.id inList eventIds }
        db.runQuery(q)
    }
}
