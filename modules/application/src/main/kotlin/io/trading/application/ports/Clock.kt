package io.trading.application.ports

import kotlinx.datetime.Instant

fun interface Clock {
    fun now(): Instant
}
