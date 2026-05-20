package io.trading.infra.persistence.rows

import java.time.LocalDate

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("portfolio_snapshots")
data class PortfolioSnapshotRow(
    @KomapperId val userId: UUID,
    @KomapperId val snapshotDate: LocalDate,
    val totalValueMicro: Long,
)
