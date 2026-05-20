package io.trading.infra.persistence.mappers

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.portfolio.Position
import io.trading.domain.user.UserId
import io.trading.infra.persistence.rows.PositionRow

fun PositionRow.toDomain(): Position = Position(
    userId = UserId(userId),
    instrumentId = InstrumentId(instrumentId),
    quantity = Quantity(quantityLots),
    avgPrice = Money.ofMicroUnits(avgPriceMicro),
)

fun Position.toRow(): PositionRow = PositionRow(
    userId = userId.raw,
    instrumentId = instrumentId.raw,
    quantityLots = quantity.lots,
    avgPriceMicro = avgPrice.microUnits,
)
