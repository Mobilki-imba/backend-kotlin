package io.trading.application.auth

import io.trading.application.ports.TransactionManager
import io.trading.domain.user.UserId

interface RefreshRotator {
    suspend fun rotate(rawToken: String): Pair<UserId, String>
}

class RefreshUseCase(
    private val rotator: RefreshRotator,
    private val issuer: TokenIssuer,
    private val tx: TransactionManager,
) {
    suspend operator fun invoke(rawToken: String): AuthResult = tx.inTransaction {
        val (userId, newRefresh) = rotator.rotate(rawToken)
        val access = issuer.issueAccess(userId)
        AuthResult(userId, access, newRefresh)
    }
}
