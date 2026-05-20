package io.trading.application.ports

/**
 * Запуск блока в одной БД-транзакции.
 *
 * ВАЖНО: внутри [block] нельзя использовать [kotlinx.coroutines.launch] или
 * [kotlinx.coroutines.async] — дочерние корутины не унаследуют CoroutineContext с
 * подключением и могут провести запись вне транзакции. Только последовательные
 * suspend-вызовы.
 */
interface TransactionManager {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}
