package com.trading.domain.model

data class Position(
    val id: Long = 0,
    val portfolioId: Long,
    val symbol: String,
    val quantity: Double,
    val avgPrice: Double
)
