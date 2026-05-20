package io.trading.domain.order

enum class Side {
    BUY,
    SELL,
    ;

    val opposite: Side
        get() = if (this == BUY) SELL else BUY
}
