package io.trading.domain.quote

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import kotlinx.datetime.Instant

enum class CandleInterval(val code: String) {
    M1("1m"),
    M5("5m"),
    M15("15m"),
    H1("1h"),
    D1("1d"),
    ;

    companion object {
        fun parse(code: String): CandleInterval =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown candle interval: '$code'")
    }
}

data class Candle(
    val instrumentId: InstrumentId,
    val interval: CandleInterval,
    val openTime: Instant,
    val open: Money,
    val high: Money,
    val low: Money,
    val close: Money,
    val volume: Long,
    val trades: Int,
    val isClosed: Boolean,
)
