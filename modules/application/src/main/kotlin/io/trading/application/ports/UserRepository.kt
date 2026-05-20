package io.trading.application.ports

import io.trading.domain.money.Money
import io.trading.domain.user.Email
import io.trading.domain.user.User
import io.trading.domain.user.UserId

interface UserRepository {
    suspend fun findById(id: UserId): User?
    suspend fun findByEmail(email: Email): User?
    suspend fun existsByEmail(email: Email): Boolean
    suspend fun insert(user: User)
    suspend fun updateCashBalance(id: UserId, newBalance: Money)
}
