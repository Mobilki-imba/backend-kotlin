package io.trading.infra.persistence

import io.trading.application.ports.Clock
import kotlinx.datetime.Clock as KtClock

class SystemClock : Clock {
    override fun now() = KtClock.System.now()
}
