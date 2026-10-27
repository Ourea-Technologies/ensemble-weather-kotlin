// SPDX-License-Identifier: BSD-2-Clause
// A tiny stand-in for the Ourea API, serving the shared vector bodies, so the examples
// run in CI without a key and without calling the real API. Not published.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    application
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

dependencies {
    implementation(libs.kotlinx.serialization.json)
}

application {
    mainClass = "com.oureawx.ensembleweather.mock.MockServerKt"
}

tasks.named<JavaExec>("run") {
    systemProperty(
        "ourea.bodies",
        rootProject.layout.projectDirectory
            .dir("spec/vectors/bodies")
            .asFile.absolutePath,
    )
}

ktlint {
    version = libs.versions.ktlint.get()
}

// Runs every example against the mock: `./gradlew runExamples`.
val examples = project(":examples")
tasks.register<JavaExec>("runExamples") {
    description = "Runs every example in examples/ against the local mock API."
    group = "verification"
    dependsOn(examples.tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.oureawx.ensembleweather.mock.RunExamplesKt"
    systemProperty(
        "ourea.bodies",
        rootProject.layout.projectDirectory
            .dir("spec/vectors/bodies")
            .asFile.absolutePath,
    )
    systemProperty(
        "ourea.examplesDir",
        rootProject.layout.projectDirectory
            .dir("examples")
            .asFile.absolutePath,
    )
    systemProperty(
        "ourea.examplesClasspath",
        examples.extensions
            .getByType<SourceSetContainer>()["main"]
            .runtimeClasspath.asPath,
    )
    systemProperty(
        "ourea.java",
        javaLauncher
            .get()
            .executablePath.asFile.absolutePath,
    )
}
