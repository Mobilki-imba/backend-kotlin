package com.trading.domain.port.input

import com.trading.domain.model.Portfolio

interface PortfolioService {
    fun getUserPortfolios(userId: Long): List<Portfolio>
}
