package io.trading.infra.market.hub

import io.trading.application.ports.MarketDataPort
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.quote.Tick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

/**
 * Один upstream-stream per instrument: при первом подписчике поднимается `MarketDataPort.streamTicks`,
 * fan-out через SharedFlow для всех клиентов. При 0 подписчиков — debounce 500мс и закрываем upstream.
 */
class MarketTickHub(
    private val marketData: MarketDataPort,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val log = LoggerFactory.getLogger(MarketTickHub::class.java)

    private data class Channel(
        val flow: MutableSharedFlow<Tick>,
        val upstreamJob: Job,
        val refCount: AtomicInteger,
    )

    private val channels = ConcurrentHashMap<InstrumentId, Channel>()

    fun subscribe(id: InstrumentId): Flow<Tick> {
        val channel = channels.compute(id) { _, existing ->
            existing ?: openChannel(id)
        }!!
        channel.refCount.incrementAndGet()
        return channel.flow.asSharedFlow()
            .onCompletion { _ -> release(id) }
    }

    private fun release(id: InstrumentId) {
        val ch = channels[id] ?: return
        val rc = ch.refCount.decrementAndGet()
        if (rc <= 0) {
            scope.launch {
                delay(500.milliseconds)
                val current = channels[id]
                if (current != null && current.refCount.get() <= 0) {
                    channels.remove(id, current)
                    current.upstreamJob.cancel()
                    log.debug("Closed upstream for instrument {}", id)
                }
            }
        }
    }

    private fun openChannel(id: InstrumentId): Channel {
        val flow = MutableSharedFlow<Tick>(
            replay = 1, extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        val job = scope.launch {
            try {
                marketData.streamTicks(setOf(id)).collect { tick ->
                    flow.tryEmit(tick)
                }
            } catch (e: Exception) {
                log.warn("upstream stream for {} failed: {}", id, e.message)
            }
        }
        log.debug("Opened upstream for instrument {}", id)
        return Channel(flow, job, AtomicInteger(0))
    }
}
