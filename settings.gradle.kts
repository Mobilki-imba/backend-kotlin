@file:Suppress("UnstableApiUsage")

rootProject.name = "trading-bff"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include(
    ":apps:bff",
    ":modules:domain",
    ":modules:application",
    ":modules:auth",
    ":modules:infra-persistence",
    ":modules:infra-market-data",
    ":modules:infra-redis",
    ":modules:infra-messaging",
    ":modules:api-rest",
    ":modules:api-ws",
    ":modules:workers",
)
