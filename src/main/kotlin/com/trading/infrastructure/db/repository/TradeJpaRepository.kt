package com.trading.infrastructure.db.repository

import com.trading.infrastructure.db.entity.TradeEntity
import org.springframework.data.jpa.repository.JpaRepository

interface TradeJpaRepository : JpaRepository<TradeEntity, Long> {
    fun findByUserIdOrderByExecutedAtDesc(userId: Long): List<TradeEntity>
}
