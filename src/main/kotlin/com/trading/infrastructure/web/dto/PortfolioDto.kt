package com.trading.infrastructure.web.dto

import com.trading.domain.model.Portfolio
import com.trading.domain.model.Position

data class PositionResponse(
    val symbol: String,
    val quantity: Double,
    val avgPrice: Double
) {
    companion object {
        fun fromDomain(p: Position) = PositionResponse(p.symbol, p.quantity, p.avgPrice)
    }
}

data class PortfolioResponse(
    val id: Long,
    val name: String,
    val positions: List<PositionResponse>
) {
    companion object {
        fun fromDomain(p: Portfolio) = PortfolioResponse(p.id, p.name, p.positions.map { PositionResponse.fromDomain(it) })
    }
}
