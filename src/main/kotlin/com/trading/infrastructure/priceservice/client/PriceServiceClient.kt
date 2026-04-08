package com.trading.infrastructure.priceservice.client

import com.trading.infrastructure.priceservice.dto.PriceQuoteDto
import retrofit2.http.GET
import retrofit2.http.Path

interface PriceServiceClient {

    @GET("price/{symbol}")
    suspend fun getPrice(@Path("symbol") symbol: String): PriceQuoteDto

    @GET("prices")
    suspend fun getAllPrices(): List<PriceQuoteDto>
}
