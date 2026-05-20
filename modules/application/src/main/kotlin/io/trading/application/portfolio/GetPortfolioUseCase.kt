package io.trading.application.portfolio

import io.trading.application.market.InstrumentCache
import io.trading.application.ports.MarketDataPort
import io.trading.application.ports.PositionRepository
import io.trading.application.ports.UserRepository
import io.trading.domain.portfolio.Portfolio
import io.trading.domain.portfolio.PositionWithMarket
import io.trading.domain.user.UserId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GetPortfolioUseCase(
    private val users: UserRepository,
    private val positions: PositionRepository,
    private val marketData: MarketDataPort,
    private val instruments: InstrumentCache,
) {
    suspend operator fun invoke(userId: UserId): Portfolio = coroutineScope {
        val user = users.findById(userId) ?: throw IllegalStateException("user missing")
        val pos = positions.listForUser(userId)
        val enriched = pos.map { p ->
            async {
                val quoteDeferred = async {
                    runCatching { marketData.getQuote(p.instrumentId) }.getOrNull()
                }
                val instrument = instruments.get(p.instrumentId)
                val quote = quoteDeferred.await()
                PositionWithMarket(
                    instrumentId = p.instrumentId,
                    instrument = instrument,
                    position = p,
                    currentPrice = quote?.price ?: p.avgPrice,
                )
            }
        }.awaitAll()
        Portfolio(userId = userId, cashBalance = user.cashBalance, positions = enriched)
    }
}
