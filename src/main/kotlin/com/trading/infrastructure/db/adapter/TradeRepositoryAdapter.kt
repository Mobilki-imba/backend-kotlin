package com.trading.infrastructure.db.adapter

import com.trading.domain.model.Trade
import com.trading.domain.port.out.TradeRepositoryPort
import com.trading.infrastructure.db.entity.TradeEntity
import com.trading.infrastructure.db.repository.TradeJpaRepository
import org.springframework.stereotype.Component

@Component
class TradeRepositoryAdapter(
    private val jpaRepository: TradeJpaRepository
) : TradeRepositoryPort {

    override fun save(trade: Trade): Trade =
        jpaRepository.save(TradeEntity.fromDomain(trade)).toDomain()

    override fun findByUserId(userId: Long): List<Trade> =
        jpaRepository.findByUserIdOrderByExecutedAtDesc(userId).map { it.toDomain() }
}
