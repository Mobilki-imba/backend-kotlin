package io.trading.infra.persistence.repo

import io.trading.application.ports.TradeRepository
import io.trading.application.ports.TradesQuery
import io.trading.domain.common.Page
import io.trading.domain.trade.Trade
import io.trading.infra.persistence.CursorCodec
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toJava
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.TradeRow
import io.trading.infra.persistence.rows._TradeRow
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase

class TradeRepositoryImpl(private val db: JdbcDatabase) : TradeRepository {
    private val t = _TradeRow.tradeRow

    override suspend fun insert(trade: Trade) {
        val q = QueryDsl.insert(t).single(trade.toRow())
        db.runQuery(q)
    }

    override suspend fun list(query: TradesQuery): Page<Trade> {
        val q = QueryDsl.from(t).where {
            t.userId eq query.userId.raw
            query.instrumentId?.let { iid -> t.instrumentId eq iid.raw }
            query.from?.let { f -> t.executedAt greaterEq f.toJava() }
            query.to?.let { to -> t.executedAt less to.toJava() }
            query.cursor?.let { c ->
                val d = CursorCodec.decode(c)
                t.executedAt less d.ts.toJava()
            }
        }
        val all: List<TradeRow> = db.runQuery(q)
        val ordered = all.sortedWith(
            compareByDescending<TradeRow> { it.executedAt }.thenByDescending { it.id },
        )
        val page = ordered.take(query.limit + 1)
        val items = page.take(query.limit).map { it.toDomain() }
        val next = if (page.size > query.limit) {
            val last = items.last()
            CursorCodec.encode(last.executedAt, last.id.raw)
        } else null
        return Page(items, next)
    }
}
