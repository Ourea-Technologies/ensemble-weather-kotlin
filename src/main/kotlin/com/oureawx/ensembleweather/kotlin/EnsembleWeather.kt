// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import com.oureawx.ensembleweather.kotlin.internal.CanonicalTerms
import com.oureawx.ensembleweather.kotlin.internal.Fetched
import com.oureawx.ensembleweather.kotlin.internal.JdkHttpTransport
import com.oureawx.ensembleweather.kotlin.internal.Operation
import com.oureawx.ensembleweather.kotlin.internal.Query
import com.oureawx.ensembleweather.kotlin.internal.Requester
import com.oureawx.ensembleweather.kotlin.internal.Settings
import com.oureawx.ensembleweather.kotlin.internal.Terms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Creates a client for the Ourea probabilistic weather API.
 *
 * ```kotlin
 * val ourea = EnsembleWeather()                       // anonymous, or OUREA_API_KEY
 * val pro = EnsembleWeather { apiKey = "ow_..."; units = Units.Imperial }
 * ```
 *
 * A client is cheap to keep and safe to share between coroutines; make one and reuse
 * it, so its ETag cache and connection pool are shared.
 */
public fun EnsembleWeather(configure: EnsembleWeather.Builder.() -> Unit = {}): EnsembleWeather =
    EnsembleWeather.Builder().apply(configure).build()

/**
 * The Ourea probabilistic weather API client. Every call is a `suspend` function, every
 * list is a cold [Flow] that follows the cursor, and cancelling the calling coroutine
 * cancels the request.
 *
 * Forecast values are distributions ([Quantiles]), not single numbers. Ask the
 * server for probabilities with [ForecastOptions.chance] and for storm totals with
 * [ForecastOptions.total]: the SDK never sums, averages or interpolates quantiles.
 *
 * Errors are [EnsembleWeatherException]s. Connection failures, 429, 502, 503 and 504
 * are retried ([Builder.maxRetries]); nothing else is.
 *
 * Search, catalog details and attribution are sent without the API key even when
 * the client has one, so every caller shares one edge-cache entry; set
 * [Builder.sendKeyOnOpenRoutes] to attribute every request to your key.
 */
