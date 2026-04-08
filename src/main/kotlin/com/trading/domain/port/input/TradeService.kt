package com.trading.domain.port.input

import com.trading.domain.model.Trade
import com.trading.domain.model.TradeType

interface TradeService {
    fun executeTrade(userId: Long, symbol: String, type: TradeType, quantity: Double): Trade
    fun getUserTrades(userId: Long): List<Trade>
}
