// SPDX-License-Identifier: BSD-2-Clause
// Current conditions from the nearest sensors, and the active NWS alerts, for a location.
package examples.currentandalerts

import com.oureawx.ensembleweather.kotlin.Client
import com.oureawx.ensembleweather.kotlin.temperatureLabel

suspend fun main() {
    val ourea = Client()

    val current = ourea.getCurrent("or/mt-hood-meadows")
    val now = current.now
    if (now != null) {
        println(
            "Now at ${current.location.name}: ${now.temperature} ${current.units.temperatureLabel}, ${now.text} (${now.source})",
        )
    }
    for (sensor in current.sensors) {
        println(
            "  ${sensor.name} (${sensor.source}) at ${sensor.observedAt}: ${sensor.temperature ?: "no"} temperature",
        )
    }

    val alerts = ourea.getAlerts("or/mt-hood-meadows")
    if (alerts.items.isEmpty()) println("No active alerts.")
    for (alert in alerts.items) println("ALERT ${alert.event} until ${alert.expires}: ${alert.headline}")
}
