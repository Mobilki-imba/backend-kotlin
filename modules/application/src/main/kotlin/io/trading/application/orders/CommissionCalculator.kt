package io.trading.application.orders

import io.trading.domain.money.Money
import io.trading.domain.money.Quantity

/** Commission в basis points: 1 bp = 0.01%. */
class CommissionCalculator(private val commissionBps: Int) {
    fun forFill(price: Money, qty: Quantity): Money {
        val notional = price.microUnits * qty.lots
        val fee = (notional * commissionBps) / 10_000L
        return Money.ofMicroUnits(fee)
    }
}
