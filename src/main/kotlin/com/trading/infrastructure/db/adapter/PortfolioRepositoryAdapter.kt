package com.trading.infrastructure.db.adapter

import com.trading.domain.model.Portfolio
import com.trading.domain.port.out.PortfolioRepositoryPort
import com.trading.infrastructure.db.repository.PortfolioJpaRepository
import org.springframework.stereotype.Component

@Component
class PortfolioRepositoryAdapter(
    private val jpaRepository: PortfolioJpaRepository
) : PortfolioRepositoryPort {

    override fun findByUserId(userId: Long): List<Portfolio> =
        jpaRepository.findByUserId(userId).map { it.toDomain() }
}
