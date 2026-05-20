package io.trading.domain.order

import java.util.UUID

@JvmInline
value class OrderId(val raw: UUID) {
    override fun toString(): String = raw.toString()

    companion object {
        fun random(): OrderId = OrderId(UUID.randomUUID())
        fun parse(value: String): OrderId = OrderId(UUID.fromString(value))
    }
}
