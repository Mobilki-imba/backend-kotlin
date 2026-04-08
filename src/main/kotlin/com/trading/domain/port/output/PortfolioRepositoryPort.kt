package com.trading.domain.port.output

import com.trading.domain.model.Portfolio

interface PortfolioRepositoryPort {
    fun findByUserId(userId: Long): List<Portfolio>
}
