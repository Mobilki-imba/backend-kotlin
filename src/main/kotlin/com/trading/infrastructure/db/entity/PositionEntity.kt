package com.trading.infrastructure.db.entity

import com.trading.domain.model.Position
import jakarta.persistence.*

@Entity
@Table(name = "positions")
class PositionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    val portfolio: PortfolioEntity,
    val symbol: String,
    val quantity: Double,
    val avgPrice: Double
) {
    fun toDomain() = Position(id, portfolio.id, symbol, quantity, avgPrice)
}
