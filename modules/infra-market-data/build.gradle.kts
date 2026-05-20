import com.google.protobuf.gradle.id

plugins {
    id("conventions.kotlin-common")
    alias(libs.plugins.protobuf)
}

dependencies {
    api(project(":modules:domain"))
    api(project(":modules:application"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.slf4j.api)

    // gRPC + protobuf
    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.java)
    implementation(libs.protobuf.kotlin)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

sourceSets {
    main {
        proto {
            srcDir("$rootDir/proto")
        }
    }
}

// NB: версии дублированы из libs.versions.toml (protobuf, grpc, grpc-kotlin) —
// Gradle DSL accessor `libs.versions.X.get()` недоступен внутри protobuf {} блока.
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.68.1"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") {}
                id("grpckt") {}
            }
            task.builtins {
                id("kotlin") {}
            }
        }
    }
}
