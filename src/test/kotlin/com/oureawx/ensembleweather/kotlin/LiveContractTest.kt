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
    private val ourea = EnsembleWeather()

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
                forecast.forecast.timeseries
                    .orEmpty()
                    .isNotEmpty(),
            )
        }

    @Test
    fun `search, catalog and attribution answer`() =
        runBlocking {
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
}
