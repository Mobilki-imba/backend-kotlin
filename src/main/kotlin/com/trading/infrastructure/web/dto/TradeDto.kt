package com.trading.infrastructure.web.dto

import com.trading.domain.model.Trade
import com.trading.domain.model.TradeType
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive

data class TradeRequest(
    val symbol: String,
    @field:Pattern(regexp = "BUY|SELL")
    val type: String,
    @field:Positive
    val quantity: Double
) {
    fun toTradeType(): TradeType = TradeType.valueOf(type)
}

data class TradeResponse(
    val id: Long,
    val symbol: String,
    val type: String,
    val quantity: Double,
    val price: Double,
    val executedAt: String
) {
    companion object {
        fun fromDomain(t: Trade) = TradeResponse(t.id, t.symbol, t.type.name, t.quantity, t.price, t.executedAt.toString())
    }
}
