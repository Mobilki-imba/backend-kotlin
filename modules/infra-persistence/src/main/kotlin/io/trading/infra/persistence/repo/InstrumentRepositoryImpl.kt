package io.trading.infra.persistence.repo

import io.trading.application.ports.InstrumentRepository
import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.InstrumentRow
import io.trading.infra.persistence.rows._InstrumentRow
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase
import java.time.Instant

class InstrumentRepositoryImpl(private val db: JdbcDatabase) : InstrumentRepository {
    private val i = _InstrumentRow.instrumentRow

    override suspend fun findById(id: InstrumentId): Instrument? {
        val q = QueryDsl.from(i).where { i.id eq id.raw }
        val rows: List<InstrumentRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun listAll(): List<Instrument> {
        val q = QueryDsl.from(i)
        val rows: List<InstrumentRow> = db.runQuery(q)
        return rows.map { it.toDomain() }
    }

    override suspend fun upsertAll(instruments: List<Instrument>) {
        if (instruments.isEmpty()) return
        val now = Instant.now()
        val rows = instruments.map { it.toRow(now) }
        val q = QueryDsl.insert(i).onDuplicateKeyUpdate().multiple(rows)
        db.runQuery(q)
    }
}
