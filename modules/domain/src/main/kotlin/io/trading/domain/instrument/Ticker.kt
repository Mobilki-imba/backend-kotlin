package io.trading.domain.instrument

@JvmInline
value class Ticker(val symbol: String) {
    init {
        require(symbol.isNotBlank() && symbol.length <= 16) {
            "Ticker must be 1..16 chars, got '$symbol'"
        }
    }

    override fun toString(): String = symbol
}
