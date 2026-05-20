package io.trading.workers

import io.trading.application.market.InstrumentCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class InstrumentSyncWorker(
    private val cache: InstrumentCache,
    private val interval: Duration = 5.minutes,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val log = LoggerFactory.getLogger(InstrumentSyncWorker::class.java)
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                try {
                    cache.refresh()
                    cache.listAll()
                    log.debug("instrument cache refreshed")
                } catch (e: Exception) {
                    log.warn("instrument refresh failed: {}", e.message)
                }
                delay(interval)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
