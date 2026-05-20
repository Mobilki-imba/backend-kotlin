package io.trading.infra.market.grpc

import io.trading.application.ports.CandlesRequest
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.QuotesRangeRequest
import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.quote.Candle
import io.trading.domain.quote.OrderBook
import io.trading.domain.quote.Quote
import io.trading.domain.quote.Tick
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import marketdata.v1.GetCandlesRequest
import marketdata.v1.GetInstrumentRequest
import marketdata.v1.GetOrderBookRequest
import marketdata.v1.GetQuoteRequest
import marketdata.v1.GetQuotesRangeRequest
import marketdata.v1.ListInstrumentsRequest
import marketdata.v1.MarketDataGrpcKt
import marketdata.v1.StreamTicksRequest

class GrpcMarketDataAdapter(
    private val channelFactory: GrpcChannelFactory,
) : MarketDataPort {

    private val stub = MarketDataGrpcKt.MarketDataCoroutineStub(channelFactory.channel)

    override suspend fun listInstruments(): List<Instrument> = GrpcErrorClassifier.wrap {
        stub.listInstruments(ListInstrumentsRequest.getDefaultInstance())
            .instrumentsList.map { it.toDomain() }
    }

    override suspend fun getInstrument(id: InstrumentId): Instrument =
        GrpcErrorClassifier.wrap(id.raw) {
            stub.getInstrument(
                GetInstrumentRequest.newBuilder().setInstrumentId(id.raw).build(),
            ).toDomain()
        }

    override suspend fun getQuote(id: InstrumentId): Quote =
        GrpcErrorClassifier.wrap(id.raw) {
            stub.getQuote(
                GetQuoteRequest.newBuilder().setInstrumentId(id.raw).build(),
            ).toDomain()
        }

    override suspend fun getCandles(req: CandlesRequest): List<Candle> =
        GrpcErrorClassifier.wrap(req.instrumentId.raw) {
            val protoReq = GetCandlesRequest.newBuilder()
                .setInstrumentId(req.instrumentId.raw)
                .setInterval(req.interval.code)
                .setFromNs(req.from?.let { it.epochSeconds * 1_000_000_000L + it.nanosecondsOfSecond } ?: 0L)
                .setToNs(req.to?.let { it.epochSeconds * 1_000_000_000L + it.nanosecondsOfSecond } ?: 0L)
                .setLimit(req.limit)
                .setIncludeOpen(req.includeOpen)
                .build()
            stub.getCandles(protoReq).candlesList.map { it.toDomain() }
        }

    override suspend fun getOrderBook(id: InstrumentId): OrderBook =
        GrpcErrorClassifier.wrap(id.raw) {
            stub.getOrderBook(
                GetOrderBookRequest.newBuilder().setInstrumentId(id.raw).build(),
            ).toDomain()
        }

    override fun streamTicks(ids: Set<InstrumentId>): Flow<Tick> {
        val req = StreamTicksRequest.newBuilder()
            .addAllInstrumentIds(ids.map { it.raw })
            .build()
        return stub.streamTicks(req).map { it.toDomain() }
    }

    override fun streamQuotesRange(req: QuotesRangeRequest): Flow<Tick> {
        val protoReq = GetQuotesRangeRequest.newBuilder()
            .addAllInstrumentIds(req.instrumentIds.map { it.raw })
            .setFromNs(req.from?.let { it.epochSeconds * 1_000_000_000L + it.nanosecondsOfSecond } ?: 0L)
            .setToNs(req.to?.let { it.epochSeconds * 1_000_000_000L + it.nanosecondsOfSecond } ?: 0L)
            .build()
        return stub.getQuotesRange(protoReq).map { it.toDomain() }
    }
}
