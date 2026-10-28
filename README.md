# ensemble-weather-kotlin

Official Kotlin SDK for the [Ourea](https://oureawx.com) probabilistic weather API.

[![CI](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/actions/workflows/ci.yml?query=branch%3Amain)
[![Security](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/actions/workflows/security.yml/badge.svg?branch=main)](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/actions/workflows/security.yml?query=branch%3Amain)
[![Maven Central](https://img.shields.io/maven-central/v/com.oureawx/ensemble-weather-kotlin)](https://central.sonatype.com/artifact/com.oureawx/ensemble-weather-kotlin)
[![JVM 11+ / Android 24+](https://img.shields.io/badge/JVM-11%2B%20%7C%20Android%20API%2024%2B-informational)](#supported-versions)
[![License: BSD-2-Clause](https://img.shields.io/badge/license-BSD--2--Clause-blue)](LICENSE)
[![Docs](https://img.shields.io/badge/docs-docs.oureawx.com-informational)](https://docs.oureawx.com/kotlin)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/Ourea-Technologies/ensemble-weather-kotlin/badge)](https://scorecard.dev/viewer/?uri=github.com/Ourea-Technologies/ensemble-weather-kotlin)

Ensemble forecasts for the contiguous United States, blended from up to five
weather models, with every hour's full distribution. Coroutines throughout:
every call is a `suspend` function, lists are `Flow`s, and cancelling the caller
cancels the request. The only dependencies are kotlinx-coroutines and
kotlinx-serialization.

## Install

```kotlin
dependencies {
    implementation("com.oureawx:ensemble-weather-kotlin:0.1.0")
}
```

## Quickstart

No signup and no API key. This is [`examples/Quickstart.kt`](examples/Quickstart.kt),
which CI runs on every change:

```kotlin
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
```

On the JVM (11+) that is all. **On Android**, pass the HTTP client you already
have (Android has no `java.net.http`); see [Android](#android).

## Authentication

Pass a key in the builder, or set the `OUREA_API_KEY` environment variable:

```kotlin
val ourea = EnsembleWeather { apiKey = "ow_..." } // or: OUREA_API_KEY=ow_... in the environment
```

|                                                      | Anonymous                  | Free key                   | Pro key                                                               |
| ---------------------------------------------------- | -------------------------- | -------------------------- | --------------------------------------------------------------------- |
| Named-location forecasts, current conditions, alerts | 10-day horizon, latest run | 10-day horizon, latest run | 15-day horizon, past runs, per-model breakdowns, all member variables |
| Forecasts at any coordinates (`getPointForecast`)    | no                         | no                         | yes                                                                   |
| Search and catalog                                   | yes                        | yes                        | yes, larger list budgets                                              |

See [pricing](https://oureawx.com/pricing). Search, catalog details and
attribution are always sent **without** the key, even when you have one, so every
caller shares one edge-cache entry; catalog list pages and forecasts are sent
with it. Set `sendKeyOnOpenRoutes = true` to attribute every request to your key.
The key is never logged, never in an error or `toString`, and never sent to
another host on a redirect. In an app, keep a Pro key on your server.

## Probabilities, not single numbers

Every forecast value is a distribution (`Quantiles`: `mean`, `p10`, `p25`,
`p50`, `p75`, `p90`, `min`, `max`). Ask the server for the probabilities and
totals you need:

```kotlin
// The chance of a threshold over a window, answered with its reference class.
val f =
    ourea.getForecast("or/mt-hood-meadows") {
        chance(
            Variable.Snowfall atLeast 12.inches within 72.hours,
            Variable.Temperature atMost 32.fahrenheit within (6.hours..18.hours) using Aggregation.All,
        )
        total(Variable.Snowfall totalWithin 72.hours)
    }
for (p in f.probabilities.orEmpty()) {
    println("${p.variable} ${p.op} ${p.threshold} h${p.windowStartHours}-${p.windowEndHours}: ${p.probability} (${p.members} members)")
}

// A storm total: the distribution of each member's own total over the window.
val storm = f.windowTotals?.first()?.quantiles // null when the run does not cover the window

// Quantiles: only the ranges the server serves, by name.
val spread = f.forecast.timeseries?.first()?.ensemble?.temperature?.p10ToP90
```

The SDK never sums, averages or interpolates quantiles, and never derives a
probability from them: the server computes every probability and window total
from the full ensemble (summing hourly quantiles gives a wrong storm total).
A threshold in another unit (`6.inches` on a metric request) is converted
exactly before sending; response values are always in the request's units.
Answers are matched to your terms by the `term` the server echoes and come back
in the order you asked; a `null` probability is never 0, and its `nullReason`
says why (`window_not_covered`, `too_few_members`, `variable_unavailable`). Wind
direction is circular: each hour's `ensemble.windDirection` is a `CircularStats`
(mean, spread and the clockwise 50 % and 90 % sectors, whose `to` may be less
than `from`), not quantiles. More at
[docs.oureawx.com/concepts/probabilities](https://docs.oureawx.com/concepts/probabilities).

## More examples

| Example                                                   | Shows                                                                   |
| --------------------------------------------------------- | ----------------------------------------------------------------------- |
| [`Quickstart.kt`](examples/Quickstart.kt)                 | the program above, anonymous                                            |
| [`PointForecast.kt`](examples/PointForecast.kt)           | a forecast at coordinates with elevation (Pro key from `OUREA_API_KEY`) |
| [`ChanceOfSnow.kt`](examples/ChanceOfSnow.kt)             | several `chance` terms in one call, with their windows and member counts |
| [`StormTotal.kt`](examples/StormTotal.kt)                 | `total` window distributions for precipitation and snowfall             |
| [`SearchAndCatalog.kt`](examples/SearchAndCatalog.kt)     | search, then page through the catalog as a `Flow`                       |
| [`CurrentAndAlerts.kt`](examples/CurrentAndAlerts.kt)     | current conditions and active alerts for a location                     |
| [`ErrorHandling.kt`](examples/ErrorHandling.kt)           | catching an out-of-coverage error by its type; reading rate-limit state |
| [`Units.kt`](examples/Units.kt)                           | the same forecast in metric and imperial                                |

Run them all against a local mock of the API with `./gradlew runExamples`, or
serve the mock with `./gradlew :mock-server:run` and point an example at it with
`OUREA_BASE_URL=http://127.0.0.1:8787`.

## Errors, retries and rate limits

Every error is an `EnsembleWeatherException`, a sealed hierarchy you can catch by
type or `when` over. HTTP errors are `ApiException`s carrying `status`, `code`
(verbatim, even when new), `message`, `requestId`, `rateLimit`, `docUrl` (the
code's entry on the docs errors page) and, on a 400 about one parameter, `param`;
a `PRO_REQUIRED` from a coordinate route may name a `nearestLocation` that
answers without a key:

| Type                        | When                                                                 |
| --------------------------- | -------------------------------------------------------------------- |
| `AuthenticationException`   | invalid or missing key (401)                                         |
| `PermissionException`       | your plan does not allow it, e.g. `PRO_REQUIRED` (403)               |
| `NotFoundException`         | no such location or route (404, 410)                                 |
| `InvalidRequestException`   | the server rejected a parameter (400)                                |
| `OutsideCoverageException`  | `OUTSIDE_COVERAGE` (404): the point is outside the domain            |
| `RateLimitException`        | over a budget (429); `retryAfter` says how long to wait              |
| `UnavailableException`      | the server could not answer (5xx)                                    |
| `ConnectionException`       | no response: DNS, TLS, reset, or the 30 s per-attempt timeout        |
| `ResponseFormatException`   | a 2xx body the SDK cannot read, or an answer missing a term you asked |
| `UsageException`            | refused before sending: an invalid term or location                  |

```kotlin
try {
    ourea.getPointForecast(lat = 51.5, lon = -0.1)
} catch (e: OutsideCoverageException) {
    println("${e.code}: ${e.message}")
}
```

Connection errors (except TLS verification failures), 429, 502, 503 and 504 are
retried up to `maxRetries` (default 2) times, waiting exactly `Retry-After` (sent
on 429, 503 and 504) when the server sends one of 60 s or less (more than that is thrown at once), else
full-jitter backoff from 0.5 s up to 8 s. Nothing else is retried. Cancelling the
calling coroutine cancels the request and any wait.

Each response has a `meta` with its `rateLimit` (`limit`, `remaining`,
`resetSeconds`, `policy` from the `RateLimit-*` headers), `requestId` and
`revalidated`; `ourea.rateLimit` is a `StateFlow` of the latest. Read them rather
than hard-coding limits. Repeated calls revalidate a local ETag cache (on by
default; `cache = null` turns it off, or pass your own `ResponseCache`), so an
unchanged forecast costs a `304`.

## Android

The SDK is Android-friendly (minSdk 24): no JVM-only API in its public surface,
and Animal Sniffer checks the library against the Android API 24 signatures.
Android has no `java.net.http`, so pass a transport built on your own OkHttp or
Ktor; the SDK does the rest (canonical URLs, redirects, retries, cache):

```kotlin
// OkHttp 5 (com.squareup.okhttp3:okhttp-coroutines)
class OkHttpTransport(private val client: OkHttpClient) : HttpTransport {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        val okRequest = Request.Builder().url(request.url).apply {
            request.headers.forEach { (name, value) -> header(name, value) }
        }.build()
        return client.newCall(okRequest).executeAsync().use { response ->
            HttpResponse(response.code, response.headers.toMultimap(), HttpTransport.readBounded(response.body.byteStream()))
        }
    }
}

val ourea = EnsembleWeather {
    transport = OkHttpTransport(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build())
}
```

```kotlin
// Ktor (any engine), with followRedirects = false
class KtorTransport(private val client: HttpClient) : HttpTransport {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        val response = client.get(request.url) { request.headers.forEach { (name, value) -> header(name, value) } }
        return HttpResponse(
            response.status.value,
            response.headers.entries().associate { it.key to it.value },
            HttpTransport.readBounded(response.bodyAsChannel().toInputStream()),
        )
    }
}
```

A transport must not follow redirects, must not disable TLS verification, and
must not log the `X-API-Key` header (with OkHttp's `HttpLoggingInterceptor`, call
`redactHeader("X-API-Key")`). The full contract is on `HttpTransport`.

## Documentation

- Guides and concepts: [docs.oureawx.com](https://docs.oureawx.com)
- This SDK's guide: [docs.oureawx.com/kotlin](https://docs.oureawx.com/kotlin)
- API reference (Dokka): [javadoc.io](https://javadoc.io/doc/com.oureawx/ensemble-weather-kotlin), or `./gradlew dokkaGenerate` locally (`build/dokka/html`)
- The OpenAPI spec this SDK targets (API v1, contract 1.0.0): [ensemble-weather-spec](https://github.com/Ourea-Technologies/ensemble-weather-spec/blob/main/openapi/v1/openapi.yaml), pinned in [`spec/`](spec/README.md)

## Supported versions

JVM 11, 17, 21 and 25 (tested in CI), and Android API 24 or newer with your own
`HttpTransport`. Kotlin 2.4 for building against it. This is version 0.x; the API
may change before 1.0.

## Contributing and community

This SDK is open source and we want it to be the best weather SDK in Kotlin. Bug
reports, feature requests, docs fixes and pull requests are all welcome,
including small ones.

- **Report a bug:** open a [bug report](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/issues/new?template=bug_report.yml).
- **Request a feature** or an API capability: open a [feature request](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/issues/new?template=feature_request.yml).
- **Ask a question or share what you built:** [GitHub Discussions](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/discussions).
- **Contribute code or docs:** start with [CONTRIBUTING.md](CONTRIBUTING.md) and the [`good first issue`](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/labels/good%20first%20issue) label.
- **Security issues:** never in public issues; see [SECURITY.md](SECURITY.md).

Licensed under [BSD-2-Clause](LICENSE), Copyright (c) 2026 Ourea Technologies.
