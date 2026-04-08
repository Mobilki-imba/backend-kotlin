package com.trading.application.service

import com.trading.domain.model.Portfolio
import com.trading.domain.port.input.PortfolioService
import com.trading.domain.port.input.QuoteService
import com.trading.domain.port.output.PortfolioRepositoryPort
import org.springframework.stereotype.Service

@Service
class PortfolioServiceImpl(
    private val portfolioRepositoryPort: PortfolioRepositoryPort,
    private val quoteService: QuoteService
) : PortfolioService {

    override fun getUserPortfolios(userId: Long): List<Portfolio> =
        portfolioRepositoryPort.findByUserId(userId)
}
