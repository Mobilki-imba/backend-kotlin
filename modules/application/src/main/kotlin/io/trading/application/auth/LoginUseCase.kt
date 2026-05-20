package io.trading.application.auth

import io.trading.application.errors.InvalidCredentialsException
import io.trading.application.errors.RateLimitedException
import io.trading.application.ports.RateLimitResult
import io.trading.application.ports.RateLimiter
import io.trading.application.ports.UserRepository
import io.trading.domain.user.Email

interface PasswordVerifying {
    suspend fun verify(password: String, hash: String): Boolean
}

data class LoginCommand(val email: String, val password: String, val ip: String)

class LoginUseCase(
    private val users: UserRepository,
    private val verifier: PasswordVerifying,
    private val issuer: TokenIssuer,
    private val rateLimiter: RateLimiter,
) {
    suspend operator fun invoke(cmd: LoginCommand): AuthResult {
        val bucketKey = "login:${cmd.ip}:${cmd.email.lowercase()}"
        when (val res = rateLimiter.tryAcquire(bucketKey)) {
            is RateLimitResult.Denied -> throw RateLimitedException(res.retryAfter.inWholeMilliseconds)
            RateLimitResult.Allowed -> {}
        }

        val email = try { Email(cmd.email) } catch (_: IllegalArgumentException) {
            throw InvalidCredentialsException()
        }
        val user = users.findByEmail(email) ?: throw InvalidCredentialsException()
        if (!verifier.verify(cmd.password, user.passwordHash)) throw InvalidCredentialsException()

        val access = issuer.issueAccess(user.id)
        val refresh = issuer.issueRefresh(user.id)
        return AuthResult(user.id, access, refresh)
    }
}
