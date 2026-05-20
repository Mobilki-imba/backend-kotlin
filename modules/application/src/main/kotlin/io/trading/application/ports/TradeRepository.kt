package io.trading.application.ports

import io.trading.domain.common.Cursor
import io.trading.domain.common.Page
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.trade.Trade
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

data class TradesQuery(
    val userId: UserId,
    val instrumentId: InstrumentId?,
    val from: Instant?,
    val to: Instant?,
    val cursor: Cursor?,
    val limit: Int,
)

interface TradeRepository {
    suspend fun insert(trade: Trade)
    suspend fun list(query: TradesQuery): Page<Trade>
}
