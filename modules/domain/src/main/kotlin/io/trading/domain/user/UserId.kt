package io.trading.domain.user

import java.util.UUID

@JvmInline
value class UserId(val raw: UUID) {
    override fun toString(): String = raw.toString()

    companion object {
        fun random(): UserId = UserId(UUID.randomUUID())
        fun parse(value: String): UserId = UserId(UUID.fromString(value))
    }
}
