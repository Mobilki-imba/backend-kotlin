package io.trading.infra.persistence.rows

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable

@KomapperEntity
@KomapperTable("positions")
data class PositionRow(
    @KomapperId val userId: java.util.UUID,
    @KomapperId val instrumentId: Int,
    val quantityLots: Long,
    val avgPriceMicro: Long,
)
