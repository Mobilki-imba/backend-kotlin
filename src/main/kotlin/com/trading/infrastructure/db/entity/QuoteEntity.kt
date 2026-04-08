package com.trading.infrastructure.db.entity

import com.trading.domain.model.Quote
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "quotes")
class QuoteEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val timestamp: Instant = Instant.now()
) {
    fun toDomain() = Quote(id, symbol, bid, ask, timestamp)

    companion object {
        fun fromDomain(q: Quote) = QuoteEntity(q.id, q.symbol, q.bid, q.ask, q.timestamp)
    }
}
