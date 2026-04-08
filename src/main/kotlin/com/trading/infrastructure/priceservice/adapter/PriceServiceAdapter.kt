package com.trading.infrastructure.priceservice.adapter

import com.trading.domain.model.Quote
import com.trading.domain.port.out.PriceServicePort
import com.trading.infrastructure.priceservice.client.PriceServiceClient
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PriceServiceAdapter(
    private val client: PriceServiceClient
) : PriceServicePort {

    override fun fetchPrice(symbol: String): Quote {
        val dto = runBlocking { client.getPrice(symbol) }
        return Quote(symbol = dto.symbol, bid = dto.bid, ask = dto.ask, timestamp = Instant.now())
    }

    override fun fetchAllPrices(): List<Quote> {
        val dtos = runBlocking { client.getAllPrices() }
        return dtos.map { Quote(symbol = it.symbol, bid = it.bid, ask = it.ask, timestamp = Instant.now()) }
    }
}
