package io.trading.infra.persistence.mappers

import io.trading.domain.money.Money
import io.trading.domain.user.Email
import io.trading.domain.user.User
import io.trading.domain.user.UserId
import io.trading.infra.persistence.rows.UserRow

fun UserRow.toDomain(): User = User(
    id = UserId(id),
    email = Email(email),
    passwordHash = passwordHash,
    displayName = displayName,
    cashBalance = Money.ofMicroUnits(cashBalanceMicro),
    createdAt = createdAt.toKt(),
)

fun User.toRow(): UserRow = UserRow(
    id = id.raw,
    email = email.value,
    passwordHash = passwordHash,
    displayName = displayName,
    cashBalanceMicro = cashBalance.microUnits,
    createdAt = createdAt.toJava(),
)
