package io.trading.application.auth

import io.trading.application.errors.EmailAlreadyTakenException
import io.trading.application.errors.InvalidPriceException
import io.trading.application.ports.Clock
import io.trading.application.ports.TransactionManager
import io.trading.application.ports.UserRepository
import io.trading.domain.money.Money
import io.trading.domain.user.Email
import io.trading.domain.user.User
import io.trading.domain.user.UserId

data class RegisterCommand(val email: String, val password: String, val displayName: String?)

data class AuthResult(val userId: UserId, val accessToken: String, val refreshToken: String)

interface PasswordHashing {
    suspend fun hash(password: String): String
}

interface TokenIssuer {
    fun issueAccess(userId: UserId): String
    suspend fun issueRefresh(userId: UserId): String
}

interface PasswordPolicyChecker {
    fun isValid(password: String): Boolean
}

class RegisterUseCase(
    private val users: UserRepository,
    private val hasher: PasswordHashing,
    private val issuer: TokenIssuer,
    private val tx: TransactionManager,
    private val clock: Clock,
    private val passwordPolicy: PasswordPolicyChecker,
    private val startingBalance: Money,
) {
    suspend operator fun invoke(cmd: RegisterCommand): AuthResult {
        val email = Email(cmd.email)
        require(passwordPolicy.isValid(cmd.password)) {
            throw InvalidPriceException("password must be >= 8 chars and contain a letter and a digit")
        }

        if (users.existsByEmail(email)) throw EmailAlreadyTakenException(cmd.email)

        val hash = hasher.hash(cmd.password)
        val userId = UserId.random()

        tx.inTransaction {
            users.insert(
                User(
                    id = userId,
                    email = email,
                    passwordHash = hash,
                    displayName = cmd.displayName?.takeIf { it.isNotBlank() },
                    cashBalance = startingBalance,
                    createdAt = clock.now(),
                ),
            )
        }

        val access = issuer.issueAccess(userId)
        val refresh = issuer.issueRefresh(userId)
        return AuthResult(userId, access, refresh)
    }
}
