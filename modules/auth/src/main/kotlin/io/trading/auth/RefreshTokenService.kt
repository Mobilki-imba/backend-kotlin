package io.trading.auth

import io.trading.application.errors.TokenExpiredException
import io.trading.application.errors.TokenInvalidException
import io.trading.application.ports.Clock
import io.trading.application.ports.RefreshToken
import io.trading.application.ports.RefreshTokenRepository
import io.trading.domain.user.UserId
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration

class RefreshTokenService(
    private val repo: RefreshTokenRepository,
    private val clock: Clock,
    private val ttl: Duration,
) {
    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    suspend fun issue(userId: UserId): String {
        val raw = ByteArray(32).also(random::nextBytes)
        val token = encoder.encodeToString(raw)
        val hash = sha256(token)
        val now = clock.now()
        repo.insert(
            RefreshToken(
                tokenHash = hash,
                userId = userId,
                expiresAt = now + ttl,
                revokedAt = null,
                createdAt = now,
            ),
        )
        return token
    }

    /** Поворот refresh: отзываем старый, выпускаем новый. Возвращает (userId, newToken). */
    suspend fun rotate(rawToken: String): Pair<UserId, String> {
        val hash = sha256(rawToken)
        val stored = repo.findByHash(hash) ?: throw TokenInvalidException("not found")
        val now = clock.now()
        when {
            stored.revokedAt != null -> {
                // reuse-detection: блокируем все рефреш-токены пользователя
                repo.revokeAllForUser(stored.userId, now)
                throw TokenInvalidException("reused")
            }
            stored.expiresAt <= now -> throw TokenExpiredException()
        }
        repo.revoke(hash, now)
        val newToken = issue(stored.userId)
        return stored.userId to newToken
    }

    suspend fun revoke(rawToken: String) {
        val hash = sha256(rawToken)
        repo.revoke(hash, clock.now())
    }

    private fun sha256(input: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
}
