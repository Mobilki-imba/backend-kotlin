package com.trading.infrastructure.web.controller

import com.trading.domain.port.input.PortfolioService
import com.trading.infrastructure.web.dto.PortfolioResponse
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/portfolio")
class PortfolioController(private val portfolioService: PortfolioService) {

    @GetMapping
    fun getUserPortfolios(@RequestHeader("X-User-Id") userId: Long): List<PortfolioResponse> =
        portfolioService.getUserPortfolios(userId).map { PortfolioResponse.fromDomain(it) }
}
