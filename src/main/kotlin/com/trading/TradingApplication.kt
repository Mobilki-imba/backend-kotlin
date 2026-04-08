package com.trading

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching
class TradingApplication

fun main(args: Array<String>) {
    runApplication<TradingApplication>(*args)
}
