// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * A short contract run against the real API, with the read-only CI key from
 * OUREA_API_KEY. Tagged `live`: only `./gradlew liveTest` (the nightly live.yml) runs
 * it, never `test`, PRs or forks.
 */
@Tag("live")
@EnabledIfEnvironmentVariable(named = "OUREA_LIVE", matches = "1")
class LiveContractTest {
    private val ourea = Client()

    @Test
    fun `a named forecast answers its probability terms in order`() =
        runBlocking {
            val forecast =
                ourea.getForecast("or/mt-hood-meadows") {
                    chance(Variable.Snowfall atLeast 6.inches within 24.hours, Variable.Temperature atMost 0.celsius)
                    total(Variable.Snowfall totalWithin 72.hours)
                }
            assertEquals("mt-hood-meadows", forecast.location.slug)
            assertEquals(listOf(Variable.Snowfall, Variable.Temperature), forecast.probabilities?.map { it.variable })
            assertEquals(1, forecast.windowTotals?.size)
            assertTrue(
                forecast.series.timeseries
                    .orEmpty()
                    .isNotEmpty(),
            )
        }

    @Test
    fun `search, catalog and attribution answer`() =
        // Unit: a @Test that returns a value is silently skipped by JUnit.
        runBlocking<Unit> {
            assertTrue(ourea.searchLocations("hood meadows").items.isNotEmpty())
            assertEquals(
                3,
                ourea
                    .catalogLocations { limit = 2 }
                    .take(3)
                    .toList()
                    .size,
            )
            assertTrue(ourea.getAttribution().providers.isNotEmpty())
            assertNotNull(ourea.getCurrent("or/mt-hood-meadows").location)
        }

    @Test
    fun `the 0_1_0 routes answer`() =
        runBlocking<Unit> {
            val plume = ourea.getForecast("or/mt-hood-meadows") { members = Members.Plume }
            assertNotNull(plume.series.plume)
            val ranked = ourea.explore(ExploreMetric.Snow5d) { limit = 2 }
            assertTrue(ranked.items.size <= 2)
            val walked =
                ourea
                    .exploreItems(ExploreMetric.BaseDepth) { limit = 2 }
                    .take(3)
                    .toList()
            assertEquals(3, walked.size)
            val sorted = ourea.searchLocations("hood") { sort = ExploreMetric.Snow5d }
            assertTrue(sorted.items.isNotEmpty())
            assertTrue(ourea.getComposition().models.isNotEmpty())
            assertEquals("or", ourea.getSeason("or/mt-hood-meadows").location.state)
            assertTrue(ourea.getSnowpack("or/mt-hood-meadows").beta)
            assertTrue(ourea.getSnowpackPlume("or/mt-hood-meadows").beta)
            assertEquals("FeatureCollection", ourea.getAlertOutlines().type)
            assertTrue(ourea.getAvalancheZones().features.isNotEmpty())
            assertNotNull(ourea.getCurrent("wa/snoqualmie-pass-i-90").pass)
            // Plus or Pro: a keyed Plus/Pro caller gets the grids, anyone else PLUS_REQUIRED.
            try {
                assertTrue(ourea.getSnowTrend("or/mt-hood-meadows").grids.isNotEmpty())
            } catch (e: PermissionException) {
                assertEquals("PLUS_REQUIRED", e.code)
            }
            val anonymous = ourea.config { apiKey = null }
            val refused = runCatching { anonymous.getSnowTrend("or/mt-hood-meadows") }.exceptionOrNull()
            assertEquals("PLUS_REQUIRED", (refused as? PermissionException)?.code)
        }
}
