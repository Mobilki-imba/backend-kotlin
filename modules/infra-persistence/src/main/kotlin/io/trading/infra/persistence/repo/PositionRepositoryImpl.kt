package io.trading.infra.persistence.repo

import io.trading.application.ports.PositionRepository
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.portfolio.Position
import io.trading.domain.user.UserId
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.PositionRow
import io.trading.infra.persistence.rows._PositionRow
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase

class PositionRepositoryImpl(private val db: JdbcDatabase) : PositionRepository {
    private val p = _PositionRow.positionRow

    override suspend fun find(userId: UserId, instrumentId: InstrumentId): Position? {
        val q = QueryDsl.from(p).where {
            p.userId eq userId.raw
            p.instrumentId eq instrumentId.raw
        }
        val rows: List<PositionRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun listForUser(userId: UserId): List<Position> {
        val q = QueryDsl.from(p).where { p.userId eq userId.raw }
        val rows: List<PositionRow> = db.runQuery(q)
        return rows.map { it.toDomain() }
    }

    override suspend fun upsert(position: Position) {
        val row = position.toRow()
        val q = QueryDsl.insert(p).onDuplicateKeyUpdate().single(row)
        db.runQuery(q)
    }
}
