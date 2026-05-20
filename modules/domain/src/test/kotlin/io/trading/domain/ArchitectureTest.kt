package io.trading.domain

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.FreeSpec

class ArchitectureTest : FreeSpec({

    val forbiddenImportPrefixes = listOf(
        "org.komapper",
        "io.ktor",
        "io.lettuce",
        "io.grpc",
        "org.flywaydb",
        "org.springframework",
        "com.google.protobuf",
        "com.fasterxml.jackson",
        "io.r2dbc",
    )

    "domain не должен зависеть от инфраструктурных библиотек" {
        Konsist.scopeFromModule("modules/domain")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    forbiddenImportPrefixes.any { import.name.startsWith(it) }
                }
            }
    }

    "domain классы не содержат annotation processing (Komapper @KomapperEntity и т.д.)" {
        Konsist.scopeFromModule("modules/domain")
            .classes()
            .assertFalse { clazz ->
                clazz.annotations.any { it.name.startsWith("Komapper") }
            }
    }
})
