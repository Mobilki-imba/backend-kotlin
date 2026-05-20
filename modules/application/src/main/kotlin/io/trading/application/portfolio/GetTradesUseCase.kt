package io.trading.application.portfolio

import io.trading.application.ports.TradeRepository
import io.trading.application.ports.TradesQuery
import io.trading.domain.common.Cursor
import io.trading.domain.common.Page
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.trade.Trade
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

class GetTradesUseCase(private val trades: TradeRepository) {
    suspend operator fun invoke(
        userId: UserId,
        instrumentId: InstrumentId?,
        from: Instant?,
        to: Instant?,
        cursor: Cursor?,
        limit: Int,
    ): Page<Trade> = trades.list(
        TradesQuery(userId, instrumentId, from, to, cursor, limit.coerceIn(1, 200)),
    )
}
