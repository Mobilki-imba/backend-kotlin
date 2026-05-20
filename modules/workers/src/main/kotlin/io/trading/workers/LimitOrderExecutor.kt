package io.trading.workers

import io.trading.application.orders.LimitOrderMatchingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration

class LimitOrderExecutor(
    private val service: LimitOrderMatchingService,
    private val tickInterval: Duration,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val log = LoggerFactory.getLogger(LimitOrderExecutor::class.java)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                try {
                    service.matchBatch()
                } catch (e: Exception) {
                    log.warn("matching failed: {}", e.message)
                }
                delay(tickInterval)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
