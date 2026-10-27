// SPDX-License-Identifier: BSD-2-Clause
// Search for a location by name, then page through the catalog. The catalog is a
// cold Flow: it fetches the next page (following next_cursor) only as you collect.
package examples.searchandcatalog

import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import kotlinx.coroutines.flow.take

suspend fun main() {
    val ourea = EnsembleWeather()

    val hits = ourea.searchLocations("hood meadows") { limit = 5 }
    for (hit in hits.items) println("search: ${hit.name} (${hit.key}), ${hit.county} County")

    ourea.catalogLocations { admin1 = "US-OR" }.take(10).collect { location ->
        println("catalog: ${location.slug} in ${location.state}, ${location.type}")
    }
}