public class EnsembleWeather private constructor(
    private val config: Builder,
    private val rateLimitState: MutableStateFlow<RateLimit?>,
) {
    private val units: Units = config.units
    private val transport: HttpTransport = config.resolvedTransport()
    private val requester: Requester =
        Requester(
            Settings(
                baseUrl = config.resolvedBaseUrl(),
                apiKey = config.resolvedApiKey(),
                sendKeyOnOpenRoutes = config.sendKeyOnOpenRoutes,
                timeout = config.timeout,
                maxRetries = config.maxRetries,
                cache = config.cache,
                transport = transport,
                userAgent = config.userAgent(),
                onDeprecation = config.onDeprecation,
                random = config.random,
                clock = config.clock,
            ),
            rateLimitState,
        )

    /**
     * The most recent rate-limit state the server sent (`RateLimit-*` headers), or
     * `null` before any. Each body's [ApiResponse.meta] carries its own.
     */
    public val rateLimit: StateFlow<RateLimit?> = rateLimitState.asStateFlow()

    /**
     * A client with this one's settings changed by [configure], sharing its transport,
     * cache and rate-limit state. For example `ourea.config { apiKey = otherKey }`.
     */
    public fun config(configure: Builder.() -> Unit): EnsembleWeather =
        EnsembleWeather(
            config
                .copy()
                .apply(configure)
                .also {
                    it.transport = it.transport ?: transport
                }.validated(),
            rateLimitState,
        )

    /** The settings, with the API key redacted. */
    override fun toString(): String = "EnsembleWeather($config)"

    // ---- Forecasts -------------------------------------------------------------------

    /**
     * The forecast for a named location, `"<state>/<slug>"` (`"or/mt-hood-meadows"`).
     * Works without an API key (Free tier).
     */
    public suspend fun getForecast(
        location: String,
        options: LocationForecastOptions.() -> Unit = {},
    ): ForecastResponse {
        val (state, name) = splitLocation(location)
        return getForecast(state, name, options)
    }

    /** The forecast for the location [name] (its slug) in [state] (a USPS code). */
    public suspend fun getForecast(
        state: String,
        name: String,
        options: LocationForecastOptions.() -> Unit = {},
    ): ForecastResponse {
        val o = LocationForecastOptions().apply(options)
        val query = Query()
        query.addList("elevations", o.elevations)
        return forecast(Operation("getLocationForecast"), locationPath(state, name), query, o)
    }

    /**
     * The forecast at a point, with an optional elevation in metres (Pro). Coordinates are
     * sent exactly as given; the server quantizes them to 0.01°.
     */
    public suspend fun getPointForecast(
        lat: Double,
        lon: Double,
        elevationM: Double? = null,
        options: ForecastOptions.() -> Unit = {},
    ): ForecastResponse {
        val o = ForecastOptions().apply(options)
        val query = Query()
        query.add("lat", lat)
        query.add("lon", lon)
        query.add("elevation_m", elevationM)
        return forecast(Operation("getPointForecast"), "/api/v1/forecast", query, o)
    }

    private suspend fun forecast(
        operation: Operation,
        path: String,
        query: Query,
        o: ForecastOptions,
    ): ForecastResponse {
        val units = o.units ?: units
        checkUnits(units)
        val prob = Terms.probabilities(o.probabilityTerms, units)
        val totals = Terms.totals(o.totalTerms)
        query.add("units", units.value, DEFAULT_UNITS)
        query.add("max_hours", o.maxHours, DEFAULT_MAX_HOURS)
        query.add("include_past", o.includePast, default = false)
        query.add("models", o.models, default = false)
        query.add("members", o.members, default = false)
        query.add("daily", o.daily, default = true)
        query.add("max_days", o.maxDays)
        query.add("hourly", o.hourly, default = true)
        query.add("run", o.run)
        query.addList("sources", o.sources)
        query.addList("exclude_sources", o.excludeSources)
        query.add("nowcast", o.nowcast, default = true)
        query.addList("diagnostics", o.diagnostics)
        query.addList("feels_like", o.feelsLike, default = listOf("nws"))
        query.addRaw("prob", prob.wire)
        query.addRaw("totals", totals.wire)
        val fetched = requester.get(operation, path, query.build())
        val body = decode(fetched, serializer<ForecastResponse>())
        return body.answering(prob, totals, units).also { it.attach(fetched.meta) }
    }

    private fun ForecastResponse.answering(
        prob: CanonicalTerms,
        totals: CanonicalTerms,
        units: Units,
    ): ForecastResponse {
        if (prob.callerOrder.isEmpty() && totals.callerOrder.isEmpty()) return this
        // The server answers in the order sent; give each caller term its answer (BEHAVIOR.md section 4).
        val answeredUnits = if (this.units.isKnown) this.units else units
        return copy(
            probabilities =
                if (prob.callerOrder.isEmpty()) {
                    probabilities
                } else {
                    Terms.mapProbabilities(
                        prob,
                        probabilities,
                        answeredUnits,
                    )
                },
            windowTotals =
                if (totals.callerOrder.isEmpty()) {
                    windowTotals
                } else {
                    Terms.mapTotals(
                        totals,
                        windowTotals,
                    )
                },
        )
    }

    // ---- Current conditions and alerts -------------------------------------------------

    /** Current conditions at a named location (`"or/mt-hood-meadows"`). */
    public suspend fun getCurrent(
        location: String,
        options: CurrentOptions.() -> Unit = {},
    ): CurrentResponse {
        val (state, name) = splitLocation(location)
        return current(Operation("getCurrentConditions"), locationPath(state, name) + "/current", Query(), options)
    }

    /** Current conditions at a point, with an optional elevation in metres (Pro). */
    public suspend fun getCurrentAt(
        lat: Double,
        lon: Double,
        elevationM: Double? = null,
        options: CurrentOptions.() -> Unit = {},
    ): CurrentResponse {
        val query = Query()
        query.add("lat", lat)
        query.add("lon", lon)
        query.add("elevation_m", elevationM)
        return current(Operation("getCurrentByCoordinates"), "/api/v1/current", query, options)
    }

    private suspend fun current(
        operation: Operation,
        path: String,
        query: Query,
        options: CurrentOptions.() -> Unit,
    ): CurrentResponse {
        val o = CurrentOptions().apply(options)
        val units = o.units ?: units
        checkUnits(units)
        query.add("units", units.value, DEFAULT_UNITS)
        query.addList("sources", o.sources)
        query.addList("exclude_sources", o.excludeSources)
        query.addList("include", o.include)
        return fetch<CurrentResponse>(operation, path, query)
    }

    /** Active NWS alerts at a named location. */
    public suspend fun getAlerts(location: String): AlertsResponse {
        val (state, name) = splitLocation(location)
        return fetch<AlertsResponse>(
            Operation("getLocationAlerts"),
            locationPath(state, name) + "/alerts",
            Query(),
        )
    }

    /** Active NWS alerts at a point (Pro). */
    public suspend fun getAlertsAt(
        lat: Double,
        lon: Double,
    ): AlertsResponse {
        val query = Query()
        query.add("lat", lat)
        query.add("lon", lon)
        return fetch<AlertsResponse>(
            Operation("getAlertsByCoordinates"),
            "/api/v1/alerts",
            query,
        )
    }

    // ---- Locations ---------------------------------------------------------------------

    /** A named location and its nearest public neighbours. */
    public suspend fun getNearby(
        location: String,
        options: NearbyOptions.() -> Unit = {},
    ): NearbyLocationsResponse {
        val (state, name) = splitLocation(location)
        val query = nearbyQuery(options)
        return fetch<NearbyLocationsResponse>(
            Operation("getNearbyLocations"),
            locationPath(state, name) + "/nearby",
            query,
        )
    }

    /** The nearest public locations to a point. */
    public suspend fun getNearbyAt(
        lat: Double,
        lon: Double,
        options: NearbyOptions.() -> Unit = {},
    ): NearbyPointResponse {
        val query = nearbyQuery(options)
        query.add("lat", lat)
        query.add("lon", lon)
        return fetch<NearbyPointResponse>(
            Operation("getNearbyByCoordinates"),
            "/api/v1/nearby",
            query,
        )
    }

    private fun nearbyQuery(options: NearbyOptions.() -> Unit): Query {
        val o = NearbyOptions().apply(options)
        val units = o.units ?: units
        checkUnits(units)
        return Query().apply {
            add("units", units.value, DEFAULT_UNITS)
            add("limit", o.limit, default = DEFAULT_NEARBY_LIMIT)
            add("radius_km", o.radiusKm, default = DEFAULT_RADIUS_KM)
        }
    }

    /** Searches locations by name, best match first. Sent without the API key (an open route). */
    public suspend fun searchLocations(
        query: String,
        options: SearchOptions.() -> Unit = {},
    ): SearchLocationsResponse {
        val o = SearchOptions().apply(options)
        val q = Query()
        q.add("q", query)
        q.add("state", o.state)
        q.add("county", o.county)
        q.add("type", o.type?.value)
        q.add("limit", o.limit, default = DEFAULT_SEARCH_LIMIT)
        return fetch<SearchLocationsResponse>(
            Operation("searchLocations", open = true),
            "/api/v1/search/locations",
            q,
        )
    }

    /**
     * Every location (or every one in [state]), page by page as you collect. Anonymous
     * and Free callers get capped pages; a full walk needs a Pro key.
     */
    public fun locations(
        state: String? = null,
        options: LocationListOptions.() -> Unit = {},
    ): Flow<LocationSummary> =
        paged(
            { cursor ->
                getLocationsPage(state) {
                    options()
                    this.cursor = cursor ?: this.cursor
                }
            },
            { it.locations to it.nextCursor },
        )

    /** One page of the location list (all states when [state] is `null`). */
    public suspend fun getLocationsPage(
        state: String? = null,
        options: LocationListOptions.() -> Unit = {},
    ): LocationsResponse {
        val o = LocationListOptions().apply(options)
        val units = o.units ?: units
        checkUnits(units)
        val query = Query()
        query.add("type", o.type?.value)
        query.add("units", units.value, DEFAULT_UNITS)
        query.add("limit", o.limit)
        query.add("cursor", o.cursor)
        val (operation, path) =
            if (state == null) {
                Operation("getAllLocations") to "/api/v1/locations"
            } else {
                Operation("getLocations") to "/api/v1/locations/${Query.encode(canonicalState(state))}"
            }
        return fetch<LocationsResponse>(operation, path, query)
    }

    // ---- Catalog -----------------------------------------------------------------------

    /** Every catalog location, following the cursor as you collect. Sent with the API key. */
    public fun catalogLocations(options: CatalogLocationOptions.() -> Unit = {}): Flow<PublicCatalogLocation> =
        paged(
            { cursor ->
                getCatalogLocationsPage {
                    options()
                    this.cursor = cursor ?: this.cursor
                }
            },
            { it.items to it.nextCursor },
        )

    /** One page of catalog locations. */
    public suspend fun getCatalogLocationsPage(
        options: CatalogLocationOptions.() -> Unit = {},
    ): PublicCatalogLocationPage {
        val o = CatalogLocationOptions().apply(options)
        val query = Query()
        query.add("admin1", o.admin1)
        query.add("country", o.country)
        query.add("type", o.type)
        query.add("template", o.template)
        query.add("limit", o.limit, default = DEFAULT_CATALOG_LIMIT)
        query.add("cursor", o.cursor)
        return fetch<PublicCatalogLocationPage>(
            Operation("listCatalogLocations"),
            "/api/v1/catalog/locations",
            query,
        )
    }

    /** A catalog location's detail, by slug. Sent without the API key (an open route). */
    public suspend fun getCatalogLocation(
        slug: String,
        options: CatalogDetailOptions.() -> Unit = {},
    ): PublicCatalogLocationDetail {
        val query = detailQuery(options)
        val path = "/api/v1/catalog/locations/${Query.encode(slug)}"
        return fetch<PublicCatalogLocationDetail>(
            Operation("getCatalogLocation", open = true),
            path,
            query,
        )
    }

    /** Every catalog station, following the cursor as you collect. Sent with the API key. */
    public fun catalogStations(options: CatalogStationOptions.() -> Unit = {}): Flow<PublicCatalogStation> =
        paged(
            { cursor ->
                getCatalogStationsPage {
                    options()
                    this.cursor = cursor ?: this.cursor
                }
            },
            { it.items to it.nextCursor },
        )

    /** One page of catalog stations. */
    public suspend fun getCatalogStationsPage(
        options: CatalogStationOptions.() -> Unit = {},
    ): PublicCatalogStationPage {
        val o = CatalogStationOptions().apply(options)
        val query = Query()
        query.add("admin1", o.admin1)
        query.add("country", o.country)
        query.addList("network", o.network)
        query.addRepeated("capability", o.capability)
        query.add("status", o.status)
        query.add("limit", o.limit, default = DEFAULT_CATALOG_LIMIT)
        query.add("cursor", o.cursor)
        return fetch<PublicCatalogStationPage>(
            Operation("listCatalogStations"),
            "/api/v1/catalog/stations",
            query,
        )
    }

    /** A catalog station's detail, by slug. Sent without the API key (an open route). */
    public suspend fun getCatalogStation(
        slug: String,
        options: CatalogDetailOptions.() -> Unit = {},
    ): PublicCatalogStationDetail {
        val query = detailQuery(options)
        val path = "/api/v1/catalog/stations/${Query.encode(slug)}"
        return fetch<PublicCatalogStationDetail>(
            Operation("getCatalogStation", open = true),
            path,
            query,
        )
    }

    private fun detailQuery(options: CatalogDetailOptions.() -> Unit): Query {
        val o = CatalogDetailOptions().apply(options)
        return Query().apply {
            addList("include", o.include)
            add("locale", o.locale, default = "en")
        }
    }

    // ---- Runs, storm reports, attribution ----------------------------------------------

    /** The model runs a `run` option can pin, and what a forecast serves now. */
    public suspend fun getRuns(limit: Int? = null): RunsResponse {
        val query = Query().apply { add("limit", limit, default = DEFAULT_RUNS_LIMIT) }
        return fetch<RunsResponse>(Operation("getAvailableRuns"), "/api/v1/runs", query)
    }

    /** Published storm report cards, most recent onset first. */
    public suspend fun getStormReports(limit: Int? = null): StormReportsResponse {
        val query = Query().apply { add("limit", limit, default = DEFAULT_STORM_LIMIT) }
        return fetch<StormReportsResponse>(
            Operation("getStormReports"),
            "/api/v1/storm-reports",
            query,
        )
    }

    /** One storm report card. */
    public suspend fun getStormReport(id: Long): StormReportCard =
        fetch<StormReportCard>(
            Operation("getStormReport"),
            "/api/v1/storm-reports/$id",
            Query(),
        )

    /** Every data source with its licence and required notices. Sent without the API key. */
    public suspend fun getAttribution(): AttributionRegistry =
        fetch<AttributionRegistry>(
            Operation("getAttribution", open = true),
            "/api/v1/attribution",
            Query(),
        )

    // ---- Plumbing ----------------------------------------------------------------------

    private suspend inline fun <reified T : ApiResponse> fetch(
        operation: Operation,
        path: String,
        query: Query,
    ): T {
        val fetched = requester.get(operation, path, query.build())
        return decode(fetched, serializer<T>()).also { it.attach(fetched.meta) }
    }

    private fun <T> decode(
        fetched: Fetched,
        strategy: DeserializationStrategy<T>,
    ): T =
        try {
            JSON.decodeFromString(strategy, fetched.body.toString(Charsets.UTF_8))
        } catch (e: SerializationException) {
            throw ResponseFormatException("the response is not the documented JSON: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            throw ResponseFormatException("the response is not the documented JSON: ${e.message}", e)
        }

    private fun <P, T> paged(
        page: suspend (cursor: String?) -> P,
        items: (P) -> Pair<List<T>, String?>,
    ): Flow<T> =
        flow {
            var cursor: String? = null
            do {
                val (rows, next) = items(page(cursor))
                rows.forEach { emit(it) }
                cursor = next
            } while (cursor != null)
        }

    private fun checkUnits(units: Units) {
        if (!units.isKnown) throw UsageException("unknown units '${units.value}': use Units.Metric, Imperial or Kelvin")
    }

    /**
     * The client's configuration. Unset settings take their defaults: the API key from
     * `OUREA_API_KEY` (else anonymous), the base URL from `OUREA_BASE_URL` (else
     * `https://api.oureawx.com`), metric units, 30 s per attempt, 2 retries, and the
     * in-memory ETag cache.
     */
    @EnsembleWeatherDsl
    public class Builder internal constructor() {
        private var apiKeySet = false
        private var baseUrlSet = false

        /**
         * The API key, sent as `X-API-Key`. Unset, `OUREA_API_KEY` from the environment is
         * used; `null` (set explicitly) means anonymous. Never logged or put in an error.
         */
        public var apiKey: String? = null
            set(value) {
                field = value
                apiKeySet = true
            }

        /** The API's base URL (`https://api.oureawx.com`; `OUREA_BASE_URL` when unset). A trailing slash is ignored. */
        public var baseUrl: String = DEFAULT_BASE_URL
            set(value) {
                field = value
                baseUrlSet = true
            }

        /** The unit system for every request that takes one (default [Units.Metric]). */
        public var units: Units = Units.Metric

        /** The timeout for each attempt (default 30 s). */
        public var timeout: Duration = DEFAULT_TIMEOUT

        /** Retries after the first attempt (default 2; 0 disables retrying). */
        public var maxRetries: Int = DEFAULT_MAX_RETRIES

        /** The ETag cache, on by default; `null` disables it. */
        public var cache: ResponseCache? = InMemoryResponseCache()

        /** Send the API key on the credential-free routes too (default `false`). */
        public var sendKeyOnOpenRoutes: Boolean = false

        /** The HTTP layer. Unset, `java.net.http` on the JVM; an Android app must set one. */
        public var transport: HttpTransport? = null

        /** A product token appended to the User-Agent (`MyApp/1.2`). Never put user data in it. */
        public var userAgentSuffix: String? = null

        /** Where deprecation notices go (once per operation per process); default `java.util.logging`. */
        public var onDeprecation: (DeprecationNotice) -> Unit = ::logDeprecation

        internal var environment: (String) -> String? = System::getenv
        internal var random: () -> Double = { Random.nextDouble() }
        internal var clock: () -> Long = System::currentTimeMillis

        internal fun copy(): Builder =
            Builder().also {
                it.apiKey = resolvedApiKey()
                it.baseUrl = resolvedBaseUrl()
                it.units = units
                it.timeout = timeout
                it.maxRetries = maxRetries
                it.cache = cache
                it.sendKeyOnOpenRoutes = sendKeyOnOpenRoutes
                it.transport = transport
                it.userAgentSuffix = userAgentSuffix
                it.onDeprecation = onDeprecation
                it.environment = environment
                it.random = random
                it.clock = clock
            }

        internal fun build(): EnsembleWeather = EnsembleWeather(validated(), MutableStateFlow(null))

        internal fun validated(): Builder {
            if (maxRetries < 0) throw UsageException("maxRetries must be 0 or more")
            if (!timeout.isPositive()) throw UsageException("timeout must be positive")
            checkBaseUrl(resolvedBaseUrl())
            return this
        }

        internal fun resolvedApiKey(): String? =
            (if (apiKeySet) apiKey else environment(ENV_API_KEY))?.takeIf {
                it.isNotBlank()
            }

        internal fun resolvedBaseUrl(): String =
            (if (baseUrlSet) baseUrl else environment(ENV_BASE_URL)?.takeIf { it.isNotBlank() } ?: baseUrl).trimEnd('/')

        internal fun resolvedTransport(): HttpTransport =
            transport ?: if (JdkHttpTransport.available) {
                JdkHttpTransport(timeout)
            } else {
                throw UsageException(
                    "no HttpTransport: this runtime has no java.net.http (Android). Set `transport` to one built " +
                        "on your HTTP client; see https://docs.oureawx.com/kotlin",
                )
            }

        internal fun userAgent(): String {
            val base = "ensemble-weather-kotlin/$SDK_VERSION (${runtime()})"
            return userAgentSuffix?.takeIf { it.isNotBlank() }?.let { "$base $it" } ?: base
        }

        /** The settings, with the API key redacted. */
        override fun toString(): String =
            "baseUrl=${resolvedBaseUrl()}, apiKey=${if (resolvedApiKey() == null) "none" else "<redacted>"}, " +
                "units=$units, timeout=$timeout, maxRetries=$maxRetries, cache=${cache != null}"

        private fun checkBaseUrl(url: String) {
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                throw UsageException("baseUrl must be an http(s) URL")
            }
        }
    }

    public companion object {
        /** This SDK's version. */
        public const val SDK_VERSION: String = "0.1.0"

        /** The production API. */
        public const val DEFAULT_BASE_URL: String = "https://api.oureawx.com"

        /** The environment variable the API key is read from. */
        public const val ENV_API_KEY: String = "OUREA_API_KEY"

        /** The environment variable the base URL is read from (a proxy, a local stub). */
        public const val ENV_BASE_URL: String = "OUREA_BASE_URL"

        private val DEFAULT_TIMEOUT = 30.seconds
        private const val DEFAULT_MAX_RETRIES = 2
        private const val DEFAULT_UNITS = "metric"
        private const val DEFAULT_MAX_HOURS = 24
        private const val DEFAULT_NEARBY_LIMIT = 3
        private const val DEFAULT_RADIUS_KM = 50.0
        private const val DEFAULT_SEARCH_LIMIT = 20
        private const val DEFAULT_CATALOG_LIMIT = 20
        private const val DEFAULT_RUNS_LIMIT = 10
        private const val DEFAULT_STORM_LIMIT = 20

        internal val JSON: Json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                isLenient = false
                allowSpecialFloatingPointValues = false
                useArrayPolymorphism = false
            }

        private val logger: Logger = Logger.getLogger("com.oureawx.ensembleweather")

        private fun logDeprecation(notice: DeprecationNotice) {
            logger.log(Level.WARNING, notice.describe())
        }

        private fun runtime(): String {
            val kotlin = "Kotlin/${KotlinVersion.CURRENT}"
            val vm = System.getProperty("java.vm.name").orEmpty()
            return if (vm.contains("Dalvik", ignoreCase = true)) {
                "$kotlin; Android/${System.getProperty("java.vm.version").orEmpty()}"
            } else {
                "$kotlin; Java/${System.getProperty("java.version").orEmpty()}"
            }
        }

        /** Splits `"or/mt-hood-meadows"` on its one `/` (BEHAVIOR.md section 3). */
        internal fun splitLocation(location: String): Pair<String, String> {
            val parts = location.split('/')
            if (parts.size != 2 || parts.any { it.isEmpty() }) {
                throw UsageException("a location is \"<state>/<slug>\", like \"or/mt-hood-meadows\"")
            }
            return parts[0] to parts[1]
        }

        /** A two-letter USPS code is lowercased; a state name is passed through (the server redirects it). */
        internal fun canonicalState(state: String): String =
            if (state.length == 2 && state.all { it in 'A'..'Z' || it in 'a'..'z' }) state.lowercase() else state

        internal fun locationPath(
            state: String,
            name: String,
        ): String {
            if (state.isEmpty() || name.isEmpty()) throw UsageException("a location needs a state and a slug")
            return "/api/v1/locations/${Query.encode(canonicalState(state))}/${Query.encode(name)}"
        }
    }
}
