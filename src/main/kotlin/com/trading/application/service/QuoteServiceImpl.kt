package com.trading.application.service

import com.trading.domain.model.Quote
import com.trading.domain.port.input.QuoteService
import com.trading.domain.port.output.PriceServicePort
import com.trading.domain.port.output.QuoteRepositoryPort
import io.micrometer.tracing.annotation.NewSpan
import io.micrometer.tracing.annotation.SpanTag
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class QuoteServiceImpl(
    private val priceServicePort: PriceServicePort,
    private val quoteRepositoryPort: QuoteRepositoryPort
) : QuoteService {

    @Cacheable("quotes", key = "#symbol")
    @NewSpan("quote.get")
    override fun getQuote(@SpanTag("symbol") symbol: String): Quote {
        val quote = priceServicePort.fetchPrice(symbol)
        return quoteRepositoryPort.save(quote)
    }

    @NewSpan("quote.getAll")
    override fun getAllQuotes(): List<Quote> =
        priceServicePort.fetchAllPrices()

    @NewSpan("quote.history")
    override fun getHistory(symbol: String, from: Instant, to: Instant): List<Quote> =
        quoteRepositoryPort.findBySymbolBetween(symbol, from, to)
}
