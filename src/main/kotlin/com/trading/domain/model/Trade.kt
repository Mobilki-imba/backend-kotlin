package com.trading.domain.model

import java.time.Instant

enum class TradeType { BUY, SELL }

data class Trade(
    val id: Long = 0,
    val userId: Long,
    val symbol: String,
    val type: TradeType,
    val quantity: Double,
    val price: Double,
    val executedAt: Instant = Instant.now()
)
