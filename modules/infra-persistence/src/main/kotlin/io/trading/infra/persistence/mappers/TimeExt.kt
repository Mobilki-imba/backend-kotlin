package io.trading.infra.persistence.mappers

import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant

fun Instant.toJava(): java.time.Instant = toJavaInstant()
fun java.time.Instant.toKt(): Instant = toKotlinInstant()
