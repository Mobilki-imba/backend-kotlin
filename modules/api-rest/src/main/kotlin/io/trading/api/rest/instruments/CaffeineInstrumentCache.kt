package io.trading.api.rest.instruments

import com.github.benmanes.caffeine.cache.AsyncCacheLoader
import com.github.benmanes.caffeine.cache.Caffeine
import io.trading.application.market.InstrumentCache
import io.trading.application.ports.InstrumentRepository
import io.trading.application.ports.MarketDataPort
import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Duration
import java.util.concurrent.CompletableFuture

class CaffeineInstrumentCache(
    private val marketData: MarketDataPort,
    private val repo: InstrumentRepository,
) : InstrumentCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10))
        .refreshAfterWrite(Duration.ofMinutes(5))
        .maximumSize(1024)
        .buildAsync(AsyncCacheLoader<Unit, List<Instrument>> { _, _ ->
            scope.future { loadAll() }
        })

    private suspend fun loadAll(): List<Instrument> {
        val fromMarket = runCatching { marketData.listInstruments() }.getOrNull()
        return if (!fromMarket.isNullOrEmpty()) {
            repo.upsertAll(fromMarket)
            fromMarket
        } else {
            repo.listAll()
        }
    }

    override suspend fun listAll(): List<Instrument> = cache.get(Unit).await()

    override suspend fun get(id: InstrumentId): Instrument? = listAll().firstOrNull { it.id == id }

    override suspend fun refresh() {
        cache.synchronous().invalidateAll()
    }
}
