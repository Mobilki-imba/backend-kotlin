package com.trading.infrastructure.priceservice.config

import com.google.gson.GsonBuilder
import com.trading.infrastructure.priceservice.client.PriceServiceClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Configuration
class PriceServiceConfig {

    @Value("\${price-service.url}")
    private lateinit var priceServiceUrl: String

    @Bean
    fun priceServiceClient(): PriceServiceClient {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()

        return Retrofit.Builder()
            .baseUrl(priceServiceUrl)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .build()
            .create(PriceServiceClient::class.java)
    }
}
