// SPDX-License-Identifier: BSD-2-Clause
// Catching errors by type: a point outside the forecast domain (London) is an
// OutsideCoverageException, or a PermissionException without a Pro key. Then
// reading the rate-limit state, rather than hard-coding limits.
package examples.errorhandling

import com.oureawx.ensembleweather.kotlin.ApiException
import com.oureawx.ensembleweather.kotlin.ConnectionException
import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import com.oureawx.ensembleweather.kotlin.OutsideCoverageException
import com.oureawx.ensembleweather.kotlin.PermissionException
import com.oureawx.ensembleweather.kotlin.RateLimitException

suspend fun main() {
    val ourea = EnsembleWeather()
    try {
        ourea.getPointForecast(lat = 51.5, lon = -0.1)
    } catch (e: OutsideCoverageException) {
        println("Outside coverage (${e.code}): ${e.message}")
    } catch (e: PermissionException) {
        println("Needs a Pro key (${e.code}): ${e.message}")
    } catch (e: RateLimitException) {
        println("Rate limited; retry after ${e.retryAfter}")
    } catch (e: ApiException) {
        println("API error ${e.status} ${e.code}: ${e.message} (request ${e.requestId})")
    } catch (e: ConnectionException) {
        println("No response: ${e.message}")
    }

    val forecast = ourea.getForecast("or/mt-hood-meadows")
    val limits = forecast.meta.rateLimit ?: ourea.rateLimit.value
    if (limits == null) {
        println("No rate-limit headers on this response (an edge-cacheable one carries none).")
    } else {
        println(
            "Rate limit: ${limits.remaining} of ${limits.limit} left, whole again in ${limits.resetSeconds} s (${limits.policy})",
        )
    }
}
