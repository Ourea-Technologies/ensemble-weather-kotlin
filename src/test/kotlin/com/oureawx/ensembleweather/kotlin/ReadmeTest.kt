// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/** Keeps the README honest: its quickstart is examples/Quickstart.kt, byte for byte, and its snippets compile. */
class ReadmeTest {
    private val repo = File(System.getProperty("ourea.repoDir") ?: ".")
    private val readme = File(repo, "README.md").readText()

    @Test
    fun `the README quickstart is examples-Quickstart_kt`() {
        val block = readme.substringAfter("## Quickstart").substringAfter("```kotlin\n").substringBefore("```")
        assertEquals(File(repo, "examples/Quickstart.kt").readText(), block)
    }

    @Test
    fun `the README links only to oureawx_com pages, never the API host or a dev host`() {
        val links = Regex("""https?://[^\s)\]`]+""").findAll(readme).map { it.value }.toList()
        assertTrue(links.none { it.startsWith("https://api.oureawx.com") }, "links the API host as a page")
        val ours =
            links
                .map {
                    java.net
                        .URI(it)
                        .host
                        .orEmpty()
                }.filter {
                    it == "oureawx.com" ||
                        it.endsWith(".oureawx.com")
                }
        assertTrue(
            ours.all {
                it == "oureawx.com" || it == "docs.oureawx.com"
            },
            "links an Ourea host other than the site and docs: $ours",
        )
    }

    @Test
    fun `the version is the same everywhere`() {
        val gradle = File(repo, "gradle.properties").readText()
        assertTrue("sdkVersion=${EnsembleWeather.SDK_VERSION}\n" in gradle)
        assertTrue("ensemble-weather-kotlin:${EnsembleWeather.SDK_VERSION}\"" in readme)
        assertTrue("## [${EnsembleWeather.SDK_VERSION}]" in File(repo, "CHANGELOG.md").readText())
    }

    /** The "Probabilities, not single numbers" snippet, compiled (it runs against a stub). */
    @Test
    fun `the probabilities snippet compiles and runs`() =
        runTest {
            val body = File(VectorsTest.VECTORS, "bodies/forecast-metric.json").readBytes()
            val ourea = EnsembleWeather { transport = HttpTransport { HttpResponse(200, emptyMap(), body) } }
            val f =
                ourea.getForecast("or/mt-hood-meadows") {
                    chance(
                        Variable.Snowfall atLeast 12.inches within 72.hours,
                        Variable.Temperature atMost 32.fahrenheit within (6.hours..18.hours) using Aggregation.All,
                    )
                    total(Variable.Snowfall totalWithin 72.hours)
                }
            for (p in f.probabilities.orEmpty()) {
                println(
                    "${p.variable} ${p.op} ${p.threshold} h${p.windowStartHours}-${p.windowEndHours}: ${p.probability}",
                )
            }
            val storm = f.windowTotals?.first()?.quantiles
            val spread =
                f.forecast.timeseries
                    ?.first()
                    ?.ensemble
                    ?.temperature
                    ?.p10ToP90
            assertEquals(null, storm)
            assertEquals(QuantileRange(-4.0, 0.0), spread)
        }
}
