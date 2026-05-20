package io.trading.domain.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class EmailTest : FreeSpec({

    "валидные email" {
        listOf(
            "a@b.co",
            "user.name+tag@example.com",
            "first_last@sub.domain.io",
        ).forEach { Email(it).value shouldBe it }
    }

    "невалидные email" {
        listOf(
            "noatsign",
            "@nolocal.com",
            "a@b",
            "spaces in@email.com",
            "",
        ).forEach { invalid ->
            shouldThrow<IllegalArgumentException> { Email(invalid) }
        }
    }

    "масштабирование маскированием" {
        Email("alice@example.com").masked shouldBe "al***@example.com"
        Email("a@b.co").masked shouldBe "a*@b.co"
    }
})
