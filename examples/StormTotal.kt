// SPDX-License-Identifier: BSD-2-Clause
// Storm totals: the distribution of each ensemble member's own total over a window,
// for precipitation and snowfall. Never add up hourly quantiles yourself: the sum of
// hourly p90s is far above the p90 of the total.
package examples.stormtotal

import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import com.oureawx.ensembleweather.kotlin.Variable
import com.oureawx.ensembleweather.kotlin.amountLabel
import com.oureawx.ensembleweather.kotlin.totalWithin
import kotlin.time.Duration.Companion.hours

suspend fun main() {
    val ourea = EnsembleWeather()
    val forecast =
        ourea.getForecast("or/mt-hood-meadows") {
            total(
                Variable.Precipitation totalWithin 72.hours,
                Variable.Snowfall totalWithin 72.hours,
                Variable.Snowfall totalWithin (72.hours..168.hours),
            )
        }

    val unit = forecast.units.amountLabel
    for (total in forecast.windowTotals.orEmpty()) {
        val q = total.quantiles
        val window = "hours ${total.windowStartHours}-${total.windowEndHours}"
        if (q == null) {
            println("${total.variable} over $window: not covered by this run")
        } else {
            println(
                "${total.variable} over $window: most likely ${q.p50} $unit, at least ${q.p10}, could reach ${q.p90} (${total.members} members)",
            )
        }
    }
}
