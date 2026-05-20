package io.trading.application.ports

import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.quote.Candle
import io.trading.domain.quote.CandleInterval
import io.trading.domain.quote.OrderBook
import io.trading.domain.quote.Quote
import io.trading.domain.quote.Tick
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

data class CandlesRequest(
    val instrumentId: InstrumentId,
    val interval: CandleInterval,
    val from: Instant?,
    val to: Instant?,
    val limit: Int,
    val includeOpen: Boolean,
)

data class QuotesRangeRequest(
    val instrumentIds: Set<InstrumentId>,
    val from: Instant?,
    val to: Instant?,
)

interface MarketDataPort {
    suspend fun listInstruments(): List<Instrument>
    suspend fun getInstrument(id: InstrumentId): Instrument
    suspend fun getQuote(id: InstrumentId): Quote
    suspend fun getCandles(req: CandlesRequest): List<Candle>
    suspend fun getOrderBook(id: InstrumentId): OrderBook
    fun streamTicks(ids: Set<InstrumentId>): Flow<Tick>
    fun streamQuotesRange(req: QuotesRangeRequest): Flow<Tick>
}
