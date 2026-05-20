plugins {
    id("conventions.kotlin-common")
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:application"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.lettuce.core)
    implementation(libs.bucket4j.core)
    implementation(libs.bucket4j.redis)
    implementation(libs.slf4j.api)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}
