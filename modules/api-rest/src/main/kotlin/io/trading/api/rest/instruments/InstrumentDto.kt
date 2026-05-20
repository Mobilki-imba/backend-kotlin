package io.trading.api.rest.instruments

import io.trading.domain.instrument.Instrument
import io.trading.domain.quote.Candle
import io.trading.domain.quote.OrderBook
import io.trading.domain.quote.OrderBookLevel
import io.trading.domain.quote.Quote
import kotlinx.serialization.Serializable

@Serializable
data class InstrumentDto(
    val id: Int,
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val priceStep: String,
    val isActive: Boolean,
)

@Serializable
data class InstrumentDetailsDto(
    val instrument: InstrumentDto,
    val quote: QuoteDto,
)

@Serializable
data class QuoteDto(
    val instrumentId: Int,
    val ticker: String,
    val price: String,
    val bid: String,
    val ask: String,
    val dayOpen: String,
    val dayHigh: String,
    val dayLow: String,
    val dayVolume: Long,
    val changePct: String,
    val ts: Long,
)

@Serializable
data class CandleDto(
    val openTime: Long,
    val open: String,
    val high: String,
    val low: String,
    val close: String,
    val volume: Long,
    val trades: Int,
    val isClosed: Boolean,
)

@Serializable
data class OrderBookLevelDto(val price: String, val quantity: Long)

@Serializable
data class OrderBookDto(
    val instrumentId: Int,
    val bids: List<OrderBookLevelDto>,
    val asks: List<OrderBookLevelDto>,
    val ts: Long,
)

fun Instrument.toDto() = InstrumentDto(
    id = id.raw, ticker = ticker.symbol, name = name, currency = currency.code,
    lotSize = lotSize, priceStep = priceStep.toDecimalString(), isActive = isActive,
)

fun Quote.toDto() = QuoteDto(
    instrumentId = instrumentId.raw,
    ticker = ticker.symbol,
    price = price.toDecimalString(),
    bid = bid.toDecimalString(),
    ask = ask.toDecimalString(),
    dayOpen = dayOpen.toDecimalString(),
    dayHigh = dayHigh.toDecimalString(),
    dayLow = dayLow.toDecimalString(),
    dayVolume = dayVolume,
    changePct = formatChangePct(changeBps),
    ts = timestamp.toEpochMilliseconds(),
)

fun Candle.toDto() = CandleDto(
    openTime = openTime.toEpochMilliseconds(),
    open = open.toDecimalString(),
    high = high.toDecimalString(),
    low = low.toDecimalString(),
    close = close.toDecimalString(),
    volume = volume, trades = trades, isClosed = isClosed,
)

fun OrderBookLevel.toDto() = OrderBookLevelDto(price.toDecimalString(), quantity)
fun OrderBook.toDto() = OrderBookDto(
    instrumentId = instrumentId.raw,
    bids = bids.map { it.toDto() },
    asks = asks.map { it.toDto() },
    ts = timestamp.toEpochMilliseconds(),
)

private fun formatChangePct(bps: Int): String {
    val sign = if (bps < 0) "-" else ""
    val abs = if (bps < 0) -bps else bps
    val whole = abs / 100
    val frac = abs % 100
    return "$sign$whole.${frac.toString().padStart(2, '0')}"
}
