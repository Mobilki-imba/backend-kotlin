package com.trading.domain.port.output

import com.trading.domain.model.Trade

interface TradeRepositoryPort {
    fun save(trade: Trade): Trade
    fun findByUserId(userId: Long): List<Trade>
}
