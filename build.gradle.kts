// SPDX-License-Identifier: BSD-2-Clause
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    // The plugin classpath is locked too (buildscript-gradle.lockfile).
    dependencyLocking { lockAllConfigurations() }
}

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.animalsniffer)
}

group = "com.oureawx"
version = providers.gradleProperty("sdkVersion").get()

// Every configuration resolves from gradle.lockfile; `./gradlew resolveAndLockAll --write-locks` refreshes it.
dependencyLocking {
    lockAllConfigurations()
}

tasks.register("resolveAndLockAll") {
    description = "Resolves every configuration so --write-locks records it."
    notCompatibleWithConfigurationCache("Resolves configurations at execution time.")
    doFirst { require(gradle.startParameter.isWriteDependencyLocks) { "Run with --write-locks." } }
    doLast { configurations.filter { it.isCanBeResolved }.forEach { it.resolve() } }
    dependsOn(":examples:resolveAndLockAll", ":mock-server:resolveAndLockAll")
}

kotlin {
    explicitApi()
    // JDK 11 compiles and (by default) tests the library, so no newer JDK API can slip in.
    jvmToolchain(11)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        allWarningsAsErrors = true
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    // Tests read the virtual clock of kotlinx-coroutines-test (testScheduler.currentTime).
    sourceSets.named("test") { languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi") }
}

dependencies {
    // The only runtime dependencies (CONTRIBUTING.md "Runtime dependencies"; checkDependencies enforces it).
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.snakeyaml)
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly(libs.junit.platform.launcher)

    spotbugsPlugins(libs.findsecbugs)
    signature(libs.android.signature) { artifact { type = "signature" } }
}

// The test JDK: 11 by default; CI runs the matrix with -PtestJdk=17/21/25.
val testJdk = providers.gradleProperty("testJdk").map(String::toInt).orElse(11)

tasks.test {
    useJUnitPlatform { excludeTags("live") }
    maxHeapSize = "1g"
    javaLauncher = javaToolchains.launcherFor { languageVersion = testJdk.map(JavaLanguageVersion::of) }
    systemProperty(
        "ourea.specDir",
        layout.projectDirectory
            .dir("spec")
            .asFile.absolutePath,
    )
    systemProperty("ourea.repoDir", layout.projectDirectory.asFile.absolutePath)
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// The nightly contract run against the real API (live.yml only; needs OUREA_API_KEY).
tasks.register<Test>("liveTest") {
    description = "Runs the live contract tests against https://api.oureawx.com."
    group = "verification"
    testClassesDirs =
        sourceSets.test
            .get()
            .output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("live") }
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(11) }
    systemProperty(
        "ourea.specDir",
        layout.projectDirectory
            .dir("spec")
            .asFile.absolutePath,
    )
}

ktlint {
    version = libs.versions.ktlint.get()
    verbose = true
    outputToConsole = true
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("config/detekt.yml"))
    parallel = true
}

spotbugs {
    effort = Effort.MAX
    reportLevel = Confidence.LOW
    excludeFilter = file("config/spotbugs-exclude.xml")
    toolVersion = libs.versions.spotbugs.get()
}

tasks.spotbugsMain {
    reports.create("xml") { required = true }
    reports.create("html") { required = true }
}

tasks.named("spotbugsTest") { enabled = false }

// No Android-missing JVM API in the library (minSdk 24). The JDK transport is the
// one deliberate exception: it is internal and only used where java.net.http exists.
animalsniffer {
    sourceSets = listOf(project.sourceSets.main.get())
    ignore("java.net.http.*", "java.time.Duration")
}

tasks.named("animalsnifferTest") { enabled = false }

