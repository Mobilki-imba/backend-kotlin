plugins {
    id("conventions.kotlin-common")
}

dependencies {
    api(libs.kotlinx.datetime)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.property)
    testImplementation(libs.konsist)
}
