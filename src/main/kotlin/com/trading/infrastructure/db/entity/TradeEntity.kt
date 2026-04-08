package com.trading.infrastructure.db.entity

import com.trading.domain.model.Trade
import com.trading.domain.model.TradeType
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "trades")
class TradeEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val userId: Long,
    val symbol: String,
    @Enumerated(EnumType.STRING)
    val type: TradeType,
    val quantity: Double,
    val price: Double,
    val executedAt: Instant = Instant.now()
) {
    fun toDomain() = Trade(id, userId, symbol, type, quantity, price, executedAt)

    companion object {
        fun fromDomain(t: Trade) = TradeEntity(t.id, t.userId, t.symbol, t.type, t.quantity, t.price, t.executedAt)
    }
}
