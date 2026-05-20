package io.trading.infra.persistence.repo

import io.trading.application.ports.RefreshToken
import io.trading.application.ports.RefreshTokenRepository
import io.trading.domain.user.UserId
import io.trading.infra.persistence.mappers.toJava
import io.trading.infra.persistence.mappers.toKt
import io.trading.infra.persistence.rows.RefreshTokenRow
import io.trading.infra.persistence.rows._RefreshTokenRow
import kotlinx.datetime.Instant
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase

class RefreshTokenRepositoryImpl(private val db: JdbcDatabase) : RefreshTokenRepository {
    private val t = _RefreshTokenRow.refreshTokenRow

    override suspend fun insert(token: RefreshToken) {
        val row = RefreshTokenRow(
            tokenHash = token.tokenHash,
            userId = token.userId.raw,
            expiresAt = token.expiresAt.toJava(),
            revokedAt = token.revokedAt?.toJava(),
            createdAt = token.createdAt.toJava(),
        )
        val q = QueryDsl.insert(t).single(row)
        db.runQuery(q)
    }

    override suspend fun findByHash(hash: ByteArray): RefreshToken? {
        val q = QueryDsl.from(t).where { t.tokenHash eq hash }
        val rows: List<RefreshTokenRow> = db.runQuery(q)
        val row = rows.firstOrNull() ?: return null
        return RefreshToken(
            tokenHash = row.tokenHash,
            userId = UserId(row.userId),
            expiresAt = row.expiresAt.toKt(),
            revokedAt = row.revokedAt?.toKt(),
            createdAt = row.createdAt.toKt(),
        )
    }

    override suspend fun revoke(hash: ByteArray, revokedAt: Instant) {
        val q = QueryDsl.update(t)
            .set { t.revokedAt eq revokedAt.toJava() }
            .where { t.tokenHash eq hash }
        db.runQuery(q)
    }

    override suspend fun revokeAllForUser(userId: UserId, revokedAt: Instant) {
        val q = QueryDsl.update(t)
            .set { t.revokedAt eq revokedAt.toJava() }
            .where {
                t.userId eq userId.raw
                t.revokedAt.isNull()
            }
        db.runQuery(q)
    }
}