dokka {
    moduleName = "ensemble-weather-kotlin"
    dokkaSourceSets.named("main") {
        includes.from("docs/module.md")
        sourceLink {
            localDirectory = file("src/main/kotlin")
            remoteUrl("https://github.com/Ourea-Technologies/ensemble-weather-kotlin/blob/main/src/main/kotlin")
            remoteLineSuffix = "#L"
        }
        externalDocumentationLinks.register("kotlinx-coroutines") {
            url("https://kotlinlang.org/api/kotlinx.coroutines/")
        }
        externalDocumentationLinks.register("kotlinx-serialization") {
            url("https://kotlinlang.org/api/kotlinx.serialization/")
        }
    }
}

mavenPublishing {
    // The javadoc jar holds the Dokka HTML reference (javadoc.io renders it).
    configure(
        KotlinJvm(javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"), sourcesJar = SourcesJar.Sources()),
    )
    publishToMavenCentral(automaticRelease = false)
    signAllPublications()
    coordinates("com.oureawx", "ensemble-weather-kotlin", version.toString())
    pom {
        name = "Ourea Ensemble Weather (Kotlin)"
        description = "Official Kotlin SDK for the Ourea probabilistic weather API: ensemble weather " +
            "forecast distributions for the contiguous United States. Keywords: ourea, weather, forecast, " +
            "ensemble, probabilistic."
        inceptionYear = "2026"
        url = "https://oureawx.com"
        licenses {
            license {
                name = "BSD-2-Clause"
                url = "https://opensource.org/license/bsd-2-clause"
                distribution = "repo"
            }
        }
        organization {
            name = "Ourea Technologies"
            url = "https://oureawx.com"
        }
        developers {
            developer {
                id = "ourea"
                name = "Ourea Technologies"
                url = "https://oureawx.com"
            }
        }
        scm {
            url = "https://github.com/Ourea-Technologies/ensemble-weather-kotlin"
            connection = "scm:git:https://github.com/Ourea-Technologies/ensemble-weather-kotlin.git"
            developerConnection = "scm:git:ssh://git@github.com/Ourea-Technologies/ensemble-weather-kotlin.git"
        }
        issueManagement {
            system = "GitHub"
            url = "https://github.com/Ourea-Technologies/ensemble-weather-kotlin/issues"
        }
    }
}

// SDK-STANDARD "Dependencies": every runtime dependency has a justification line in CONTRIBUTING.md.
val checkDependencies =
    tasks.register("checkDependencies") {
        description = "Fails if a runtime dependency has no justification in CONTRIBUTING.md."
        group = "verification"
        val contributing = layout.projectDirectory.file("CONTRIBUTING.md")
        val declared =
            provider {
                listOf("api", "implementation", "runtimeOnly").flatMap { name ->
                    configurations[name].dependencies.map { "${it.group}:${it.name}" }
                }
            }
        inputs.file(contributing)
        inputs.property("declared", declared)
        doLast {
            val text = contributing.asFile.readText()
            val section =
                text
                    .substringAfter(
                        "<!-- runtime-dependencies:start -->",
                    ).substringBefore("<!-- runtime-dependencies:end -->")
            val justified =
                Regex(
                    """^- `([^`]+)`: \S""",
                    RegexOption.MULTILINE,
                ).findAll(section).map { it.groupValues[1] }.toSet()
            val missing = declared.get().filterNot { it in justified }
            val stale = justified.filterNot { it in declared.get() }
            check(missing.isEmpty()) { "Runtime dependencies without a justification in CONTRIBUTING.md: $missing" }
            check(stale.isEmpty()) { "CONTRIBUTING.md justifies dependencies the build does not declare: $stale" }
            println("Runtime dependencies, each justified: ${declared.get()}")
        }
    }

tasks.check { dependsOn(checkDependencies) }

// `assemble` builds everything Maven Central gets: the jar, the sources jar and the Dokka javadoc jar.
tasks.assemble { dependsOn("dokkaJavadocJar", "generatePomFileForMavenPublication") }

tasks.register("runExamples") {
    description = "Runs every example in examples/ against the local mock API."
    group = "verification"
    dependsOn(":mock-server:runExamples")
}
