package io.trading.application.ports

import io.trading.domain.instrument.InstrumentId
import io.trading.domain.portfolio.Position
import io.trading.domain.user.UserId

interface PositionRepository {
    suspend fun find(userId: UserId, instrumentId: InstrumentId): Position?
    suspend fun listForUser(userId: UserId): List<Position>
    suspend fun upsert(position: Position)
}
