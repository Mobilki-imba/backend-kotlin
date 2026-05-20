package io.trading.application.market

import io.trading.application.errors.InstrumentNotFoundException
import io.trading.application.ports.CandlesRequest
import io.trading.application.ports.MarketDataPort
import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.quote.Candle
import io.trading.domain.quote.CandleInterval
import io.trading.domain.quote.OrderBook
import io.trading.domain.quote.Quote
import kotlinx.datetime.Instant

interface InstrumentCache {
    suspend fun listAll(): List<Instrument>
    suspend fun get(id: InstrumentId): Instrument?
    suspend fun refresh()
}

class ListInstrumentsUseCase(private val cache: InstrumentCache) {
    suspend operator fun invoke(): List<Instrument> = cache.listAll()
}

class GetInstrumentDetailsUseCase(
    private val cache: InstrumentCache,
    private val marketData: MarketDataPort,
) {
    data class Result(val instrument: Instrument, val quote: Quote)

    suspend operator fun invoke(id: InstrumentId): Result {
        val instrument = cache.get(id) ?: throw InstrumentNotFoundException(id.raw)
        val quote = marketData.getQuote(id)
        return Result(instrument, quote)
    }
}

class GetCandlesUseCase(private val marketData: MarketDataPort) {
    suspend operator fun invoke(
        id: InstrumentId,
        interval: CandleInterval,
        from: Instant?,
        to: Instant?,
        limit: Int,
        includeOpen: Boolean,
    ): List<Candle> = marketData.getCandles(
        CandlesRequest(id, interval, from, to, limit.coerceIn(1, 1000), includeOpen),
    )
}

class GetOrderBookUseCase(private val marketData: MarketDataPort) {
    suspend operator fun invoke(id: InstrumentId): OrderBook = marketData.getOrderBook(id)
}

class GetSparklineUseCase(private val marketData: MarketDataPort) {
    suspend operator fun invoke(id: InstrumentId, points: Int = 30): List<Candle> =
        marketData.getCandles(
            CandlesRequest(id, CandleInterval.M1, null, null, points.coerceIn(1, 100), includeOpen = true),
        )
}
