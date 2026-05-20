package io.trading.infra.persistence.repo

import io.trading.application.ports.OrderListFilter
import io.trading.application.ports.OrderRepository
import io.trading.domain.common.Cursor
import io.trading.domain.common.Page
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.Order
import io.trading.domain.order.OrderId
import io.trading.domain.order.OrderStatus
import io.trading.domain.user.UserId
import io.trading.infra.persistence.CursorCodec
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toJava
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.OrderRow
import io.trading.infra.persistence.rows._OrderRow
import kotlinx.datetime.Instant
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase

class OrderRepositoryImpl(private val db: JdbcDatabase) : OrderRepository {
    private val o = _OrderRow.orderRow

    override suspend fun findById(id: OrderId): Order? {
        val q = QueryDsl.from(o).where { o.id eq id.raw }
        val rows: List<OrderRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun findByIdempotencyKey(userId: UserId, key: String): Order? {
        val q = QueryDsl.from(o).where {
            o.userId eq userId.raw
            o.idempotencyKey eq key
        }
        val rows: List<OrderRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun insert(order: Order) {
        val q = QueryDsl.insert(o).single(order.toRow())
        db.runQuery(q)
    }

    override suspend fun updateOnFill(
        id: OrderId,
        newFilledQuantity: Quantity,
        newStatus: OrderStatus,
        avgFillPrice: Money,
        commission: Money,
        closedAt: Instant?,
    ) {
        val q = QueryDsl.update(o)
            .set {
                o.filledQuantityLots eq newFilledQuantity.lots
                o.status eq newStatus.name
                o.avgFillPriceMicro eq avgFillPrice.microUnits
                o.commissionMicro eq commission.microUnits
                o.closedAt eq closedAt?.toJava()
            }
            .where { o.id eq id.raw }
        db.runQuery(q)
    }

    override suspend fun updateOnCancel(id: OrderId, closedAt: Instant) {
        val q = QueryDsl.update(o)
            .set {
                o.status eq OrderStatus.CANCELLED.name
                o.closedAt eq closedAt.toJava()
            }
            .where { o.id eq id.raw }
        db.runQuery(q)
    }

    override suspend fun listForUser(
        userId: UserId,
        filter: OrderListFilter,
        cursor: Cursor?,
        limit: Int,
    ): Page<Order> {
        val q = QueryDsl.from(o).where {
            o.userId eq userId.raw
            when (filter) {
                OrderListFilter.ACTIVE -> o.status inList listOf("PENDING", "PARTIAL")
                OrderListFilter.HISTORY -> o.status inList listOf("FILLED", "CANCELLED", "REJECTED")
                OrderListFilter.ALL -> {}
            }
            cursor?.let { c ->
                val d = CursorCodec.decode(c)
                o.createdAt less d.ts.toJava()
            }
        }
        val all: List<OrderRow> = db.runQuery(q)
        val ordered = all.sortedWith(
            compareByDescending<OrderRow> { it.createdAt }.thenByDescending { it.id },
        )
        val page = ordered.take(limit + 1)
        val items = page.take(limit).map { it.toDomain() }
        val next = if (page.size > limit) {
            val last = items.last()
            CursorCodec.encode(last.createdAt, last.id.raw)
        } else null
        return Page(items, next)
    }

    override suspend fun listActiveForMatching(limit: Int): List<Order> {
        val q = QueryDsl.from(o).where { o.status inList listOf("PENDING", "PARTIAL") }
        val all: List<OrderRow> = db.runQuery(q)
        return all.sortedBy { it.createdAt }.take(limit).map { it.toDomain() }
    }
}
