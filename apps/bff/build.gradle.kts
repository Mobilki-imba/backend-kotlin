plugins {
    id("conventions.kotlin-serialization")
    application
}

application {
    mainClass.set("io.trading.bff.ApplicationKt")
}

dependencies {
    // Domain & app layers
    implementation(project(":modules:domain"))
    implementation(project(":modules:application"))
    implementation(project(":modules:auth"))
    implementation(project(":modules:infra-persistence"))
    implementation(project(":modules:infra-market-data"))
    implementation(project(":modules:infra-redis"))
    implementation(project(":modules:infra-messaging"))
    implementation(project(":modules:api-rest"))
    implementation(project(":modules:api-ws"))
    implementation(project(":modules:workers"))

    // Komapper + Flyway + Lettuce (типы JdbcDatabase, FlywayMigrator, StatefulRedisConnection используются в Application/Koin/Health)
    implementation(libs.komapper.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.lettuce.core)

    // Ktor server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Coroutines, serialization
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // DI
    implementation(libs.koin.core)
    implementation(libs.koin.logger.slf4j)

    // Config
    implementation(libs.hoplite.core)
    implementation(libs.hoplite.hocon)

    // Metrics
    implementation(libs.micrometer.core)
    implementation(libs.micrometer.registry.prometheus)

    // Logging (runtime)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.logstash.logback.encoder)

    // Tests
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.koin.test)
}
