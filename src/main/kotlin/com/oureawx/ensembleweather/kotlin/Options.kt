// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

/** Marks the SDK's configuration DSLs, so an inner block cannot reach an outer one by mistake. */
@DslMarker
public annotation class EnsembleWeatherDsl

/**
 * Options for a forecast ([EnsembleWeather.getForecast], [EnsembleWeather.getPointForecast]).
 * Leave an option unset for the API's default; an option set to its default is not sent.
 *
 * ```kotlin
 * ourea.getForecast("or/mt-hood-meadows") {
 *     units = Units.Imperial
 *     maxHours = 72
 *     chance(Variable.Snowfall atLeast 12.inches within 72.hours)
 *     total(Variable.Snowfall totalWithin 72.hours)
 * }
 * ```
 */
@EnsembleWeatherDsl
public open class ForecastOptions internal constructor() {
    /** The unit system; defaults to the client's [EnsembleWeather.Builder.units]. */
    public var units: Units? = null

    /** Hours of the hourly series past the current hour (6..360, default 24; clamped to your tier). */
    public var maxHours: Int? = null

    /** Serve the hourly series from the model run's init, past hours included (default `false`). */
    public var includePast: Boolean? = null

    /** Include per-model breakdowns (Pro; default `false`). */
    public var models: Boolean? = null

    /** Include each member's values (default `false`). */
    public var members: Boolean? = null

    /** Include daily summaries (default `true`). */
    public var daily: Boolean? = null

    /** The number of daily rows (1..15; default the tier's full horizon). */
    public var maxDays: Int? = null

    /** Include the hourly series (default `true`). */
    public var hourly: Boolean? = null

    /** A specific model run's init time, ISO 8601 (Pro). */
    public var run: String? = null

    /** An allowlist of observation sources for the nowcast (`snotel`, `ski_resort`, ...). */
    public var sources: List<String>? = null

    /** A denylist of observation sources. */
    public var excludeSources: List<String>? = null

    /** `true` (default): the live nowcast layer; `false`: the cacheable base forecast. */
    public var nowcast: Boolean? = null

    /** Opt-in diagnostics: `provenance`, `ptype`. */
    public var diagnostics: List<String>? = null

    /** Which feels-like indices to serve (default `nws`). */
    public var feelsLike: List<String>? = null

    internal val probabilityTerms = mutableListOf<ProbabilityTerm>()
    internal val totalTerms = mutableListOf<WindowTotalTerm>()

    /**
     * Asks the server for the chance of [term] (BEHAVIOR.md section 4). Answers come
     * back in [ForecastResponse.probabilities], in the order asked.
     */
    public fun chance(term: ProbabilityTerm) {
        probabilityTerms += term
    }

    /** Asks for several probabilities at once. */
    public fun chance(vararg terms: ProbabilityTerm) {
        probabilityTerms += terms
    }

    /**
     * Asks the server for the distribution of each member's own total over a window
     * (BEHAVIOR.md section 5). Answers come back in [ForecastResponse.windowTotals].
     */
    public fun total(term: WindowTotalTerm) {
        totalTerms += term
    }

    /** Asks for several window totals at once. */
    public fun total(vararg terms: WindowTotalTerm) {
        totalTerms += terms
    }
}

/** Options for a named-location forecast: [ForecastOptions] plus [elevations]. */
@EnsembleWeatherDsl
public class LocationForecastOptions internal constructor() : ForecastOptions() {
    /**
     * Extra elevations to forecast (ski areas): names from the location's elevations, or
     * [ALL_ELEVATIONS] for every one.
     */
    public var elevations: List<String>? = null

    public companion object {
        /** Every extra elevation the location has. */
        public val ALL_ELEVATIONS: List<String> = listOf("all")
    }
}

/** Options for current conditions. */
@EnsembleWeatherDsl
public class CurrentOptions internal constructor() {
    /** The unit system; defaults to the client's. */
    public var units: Units? = null

    /** An allowlist of observation sources. */
    public var sources: List<String>? = null

    /** A denylist of observation sources. */
    public var excludeSources: List<String>? = null

    /** Opt-in sections: `nowcast_hours`. */
    public var include: List<String>? = null
}

/** Options for nearby locations. */
@EnsembleWeatherDsl
public class NearbyOptions internal constructor() {
    /** The unit system (for elevations); defaults to the client's. */
    public var units: Units? = null

    /** How many neighbours (1..20, default 3). */
    public var limit: Int? = null

    /** The search radius, km (default 50, at most 100). */
    public var radiusKm: Double? = null
}

/** Options for a location search. */
@EnsembleWeatherDsl
public class SearchOptions internal constructor() {
    /** A hard state filter (code or name). */
    public var state: String? = null

    /** A hard county filter. */
    public var county: String? = null

    /** A hard location-type filter. */
    public var type: LocationType? = null

    /** The most hits (1..100, default 20). */
    public var limit: Int? = null
}

/** Options for the location lists ([EnsembleWeather.locations]). */
@EnsembleWeatherDsl
public class LocationListOptions internal constructor() {
    /** Only this location type. */
    public var type: LocationType? = null

    /** The unit system; defaults to the client's. */
    public var units: Units? = null

    /** Page size (1..5000). */
    public var limit: Int? = null

    /** The cursor from the previous page (page calls only). */
    public var cursor: String? = null
}

/** Options for the catalog location list ([EnsembleWeather.catalogLocations]). */
@EnsembleWeatherDsl
public class CatalogLocationOptions internal constructor() {
    /** An ISO 3166-2 admin1 filter (`US-OR`). */
    public var admin1: String? = null

    /** An ISO 3166-1 country filter (`US`). */
    public var country: String? = null

    /** A catalog type filter (`SkiArea`). */
    public var type: String? = null

    /** A template variant filter (`Mountain`). */
    public var template: String? = null

    /** Page size (1..100, default 20). */
    public var limit: Int? = null

    /** The cursor from the previous page (page calls only). */
    public var cursor: String? = null
}

/** Options for the catalog station list ([EnsembleWeather.catalogStations]). */
@EnsembleWeatherDsl
public class CatalogStationOptions internal constructor() {
    /** An ISO 3166-2 admin1 filter. */
    public var admin1: String? = null

    /** An ISO 3166-1 country filter. */
    public var country: String? = null

    /** Networks (`Snotel`, `Raws`). */
    public var network: List<String>? = null

    /** Capabilities every station must have (`swe`, `air_temp`). */
    public var capability: List<String>? = null

    /** A status filter (`Active`). */
    public var status: String? = null

    /** Page size (1..100, default 20). */
    public var limit: Int? = null

    /** The cursor from the previous page (page calls only). */
    public var cursor: String? = null
}

/** Options for a catalog detail ([EnsembleWeather.getCatalogLocation], [EnsembleWeather.getCatalogStation]). */
@EnsembleWeatherDsl
public class CatalogDetailOptions internal constructor() {
    /** Opt-in extras: `content`, `template`, `ads`. */
    public var include: List<String>? = null

    /** The BCP-47 locale for content (default `en`). */
    public var locale: String? = null
}
