package com.trading.infrastructure.db.repository

import com.trading.infrastructure.db.entity.QuoteEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface QuoteJpaRepository : JpaRepository<QuoteEntity, Long> {
    fun findTopBySymbolOrderByTimestampDesc(symbol: String): QuoteEntity?
    fun findBySymbolAndTimestampBetween(symbol: String, from: Instant, to: Instant): List<QuoteEntity>
}
