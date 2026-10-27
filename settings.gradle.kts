// SPDX-License-Identifier: BSD-2-Clause
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Provisions the JDK toolchains (11 to build, 11/17/21/25 to test) where none is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ensemble-weather-kotlin"

include(":examples", ":mock-server")

// examples/ holds only the programs; their build file is gradle/examples/build.gradle.kts.
project(":examples").projectDir = file("gradle/examples")
