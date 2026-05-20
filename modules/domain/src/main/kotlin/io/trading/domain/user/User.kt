package io.trading.domain.user

import io.trading.domain.money.Money
import kotlinx.datetime.Instant

data class User(
    val id: UserId,
    val email: Email,
    val passwordHash: String,
    val displayName: String?,
    val cashBalance: Money,
    val createdAt: Instant,
) {
    init {
        require(!cashBalance.isNegative) { "Cash balance cannot be negative" }
    }

    fun withdraw(amount: Money): User {
        require(!amount.isNegative) { "Withdraw amount must be non-negative" }
        require(cashBalance >= amount) { "Insufficient funds" }
        return copy(cashBalance = cashBalance - amount)
    }

    fun deposit(amount: Money): User {
        require(!amount.isNegative) { "Deposit amount must be non-negative" }
        return copy(cashBalance = cashBalance + amount)
    }
}
