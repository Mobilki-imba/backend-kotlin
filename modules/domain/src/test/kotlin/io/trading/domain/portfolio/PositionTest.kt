package io.trading.domain.portfolio

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.order.OrderId
import io.trading.domain.order.Side
import io.trading.domain.trade.Trade
import io.trading.domain.trade.TradeId
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

class PositionTest : FreeSpec({

    val now = Instant.parse("2026-05-19T10:00:00Z")
    val userId = UserId.random()
    val instrumentId = InstrumentId(1)

    fun trade(side: Side, qty: Long, price: String): Trade = Trade(
        id = TradeId.random(),
        orderId = OrderId.random(),
        userId = userId,
        instrumentId = instrumentId,
        side = side,
        price = Money.ofDecimal(price),
        quantity = Quantity(qty),
        commission = Money.ZERO,
        executedAt = now,
    )

    "BUY 10@100 + BUY 10@200 → 20@150 (взвешенный avgPrice)" {
        val empty = Position.empty(userId, instrumentId)
        val after1 = empty.applyFill(trade(Side.BUY, 10, "100"))
        val after2 = after1.applyFill(trade(Side.BUY, 10, "200"))

        after2.quantity shouldBe Quantity(20)
        after2.avgPrice shouldBe Money.ofDecimal("150")
    }

    "BUY 30@100 затем SELL 10 → 20@100 (avgPrice не меняется)" {
        val pos = Position.empty(userId, instrumentId)
            .applyFill(trade(Side.BUY, 30, "100"))
            .applyFill(trade(Side.SELL, 10, "120"))

        pos.quantity shouldBe Quantity(20)
        pos.avgPrice shouldBe Money.ofDecimal("100")
    }

    "SELL > позиции — отвергается" {
        val pos = Position.empty(userId, instrumentId).applyFill(trade(Side.BUY, 5, "100"))
        shouldThrow<IllegalArgumentException> {
            pos.applyFill(trade(Side.SELL, 10, "100"))
        }
    }

    "fill чужого инструмента — отвергается" {
        val pos = Position.empty(userId, instrumentId)
        shouldThrow<IllegalArgumentException> {
            pos.applyFill(trade(Side.BUY, 1, "100").copy(instrumentId = InstrumentId(99)))
        }
    }

    "SELL до нуля → quantity=0, avgPrice=0" {
        val pos = Position.empty(userId, instrumentId)
            .applyFill(trade(Side.BUY, 10, "100"))
            .applyFill(trade(Side.SELL, 10, "150"))

        pos.quantity shouldBe Quantity.ZERO
        pos.avgPrice shouldBe Money.ZERO
    }
})
