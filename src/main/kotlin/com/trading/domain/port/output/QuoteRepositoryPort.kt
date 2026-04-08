package com.trading.domain.port.output

import com.trading.domain.model.Quote
import java.time.Instant

interface QuoteRepositoryPort {
    fun save(quote: Quote): Quote
    fun findLatestBySymbol(symbol: String): Quote?
    fun findBySymbolBetween(symbol: String, from: Instant, to: Instant): List<Quote>
}
