// SPDX-License-Identifier: BSD-2-Clause
// The same forecast in metric and imperial. The server converts; the SDK only labels
// each value with its unit and never converts a response value itself.
package examples.units

import com.oureawx.ensembleweather.kotlin.Client
import com.oureawx.ensembleweather.kotlin.Units
import com.oureawx.ensembleweather.kotlin.amountLabel
import com.oureawx.ensembleweather.kotlin.speedLabel
import com.oureawx.ensembleweather.kotlin.temperatureLabel

suspend fun main() {
    val ourea = Client()
    for (system in listOf(Units.Metric, Units.Imperial)) {
        val forecast = ourea.getForecast("or/mt-hood-meadows") { units = system }
        val hour =
            forecast.series.timeseries
                .orEmpty()
                .firstOrNull() ?: continue
        val e = hour.ensemble
        println(
            "${forecast.units}: ${e.temperature.median} ${forecast.units.temperatureLabel}, " +
                "wind ${e.windSpeed.median} ${forecast.units.speedLabel}, " +
                "precipitation ${e.precipitation.median} ${forecast.units.amountLabel}",
        )
    }
}
