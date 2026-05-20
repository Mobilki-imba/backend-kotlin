package io.trading.api.rest.portfolio

import io.trading.domain.portfolio.Portfolio
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioDto(
    val cash: String,
    val totalValue: String,
    val positions: List<PositionDto>,
)

@Serializable
data class PositionDto(
    val instrumentId: Int,
    val ticker: String,
    val name: String,
    val currency: String,
    val lotSize: Int,
    val quantity: Long,
    val avgPrice: String,
    val currentPrice: String,
    val value: String,
    val unrealizedPnl: String,
    val unrealizedPnlPct: String,
)

fun Portfolio.toDto() = PortfolioDto(
    cash = cashBalance.toDecimalString(),
    totalValue = totalValue.toDecimalString(),
    positions = positions.map { p ->
        val bps = p.unrealizedPnlBps
        val sign = if (bps < 0) "-" else ""
        val abs = if (bps < 0) -bps else bps
        val whole = abs / 100
        val frac = abs % 100
        PositionDto(
            instrumentId = p.instrumentId.raw,
            ticker = p.instrument?.ticker?.symbol ?: p.instrumentId.raw.toString(),
            name = p.instrument?.name ?: "",
            currency = p.instrument?.currency?.code ?: "",
            lotSize = p.instrument?.lotSize ?: 1,
            quantity = p.position.quantity.lots,
            avgPrice = p.position.avgPrice.toDecimalString(),
            currentPrice = p.currentPrice.toDecimalString(),
            value = p.marketValue.toDecimalString(),
            unrealizedPnl = p.unrealizedPnl.toDecimalString(),
            unrealizedPnlPct = "$sign$whole.${frac.toString().padStart(2, '0')}",
        )
    },
)
