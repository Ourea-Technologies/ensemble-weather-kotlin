// Quickstart: an anonymous forecast for Mt. Hood Meadows, Oregon (no API key).
// Prints the next 24 hours' temperature as a p10-p90 range, and the chance of
// at least 6 in (15 cm) of snow in the next 24 hours.
import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import com.oureawx.ensembleweather.kotlin.Variable
import com.oureawx.ensembleweather.kotlin.atLeast
import com.oureawx.ensembleweather.kotlin.inches
import com.oureawx.ensembleweather.kotlin.temperatureLabel
import com.oureawx.ensembleweather.kotlin.within
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.hours

suspend fun main() {
    val ourea = EnsembleWeather()
    val forecast =
        ourea.getForecast("or/mt-hood-meadows") {
            chance(Variable.Snowfall atLeast 6.inches within 24.hours)
        }

    println("${forecast.location.name}: temperature over the next 24 h (p10 to p90)")
    val hours = forecast.forecast.timeseries.orEmpty()
    for (hour in hours.take(25)) {
        val range = hour.ensemble.temperature.p10ToP90 ?: continue
        println("  ${hour.validTime}  ${range.low} to ${range.high} ${forecast.units.temperatureLabel}")
    }

    val snow = forecast.probabilities?.firstOrNull()
    val chance = snow?.probability
    if (snow != null && chance != null) {
        val percent = (chance * 100).roundToInt()
        println("Chance of 6 in (15 cm) or more of snow in the next 24 h: $percent% (${snow.members} members)")
    } else {
        println("The chance of 6 in of snow is not available for this window yet.")
    }
}
