package io.trading.domain.money

@JvmInline
value class Quantity(val lots: Long) : Comparable<Quantity> {
    init {
        require(lots >= 0) { "Quantity must be non-negative, got $lots" }
    }

    operator fun plus(other: Quantity): Quantity = Quantity(lots + other.lots)
    operator fun minus(other: Quantity): Quantity = Quantity(lots - other.lots)
    override operator fun compareTo(other: Quantity): Int = lots.compareTo(other.lots)

    val isZero: Boolean get() = lots == 0L
    val isPositive: Boolean get() = lots > 0

    fun isMultipleOf(lotSize: Int): Boolean = lots % lotSize.toLong() == 0L

    companion object {
        val ZERO: Quantity = Quantity(0L)
    }
}
