package io.trading.infra.persistence

import io.trading.application.ports.TransactionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.komapper.jdbc.JdbcDatabase
import org.komapper.tx.core.TransactionAttribute

/**
 * JDBC-вариант [TransactionManager]: оборачивает блок в Hikari-транзакцию
 * через Komapper. Так как JDBC синхронный, физически работа выполняется
 * на [Dispatchers.IO], чтобы не блокировать Ktor event loop.
 *
 * NB: внутри [block] нельзя делать `launch`/`async` с другим dispatcher —
 * transaction context (Connection) живёт в текущем потоке.
 */
class TransactionManagerImpl(
    private val db: JdbcDatabase,
) : TransactionManager {

    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        withContext(Dispatchers.IO) {
            db.withTransaction(TransactionAttribute.REQUIRED) {
                kotlinx.coroutines.runBlocking { block() }
            }
        }
}
