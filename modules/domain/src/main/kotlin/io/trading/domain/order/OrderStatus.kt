package io.trading.domain.order

enum class OrderStatus {
    PENDING,
    PARTIAL,
    FILLED,
    CANCELLED,
    REJECTED,
    ;

    val isTerminal: Boolean get() = this == FILLED || this == CANCELLED || this == REJECTED
    val isActive: Boolean get() = this == PENDING || this == PARTIAL
}
