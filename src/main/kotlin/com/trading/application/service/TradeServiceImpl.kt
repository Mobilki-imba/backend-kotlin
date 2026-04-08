package com.trading.application.service

import com.trading.domain.model.Trade
import com.trading.domain.model.TradeType
import com.trading.domain.port.input.QuoteService
import com.trading.domain.port.input.TradeService
import com.trading.domain.port.output.TradeRepositoryPort
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.tracing.annotation.NewSpan
import io.micrometer.tracing.annotation.SpanTag
import org.springframework.stereotype.Service

@Service
class TradeServiceImpl(
    private val tradeRepositoryPort: TradeRepositoryPort,
    private val quoteService: QuoteService,
    private val meterRegistry: MeterRegistry
) : TradeService {

    private val tradeCounter = meterRegistry.counter("trades.executed")

    @NewSpan("trade.execute")
    override fun executeTrade(
        @SpanTag("userId") userId: Long,
        symbol: String,
        type: TradeType,
        quantity: Double
    ): Trade {
        val quote = quoteService.getQuote(symbol)
        val price = if (type == TradeType.BUY) quote.ask else quote.bid

        return tradeRepositoryPort.save(
            Trade(userId = userId, symbol = symbol, type = type, quantity = quantity, price = price)
        ).also { tradeCounter.increment() }
    }

    @NewSpan("trade.history")
    override fun getUserTrades(@SpanTag("userId") userId: Long): List<Trade> =
        tradeRepositoryPort.findByUserId(userId)
}
