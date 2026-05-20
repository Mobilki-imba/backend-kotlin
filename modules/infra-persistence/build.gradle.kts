plugins {
    id("conventions.kotlin-serialization")
    alias(libs.plugins.ksp)
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:application"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.komapper.core)
    implementation(libs.komapper.annotation)
    implementation(libs.komapper.jdbc)
    implementation(libs.komapper.tx.jdbc)
    implementation(libs.komapper.dialect.postgresql.jdbc)
    ksp(libs.komapper.processor)

    implementation(libs.hikari.cp)
    implementation(libs.postgresql.jdbc)

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.slf4j.api)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)
}

kotlin {
    sourceSets["main"].kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/main/kotlin"))
}
