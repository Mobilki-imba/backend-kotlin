package io.trading.application.auth

interface RefreshRevoker {
    suspend fun revoke(rawToken: String)
}

class LogoutUseCase(private val revoker: RefreshRevoker) {
    suspend operator fun invoke(rawToken: String) = revoker.revoke(rawToken)
}
