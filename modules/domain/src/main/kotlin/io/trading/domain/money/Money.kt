package io.trading.domain.money

@JvmInline
value class Money private constructor(val microUnits: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(microUnits + other.microUnits)
    operator fun minus(other: Money): Money = Money(microUnits - other.microUnits)
    operator fun unaryMinus(): Money = Money(-microUnits)
    operator fun times(factor: Long): Money = Money(microUnits * factor)
    operator fun times(qty: Quantity): Money = Money(microUnits * qty.lots)
    override operator fun compareTo(other: Money): Int = microUnits.compareTo(other.microUnits)

    val isPositive: Boolean get() = microUnits > 0
    val isNegative: Boolean get() = microUnits < 0
    val isZero: Boolean get() = microUnits == 0L

    fun toDecimalString(): String {
        val sign = if (microUnits < 0) "-" else ""
        val abs = if (microUnits < 0) -microUnits else microUnits
        val whole = abs / SCALE
        val fraction = abs % SCALE
        return "$sign$whole.${fraction.toString().padStart(6, '0')}"
    }

    override fun toString(): String = toDecimalString()

    companion object {
        const val SCALE: Long = 1_000_000L
        val ZERO: Money = Money(0L)

        private val DECIMAL_REGEX = Regex("""^-?\d+(?:\.\d{1,6})?$""")

        fun ofMicroUnits(microUnits: Long): Money = Money(microUnits)

        fun ofDecimal(value: String): Money {
            require(DECIMAL_REGEX.matches(value)) { "Invalid decimal: '$value'" }
            val negative = value.startsWith("-")
            val unsigned = if (negative) value.substring(1) else value
            val dot = unsigned.indexOf('.')
            val whole: Long
            val fraction: Long
            if (dot == -1) {
                whole = unsigned.toLong()
                fraction = 0L
            } else {
                whole = unsigned.substring(0, dot).toLong()
                fraction = unsigned.substring(dot + 1).padEnd(6, '0').toLong()
            }
            val total = whole * SCALE + fraction
            return Money(if (negative) -total else total)
        }
    }
}
