package io.trading.infra.persistence.repo

import io.trading.application.ports.UserRepository
import io.trading.domain.money.Money
import io.trading.domain.user.Email
import io.trading.domain.user.User
import io.trading.domain.user.UserId
import io.trading.infra.persistence.mappers.toDomain
import io.trading.infra.persistence.mappers.toRow
import io.trading.infra.persistence.rows.UserRow
import io.trading.infra.persistence.rows._UserRow
import org.komapper.core.dsl.QueryDsl
import org.komapper.jdbc.JdbcDatabase

class UserRepositoryImpl(private val db: JdbcDatabase) : UserRepository {
    private val u = _UserRow.userRow

    override suspend fun findById(id: UserId): User? {
        val q = QueryDsl.from(u).where { u.id eq id.raw }
        val rows: List<UserRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun findByEmail(email: Email): User? {
        val q = QueryDsl.from(u).where { u.email eq email.value }
        val rows: List<UserRow> = db.runQuery(q)
        return rows.firstOrNull()?.toDomain()
    }

    override suspend fun existsByEmail(email: Email): Boolean {
        val q = QueryDsl.from(u).where { u.email eq email.value }
        val rows: List<UserRow> = db.runQuery(q)
        return rows.isNotEmpty()
    }

    override suspend fun insert(user: User) {
        val row = user.toRow()
        val q = QueryDsl.insert(u).single(row)
        db.runQuery(q)
    }

    override suspend fun updateCashBalance(id: UserId, newBalance: Money) {
        val q = QueryDsl.update(u)
            .set { u.cashBalanceMicro eq newBalance.microUnits }
            .where { u.id eq id.raw }
        db.runQuery(q)
    }
}
