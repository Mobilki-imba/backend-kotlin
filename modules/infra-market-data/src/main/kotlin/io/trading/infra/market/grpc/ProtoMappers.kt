package io.trading.infra.market.grpc

import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.instrument.Ticker
import io.trading.domain.money.Currency
import io.trading.domain.money.Money
import io.trading.domain.quote.Candle
import io.trading.domain.quote.CandleInterval
import io.trading.domain.quote.OrderBook
import io.trading.domain.quote.OrderBookLevel
import io.trading.domain.quote.Quote
import io.trading.domain.quote.Tick
import kotlinx.datetime.Instant
import marketdata.v1.Candle as ProtoCandle
import marketdata.v1.Instrument as ProtoInstrument
import marketdata.v1.OrderBook as ProtoOrderBook
import marketdata.v1.OrderBookLevel as ProtoOrderBookLevel
import marketdata.v1.Quote as ProtoQuote
import marketdata.v1.Tick as ProtoTick

private fun Long.tsNsToInstant(): Instant = Instant.fromEpochSeconds(this / 1_000_000_000L, this % 1_000_000_000L)

fun ProtoInstrument.toDomain(): Instrument = Instrument(
    id = InstrumentId(id),
    ticker = Ticker(ticker),
    name = name,
    currency = runCatching { Currency(currency) }.getOrDefault(Currency("RUB")),
    lotSize = lotSize,
    priceStep = Money.ofMicroUnits(priceStepMicro),
    isActive = isActive,
)

fun ProtoQuote.toDomain(): Quote = Quote(
    instrumentId = InstrumentId(instrumentId),
    ticker = Ticker(ticker),
    price = Money.ofMicroUnits(priceMicro),
    bid = Money.ofMicroUnits(bidMicro),
    ask = Money.ofMicroUnits(askMicro),
    dayOpen = Money.ofMicroUnits(openMicro),
    dayHigh = Money.ofMicroUnits(highMicro),
    dayLow = Money.ofMicroUnits(lowMicro),
    dayVolume = dayVolume,
    changeBps = changeBps,
    timestamp = timestampNs.tsNsToInstant(),
)

fun ProtoTick.toDomain(): Tick = Tick(
    instrumentId = InstrumentId(instrumentId),
    ticker = Ticker(ticker),
    timestamp = timestampNs.tsNsToInstant(),
    price = Money.ofMicroUnits(priceMicro),
    volume = volume.toLong(),
    bid = Money.ofMicroUnits(bidMicro),
    ask = Money.ofMicroUnits(askMicro),
)

fun ProtoCandle.toDomain(): Candle = Candle(
    instrumentId = InstrumentId(instrumentId),
    interval = CandleInterval.parse(interval),
    openTime = openTimeNs.tsNsToInstant(),
    open = Money.ofMicroUnits(openMicro),
    high = Money.ofMicroUnits(highMicro),
    low = Money.ofMicroUnits(lowMicro),
    close = Money.ofMicroUnits(closeMicro),
    volume = volume.toLong(),
    trades = trades,
    isClosed = isClosed,
)

fun ProtoOrderBookLevel.toDomain(): OrderBookLevel = OrderBookLevel(
    price = Money.ofMicroUnits(priceMicro),
    quantity = quantity.toLong(),
)

fun ProtoOrderBook.toDomain(): OrderBook = OrderBook(
    instrumentId = InstrumentId(instrumentId),
    bids = bidsList.map { it.toDomain() },
    asks = asksList.map { it.toDomain() },
    timestamp = timestampNs.tsNsToInstant(),
)
