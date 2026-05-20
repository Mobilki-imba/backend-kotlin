package io.trading.workers

import io.trading.application.ports.Clock
import io.trading.application.ports.OrderEventBus
import io.trading.application.ports.OrderEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class OutboxDispatcher(
    private val events: OrderEventRepository,
    private val bus: OrderEventBus,
    private val clock: Clock,
    private val pollInterval: Duration = 1.seconds,
    private val lag: Duration = 5.seconds,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val log = LoggerFactory.getLogger(OutboxDispatcher::class.java)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                try {
                    val now = clock.now()
                    val pending = events.fetchPendingForDispatch(
                        olderThan = now - lag,
                        limit = 100,
                    )
                    pending.forEach { bus.publish(it.event) }
                    if (pending.isNotEmpty()) {
                        events.markDispatched(pending.map { it.eventId }, clock.now())
                        log.debug("outbox dispatched {} events", pending.size)
                    }
                } catch (e: Exception) {
                    log.warn("outbox dispatch failed: {}", e.message)
                }
                delay(pollInterval)
            }
        }
    }

    fun stop() = job?.cancel()
}
