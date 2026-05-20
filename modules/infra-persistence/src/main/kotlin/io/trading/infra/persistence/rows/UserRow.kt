package io.trading.infra.persistence.rows

import java.time.Instant

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import java.util.UUID

@KomapperEntity
@KomapperTable("users")
data class UserRow(
    @KomapperId
    val id: UUID,
    val email: String,
    val passwordHash: String,
    val displayName: String?,
    val cashBalanceMicro: Long,
    val createdAt: Instant,
)
