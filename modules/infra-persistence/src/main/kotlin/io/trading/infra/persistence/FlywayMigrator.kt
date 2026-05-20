package io.trading.infra.persistence

import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.slf4j.LoggerFactory

/** Запускается при старте до открытия Komapper-пула. */
class FlywayMigrator(
    private val jdbcUrl: String,
    private val user: String,
    private val password: String,
) {
    private val log = LoggerFactory.getLogger(FlywayMigrator::class.java)

    fun migrate(): MigrateResult {
        log.info("Running Flyway migrations against {}", jdbcUrl.replaceAfter('?', "***"))
        val flyway = Flyway.configure()
            .dataSource(jdbcUrl, user, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .lockRetryCount(5)
            .load()
        val result = flyway.migrate()
        log.info(
            "Flyway: applied {} migrations, current schema version={}",
            result.migrationsExecuted, result.targetSchemaVersion,
        )
        return result
    }

    companion object {
        /** На случай если конфиг прокидывает r2dbc:-URL — конвертирует в jdbc:; иначе возвращает как есть. */
        fun toJdbcUrl(url: String): String =
            if (url.startsWith("r2dbc:")) "jdbc:${url.removePrefix("r2dbc:")}" else url
    }
}
