package io.trading.infra.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.komapper.dialect.postgresql.jdbc.PostgreSqlJdbcDialect
import org.komapper.jdbc.JdbcDatabase
import java.time.Duration

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maximumPoolSize: Int = 50,
    val minimumIdle: Int = 10,
    val connectionTimeout: Duration = Duration.ofSeconds(2),
    val idleTimeout: Duration = Duration.ofMinutes(10),
    val maxLifetime: Duration = Duration.ofMinutes(30),
)

object DataSourceFactory {

    fun createDataSource(cfg: DatabaseConfig): HikariDataSource {
        val hikariCfg = HikariConfig().apply {
            jdbcUrl = cfg.url
            username = cfg.user
            password = cfg.password
            maximumPoolSize = cfg.maximumPoolSize
            minimumIdle = cfg.minimumIdle
            connectionTimeout = cfg.connectionTimeout.toMillis()
            idleTimeout = cfg.idleTimeout.toMillis()
            maxLifetime = cfg.maxLifetime.toMillis()
            poolName = "trading-bff"
        }
        return HikariDataSource(hikariCfg)
    }

    fun create(cfg: DatabaseConfig): JdbcDatabase {
        val ds = createDataSource(cfg)
        return JdbcDatabase(dataSource = ds, dialect = PostgreSqlJdbcDialect())
    }
}
