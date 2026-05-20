package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("refresh_tokens")
data class RefreshTokenRow(
    @KomapperId
    val tokenHash: ByteArray,
    val userId: UUID,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant,
) {
    override fun equals(other: Any?): Boolean =
        other is RefreshTokenRow && tokenHash.contentEquals(other.tokenHash)
    override fun hashCode(): Int = tokenHash.contentHashCode()
}
