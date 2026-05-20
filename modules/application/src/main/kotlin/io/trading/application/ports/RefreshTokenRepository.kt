package io.trading.application.ports

import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

data class RefreshToken(
    val tokenHash: ByteArray,
    val userId: UserId,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
) {
    override fun equals(other: Any?): Boolean =
        other is RefreshToken && tokenHash.contentEquals(other.tokenHash)

    override fun hashCode(): Int = tokenHash.contentHashCode()
}

interface RefreshTokenRepository {
    suspend fun insert(token: RefreshToken)
    suspend fun findByHash(hash: ByteArray): RefreshToken?
    suspend fun revoke(hash: ByteArray, revokedAt: Instant)
    suspend fun revokeAllForUser(userId: UserId, revokedAt: Instant)
}
