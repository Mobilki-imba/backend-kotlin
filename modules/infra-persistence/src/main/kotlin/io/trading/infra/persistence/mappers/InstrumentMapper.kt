package io.trading.infra.persistence.mappers

import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.instrument.Ticker
import io.trading.domain.money.Currency
import io.trading.domain.money.Money
import io.trading.infra.persistence.rows.InstrumentRow
import java.time.Instant

fun InstrumentRow.toDomain(): Instrument = Instrument(
    id = InstrumentId(id),
    ticker = Ticker(ticker),
    name = name,
    currency = Currency(currency),
    lotSize = lotSize,
    priceStep = Money.ofMicroUnits(priceStepMicro),
    isActive = isActive,
)

fun Instrument.toRow(updatedAt: Instant): InstrumentRow = InstrumentRow(
    id = id.raw,
    ticker = ticker.symbol,
    name = name,
    currency = currency.code,
    lotSize = lotSize,
    priceStepMicro = priceStep.microUnits,
    isActive = isActive,
    updatedAt = updatedAt,
)
