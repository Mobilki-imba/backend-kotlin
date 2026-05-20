package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable

@KomapperEntity
@KomapperTable("instruments")
data class InstrumentRow(
    @KomapperId
    val id: Int,
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val priceStepMicro: Long,
    val isActive: Boolean,
    val updatedAt: Instant,
)
