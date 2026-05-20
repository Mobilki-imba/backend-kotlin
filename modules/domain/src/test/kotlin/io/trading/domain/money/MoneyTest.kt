package io.trading.domain.money

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MoneyTest : FreeSpec({

    "decimal parsing" - {
        "целое значение" {
            Money.ofDecimal("100").microUnits shouldBe 100_000_000L
        }
        "с дробной частью до 6 знаков" {
            Money.ofDecimal("312.45").microUnits shouldBe 312_450_000L
            Money.ofDecimal("0.000001").microUnits shouldBe 1L
            Money.ofDecimal("0.123456").microUnits shouldBe 123_456L
        }
        "отрицательные значения" {
            Money.ofDecimal("-1.5").microUnits shouldBe -1_500_000L
        }
        "нулевое значение" {
            Money.ofDecimal("0").microUnits shouldBe 0L
            Money.ofDecimal("0.0").microUnits shouldBe 0L
        }
        "невалидные входы" {
            shouldThrow<IllegalArgumentException> { Money.ofDecimal("abc") }
            shouldThrow<IllegalArgumentException> { Money.ofDecimal("1.1234567") }
            shouldThrow<IllegalArgumentException> { Money.ofDecimal("") }
            shouldThrow<IllegalArgumentException> { Money.ofDecimal(".5") }
        }
    }

    "decimal output (round-trip)" {
        val values = listOf("100.000000", "312.450000", "0.000001", "-1.500000", "0.000000")
        values.forEach { Money.ofDecimal(it).toDecimalString() shouldBe it }
    }

    "арифметика" {
        val a = Money.ofDecimal("100")
        val b = Money.ofDecimal("0.5")
        (a + b).toDecimalString() shouldBe "100.500000"
        (a - b).toDecimalString() shouldBe "99.500000"
        (-a).microUnits shouldBe -100_000_000L
        (a * 3L).toDecimalString() shouldBe "300.000000"
    }

    "сравнение" {
        Money.ofDecimal("100") shouldBe Money.ofMicroUnits(100_000_000L)
        (Money.ofDecimal("100") > Money.ofDecimal("99.999999")) shouldBe true
        Money.ofDecimal("0").isZero shouldBe true
        Money.ofDecimal("1").isPositive shouldBe true
        Money.ofDecimal("-1").isNegative shouldBe true
    }
})
