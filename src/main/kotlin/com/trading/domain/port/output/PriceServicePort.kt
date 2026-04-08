package com.trading.domain.port.output

import com.trading.domain.model.Quote

interface PriceServicePort {
    fun fetchPrice(symbol: String): Quote
    fun fetchAllPrices(): List<Quote>
}
