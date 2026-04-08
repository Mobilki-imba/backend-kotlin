package com.trading.infrastructure.web.dto

import com.trading.domain.model.Quote

data class QuoteResponse(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val timestamp: String
) {
    companion object {
        fun fromDomain(q: Quote) = QuoteResponse(q.symbol, q.bid, q.ask, q.timestamp.toString())
    }
}
