package com.trading.infrastructure.db.entity

import com.trading.domain.model.Portfolio
import jakarta.persistence.*

@Entity
@Table(name = "portfolios")
class PortfolioEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    @OneToMany(mappedBy = "portfolio", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val positions: List<PositionEntity> = emptyList()
) {
    fun toDomain() = Portfolio(id, userId, name, positions.map { it.toDomain() })
}
