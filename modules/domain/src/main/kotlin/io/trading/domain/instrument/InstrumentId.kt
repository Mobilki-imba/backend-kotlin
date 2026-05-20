package io.trading.domain.instrument

@JvmInline
value class InstrumentId(val raw: Int) {
    init {
        require(raw >= 0) { "InstrumentId must be non-negative, got $raw" }
    }

    override fun toString(): String = raw.toString()
}
