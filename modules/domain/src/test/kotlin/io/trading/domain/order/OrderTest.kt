package io.trading.domain.order

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.trading.domain.instrument.Instrument
import io.trading.domain.instrument.InstrumentId
import io.trading.domain.instrument.Ticker
import io.trading.domain.money.Currency
import io.trading.domain.money.Money
import io.trading.domain.money.Quantity
import io.trading.domain.user.UserId
import kotlinx.datetime.Instant

private val sber = Instrument(
    id = InstrumentId(1),
    ticker = Ticker("SBER"),
    name = "Sberbank",
    currency = Currency.RUB,
    lotSize = 10,
    priceStep = Money.ofDecimal("0.01"),
    isActive = true,
)

class OrderTest : FreeSpec({

    val now = Instant.parse("2026-05-19T10:00:00Z")
    val userId = UserId.random()

    "createLimit" - {
        "валидная LIMIT BUY 10@312.45" {
            val o = Order.createLimit(
                id = OrderId.random(),
                userId = userId,
                instrument = sber,
                side = Side.BUY,
                quantity = Quantity(10),
                limitPrice = Money.ofDecimal("312.45"),
                idempotencyKey = "abc",
                createdAt = now,
            )
            o.type shouldBe OrderType.LIMIT
            o.status shouldBe OrderStatus.PENDING
            o.remainingQuantity shouldBe Quantity(10)
        }
        "quantity не кратное lotSize отвергается" {
            shouldThrow<IllegalArgumentException> {
                Order.createLimit(OrderId.random(), userId, sber, Side.BUY, Quantity(7),
                    Money.ofDecimal("312.45"), null, now)
            }
        }
        "price не кратная priceStep отвергается" {
            shouldThrow<IllegalArgumentException> {
                Order.createLimit(OrderId.random(), userId, sber, Side.BUY, Quantity(10),
                    Money.ofDecimal("312.455"), null, now)
            }
        }
        "неактивный инструмент отвергается" {
            shouldThrow<IllegalArgumentException> {
                Order.createLimit(OrderId.random(), userId, sber.copy(isActive = false), Side.BUY,
                    Quantity(10), Money.ofDecimal("312.45"), null, now)
            }
        }
    }

    "createMarket не имеет limitPrice" {
        val o = Order.createMarket(OrderId.random(), userId, sber, Side.SELL, Quantity(10), null, now)
        o.type shouldBe OrderType.MARKET
        o.limitPrice shouldBe null
    }

    "MARKET с limitPrice через прямой конструктор отвергается" {
        shouldThrow<IllegalArgumentException> {
            Order(
                id = OrderId.random(),
                userId = userId,
                instrumentId = sber.id,
                side = Side.BUY,
                type = OrderType.MARKET,
                limitPrice = Money.ofDecimal("100"),
                quantity = Quantity(10),
                filledQuantity = Quantity.ZERO,
                status = OrderStatus.PENDING,
                avgFillPrice = null,
                commission = Money.ZERO,
                idempotencyKey = null,
                createdAt = now,
                closedAt = null,
            )
        }
    }
})
