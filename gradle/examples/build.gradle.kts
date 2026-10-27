// SPDX-License-Identifier: BSD-2-Clause
// The build of examples/: one file per task, each with its own `main`. This build file
// lives here so examples/ holds only the programs. CI runs them all against the local
// mock API (`./gradlew runExamples`); none calls the real API.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockAll") {
    notCompatibleWithConfigurationCache("Resolves configurations at execution time.")
    doLast { configurations.filter { it.isCanBeResolved }.forEach { it.resolve() } }
}

kotlin {
    jvmToolchain(11)
    compilerOptions { allWarningsAsErrors = true }
}

sourceSets.main {
    kotlin.setSrcDirs(listOf(rootProject.layout.projectDirectory.dir("examples")))
}

dependencies {
    implementation(project(":"))
}

ktlint {
    version = libs.versions.ktlint.get()
}

// The README quickstart as a user runs it (the nightly live.yml runs it against the real API).
tasks.register<JavaExec>("runQuickstart") {
    description = "Runs examples/Quickstart.kt (against OUREA_BASE_URL, else the real API)."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "QuickstartKt"
}
