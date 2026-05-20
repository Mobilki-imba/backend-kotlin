package io.trading.domain.money

@JvmInline
value class Currency(val code: String) {
    init {
        require(code.length == 3 && code.all { it.isUpperCase() }) {
            "Currency code must be 3 uppercase letters, got '$code'"
        }
    }

    companion object {
        val RUB: Currency = Currency("RUB")
        val USD: Currency = Currency("USD")
        val EUR: Currency = Currency("EUR")
    }
}
