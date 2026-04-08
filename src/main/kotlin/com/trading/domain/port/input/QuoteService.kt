package com.trading.domain.port.input

import com.trading.domain.model.Quote
import java.time.Instant

interface QuoteService {
    fun getQuote(symbol: String): Quote
    fun getAllQuotes(): List<Quote>
    fun getHistory(symbol: String, from: Instant, to: Instant): List<Quote>
}
