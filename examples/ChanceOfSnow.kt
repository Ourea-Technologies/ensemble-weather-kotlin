// SPDX-License-Identifier: BSD-2-Clause
// Several probability terms in one call. Each answer comes back in the order asked,
// with its reference class: the variable, the threshold, the window and how many
// ensemble members voted. The server computes every probability; the SDK never does.
package examples.chanceofsnow

import com.oureawx.ensembleweather.kotlin.Aggregation
import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import com.oureawx.ensembleweather.kotlin.Units
import com.oureawx.ensembleweather.kotlin.Variable
import com.oureawx.ensembleweather.kotlin.atLeast
import com.oureawx.ensembleweather.kotlin.atMost
import com.oureawx.ensembleweather.kotlin.fahrenheit
import com.oureawx.ensembleweather.kotlin.inches
import com.oureawx.ensembleweather.kotlin.using
import com.oureawx.ensembleweather.kotlin.within
import kotlin.time.Duration.Companion.hours

suspend fun main() {
    val ourea = EnsembleWeather { units = Units.Imperial }
    val forecast =
        ourea.getForecast("or/mt-hood-meadows") {
            chance(
                Variable.Snowfall atLeast 6.inches within 24.hours,
                Variable.Snowfall atLeast 12.inches within 72.hours,
                Variable.Temperature atMost 32.fahrenheit within (6.hours..18.hours) using Aggregation.All,
            )
        }

    for (p in forecast.probabilities.orEmpty()) {
        val chance = p.probability?.let { "${(it * 100).toInt()}%" } ?: "not available yet"
        val window = "hours ${p.windowStartHours}-${p.windowEndHours} (${p.from} to ${p.to})"
        println("${p.variable} ${p.op} ${p.threshold} [${p.aggregation}] over $window: $chance, ${p.members} members")
    }
}
