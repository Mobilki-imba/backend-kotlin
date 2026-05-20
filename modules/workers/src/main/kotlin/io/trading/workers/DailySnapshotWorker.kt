package io.trading.workers

import io.trading.application.ports.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Раз в сутки в 00:00 UTC снимает снапшот totalValue по каждому пользователю —
 * для расчёта dayPnl в GET /portfolio. Скан текущих позиций + последняя котировка.
 *
 * Реализация по плану: тикер каждую минуту проверяет, не прошли ли мы 00:00 UTC
 * с момента последнего снапшота. Если да — берём всех активных пользователей и
 * сохраняем `portfolio_snapshots(user_id, snapshot_date, total_value_micro)`.
 *
 * NB: для большого числа пользователей лучше cron в k8s, но в MVP — внутренний worker.
 */
class DailySnapshotWorker(
    private val clock: Clock,
    private val snapshotTaker: SnapshotTaker,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    fun interface SnapshotTaker {
        suspend fun takeFor(date: kotlinx.datetime.LocalDate)
    }

    private val log = LoggerFactory.getLogger(DailySnapshotWorker::class.java)
    private var job: Job? = null

    @Volatile
    private var lastSnapshotDate: kotlinx.datetime.LocalDate? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                try {
                    val nowUtc = clock.now().toLocalDateTime(TimeZone.UTC)
                    val today = nowUtc.date
                    if (lastSnapshotDate != today && nowUtc.hour == 0) {
                        snapshotTaker.takeFor(today)
                        lastSnapshotDate = today
                        log.info("daily snapshot taken for {}", today)
                    }
                } catch (e: Exception) {
                    log.warn("daily snapshot failed: {}", e.message)
                }
                delay(1.minutes)
            }
        }
    }

    fun stop() = job?.cancel()
}
