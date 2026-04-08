package com.trading.infrastructure.db.adapter

import com.trading.domain.model.Quote
import com.trading.domain.port.out.QuoteRepositoryPort
import com.trading.infrastructure.db.entity.QuoteEntity
import com.trading.infrastructure.db.repository.QuoteJpaRepository
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class QuoteRepositoryAdapter(
    private val jpaRepository: QuoteJpaRepository
) : QuoteRepositoryPort {

    override fun save(quote: Quote): Quote =
        jpaRepository.save(QuoteEntity.fromDomain(quote)).toDomain()

    override fun findLatestBySymbol(symbol: String): Quote? =
        jpaRepository.findTopBySymbolOrderByTimestampDesc(symbol)?.toDomain()

    override fun findBySymbolBetween(symbol: String, from: Instant, to: Instant): List<Quote> =
        jpaRepository.findBySymbolAndTimestampBetween(symbol, from, to).map { it.toDomain() }
}
