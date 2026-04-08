package com.trading.infrastructure.web.controller

import com.trading.domain.port.input.QuoteService
import com.trading.infrastructure.web.dto.QuoteResponse
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/v1/quotes")
class QuoteController(private val quoteService: QuoteService) {

    @GetMapping("/{symbol}")
    fun getQuote(@PathVariable symbol: String): QuoteResponse =
        QuoteResponse.fromDomain(quoteService.getQuote(symbol))

    @GetMapping
    fun getAllQuotes(): List<QuoteResponse> =
        quoteService.getAllQuotes().map { QuoteResponse.fromDomain(it) }

    @GetMapping("/{symbol}/history")
    fun getHistory(
        @PathVariable symbol: String,
        @RequestParam from: Instant,
        @RequestParam to: Instant
    ): List<QuoteResponse> =
        quoteService.getHistory(symbol, from, to).map { QuoteResponse.fromDomain(it) }
}
