package com.trading.infrastructure.web.controller

import com.trading.domain.port.input.TradeService
import com.trading.infrastructure.web.dto.TradeRequest
import com.trading.infrastructure.web.dto.TradeResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/trades")
class TradeController(private val tradeService: TradeService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun executeTrade(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: TradeRequest
    ): TradeResponse =
        TradeResponse.fromDomain(
            tradeService.executeTrade(userId, request.symbol, request.toTradeType(), request.quantity)
        )

    @GetMapping
    fun getUserTrades(@RequestHeader("X-User-Id") userId: Long): List<TradeResponse> =
        tradeService.getUserTrades(userId).map { TradeResponse.fromDomain(it) }
}
