package io.trading.application.ports

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object SystemNow {
    fun now(): Instant = Clock.System.now()
}
