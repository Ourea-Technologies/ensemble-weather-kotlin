// SPDX-License-Identifier: BSD-2-Clause
// A snow report: the places with the most snow coming in the next five days, the
// season to date at one of them, and its snowpack forecast (beta). Every number is
// the server's; the SDK ranks, sums and models nothing itself.
package examples.snowreport

import com.oureawx.ensembleweather.kotlin.Client
import com.oureawx.ensembleweather.kotlin.ExploreMetric
import com.oureawx.ensembleweather.kotlin.LocationType

suspend fun main() {
    val ourea = Client()

    val ranked =
        ourea.explore(ExploreMetric.Snow5d) {
            type = LocationType.SkiArea
            limit = 5
        }
    println("Most snow in the next 5 days (${ranked.unit}):")
    for (item in ranked.items) {
        val range = if (item.p10 != null && item.p90 != null) " (${item.p10} to ${item.p90})" else ""
        println("  ${item.rank}. ${item.location.name}, ${item.location.state}: ${item.value}$range")
    }

    val season = ourea.getSeason("or/mt-hood-meadows")
    val precipitation = season.precipitation
    if (precipitation == null) {
        println("No observed days yet in water year ${season.waterYear}.")
    } else {
        val normal = precipitation.percentOfNormal?.let { ", $it% of normal" } ?: ""
        println(
            "Water year ${season.waterYear} at ${season.location.name}: " +
                "${precipitation.amount} ${precipitation.unit} through ${season.through}$normal",
        )
    }

    val snowpack = ourea.getSnowpack("or/mt-hood-meadows")
    for (spot in snowpack.spots) {
        val depth = spot.depth.p50.firstOrNull() ?: continue
        println("  snow depth at ${spot.displayName ?: spot.name ?: "the base"}: $depth (${spot.depth.basis}, beta)")
    }
}
