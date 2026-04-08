package com.trading.infrastructure.priceservice.dto

data class PriceQuoteDto(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val timestamp: String
)
