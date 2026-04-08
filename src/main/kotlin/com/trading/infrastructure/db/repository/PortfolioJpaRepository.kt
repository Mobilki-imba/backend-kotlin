package com.trading.infrastructure.db.repository

import com.trading.infrastructure.db.entity.PortfolioEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PortfolioJpaRepository : JpaRepository<PortfolioEntity, Long> {
    fun findByUserId(userId: Long): List<PortfolioEntity>
}
