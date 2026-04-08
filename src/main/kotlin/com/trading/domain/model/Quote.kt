package com.trading.domain.model

import java.time.Instant

data class Quote(
    val id: Long = 0,
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val timestamp: Instant = Instant.now()
)
