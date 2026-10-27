// SPDX-License-Identifier: BSD-2-Clause
// A forecast at coordinates with an elevation: the summit of Mt. Hood's Palmer
// snowfield, which no named location covers. Needs a Pro key in OUREA_API_KEY.
package examples.pointforecast

import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import com.oureawx.ensembleweather.kotlin.lengthLabel
import com.oureawx.ensembleweather.kotlin.temperatureLabel

suspend fun main() {
    if (System.getenv(EnsembleWeather.ENV_API_KEY).isNullOrBlank()) {
        println("Set OUREA_API_KEY to a Pro key to run this example (forecasts at coordinates are Pro).")
        return
    }
    val ourea = EnsembleWeather()
    val forecast = ourea.getPointForecast(lat = 45.3735, lon = -121.6959, elevationM = 2600.0) { maxHours = 48 }

    val point = forecast.point
    if (point != null) {
        println(
            "Forecast at ${point.lat}, ${point.lon} (${point.elevation} ${forecast.units.lengthLabel}, from ${point.elevationSource})",
        )
    }
    for (hour in forecast.forecast.timeseries
        .orEmpty()
        .take(6)) {
        val t = hour.ensemble.temperature
        println("  ${hour.validTime}  median ${t.median} ${forecast.units.temperatureLabel}, ${hour.conditionText}")
    }
}
