package io.trading.application.ports

import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId

interface InstrumentRepository {
    suspend fun findById(id: InstrumentId): Instrument?
    suspend fun listAll(): List<Instrument>
    suspend fun upsertAll(instruments: List<Instrument>)
}
