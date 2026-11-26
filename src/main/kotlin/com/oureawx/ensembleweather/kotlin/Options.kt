// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

/** Marks the SDK's configuration DSLs, so an inner block cannot reach an outer one by mistake. */
@DslMarker
public annotation class OureaDsl

/**
 * Options for a forecast ([Client.getForecast], [Client.getPointForecast]).
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
@OureaDsl
public open class ForecastOptions internal constructor() {
    /** The unit system; defaults to the client's [Client.Builder.units]. */
    public var units: Units? = null

    /** Hours of the hourly series past the current hour (6..360, default 24; clamped to your tier). */
    public var maxHours: Int? = null

    /** Serve the hourly series from the model run's init, past hours included (default `false`). */
    public var includePast: Boolean? = null

    /** Include per-model breakdowns (Pro; default `false`). */
    public var models: Boolean? = null

    /**
     * Each member's values: [Members.Hourly] at every hour ([TimeSeriesPoint.members]), or [Members.Plume] for
     * the plume view ([ForecastData.plume], columns instead of the hourly series). Default [Members.Off].
     */
    public var members: Members? = null

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

    /** Opt-in diagnostics: `provenance` (each hour's post-processing tags). */
    public var diagnostics: List<String>? = null

    /**
     * Opt-in forecast variables: `mslp` ([EnsembleStats.mslp]) and `skin_temperature`
     * ([EnsembleStats.skinTemperature]); neither by default. Sent sorted.
     */
    public var include: List<String>? = null

    /** Which feels-like indices to serve: `nws`, the only one served, is the default. */
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
@OureaDsl
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
@OureaDsl
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
@OureaDsl
public class NearbyOptions internal constructor() {
    /** The unit system (for elevations); defaults to the client's. */
    public var units: Units? = null

    /** How many neighbours (1..20, default 3). */
    public var limit: Int? = null

    /** The search radius, km (default 50, at most 100). */
    public var radiusKm: Double? = null
}

/** Options for a location search. */
@OureaDsl
public class SearchOptions internal constructor() {
    /** A hard state filter (a USPS code, sent lowercased, or a name). */
    public var state: String? = null

    /** A hard county filter. */
    public var county: String? = null

    /** A hard location-type filter. */
    public var type: LocationType? = null

    /** The most hits (1..100, default 20). */
    public var limit: Int? = null

    /**
     * Rank the hits by an explore metric instead of by relevance (the default, `null`); each hit then carries
     * [SearchHit.explore].
     */
    public var sort: ExploreMetric? = null

    /** The direction of a metric [sort] (default [SortOrder.Descending]). */
    public var order: SortOrder? = null

    /** Only hits within [radiusKm] of this point. */
    public var near: LatLon? = null

    /** The radius around [near], km (above 0, at most 1000; default 100). */
    public var radiusKm: Double? = null

    /** The unit system of [SearchHit.explore] values; defaults to the client's. */
    public var units: Units? = null
}

/** Options for [Client.explore] and [Client.exploreItems]. */
@OureaDsl
public class ExploreOptions internal constructor() {
    /** Highest first ([SortOrder.Descending], the default) or lowest first. */
    public var order: SortOrder? = null

    /** Only places of this type. */
    public var type: LocationType? = null

    /** Only places in this state (a USPS code, sent lowercased). */
    public var state: String? = null

    /** Only places within [radiusKm] of this point; each item then carries [ExploreItem.distanceKm]. */
    public var near: LatLon? = null

    /** The radius around [near], km (above 0, at most 1000; default 100). */
    public var radiusKm: Double? = null

    /** Only points at or above this elevation, in the request's length unit. */
    public var elevationMin: Double? = null

    /** Only points at or below this elevation, in the request's length unit. */
    public var elevationMax: Double? = null

    /** Each place's own point ([ExplorePoints.Primary], the default) or every named elevation too. */
    public var points: ExplorePoints? = null

    /** Page size (1..100, default 25). */
    public var limit: Int? = null

    /** The cursor from the previous page (page calls only). */
    public var cursor: String? = null

    /** The unit system; defaults to the client's. */
    public var units: Units? = null
}

/** Options for [Client.getSeason]. */
@OureaDsl
public class SeasonOptions internal constructor() {
    /** The water year, by its ending year (2027 is 2026-10-01 to 2027-09-30); default the current one. */
    public var waterYear: Int? = null

    /** Opt-in sections: `daily` ([SeasonResponse.daily]). */
    public var include: List<String>? = null

    /** The unit system; defaults to the client's. */
    public var units: Units? = null
}

/** Options for [Client.getSnowTrend]. */
@OureaDsl
public class SnowTrendOptions internal constructor() {
    /** A named elevation of the location (`main_lodge`); default its own point. */
    public var elevation: String? = null

    /** The unit system; defaults to the client's. */
    public var units: Units? = null
}

/** Options for [Client.getSnowpack] and [Client.getSnowpackPlume]. */
@OureaDsl
public class SnowpackOptions internal constructor() {
    /** The unit system; defaults to the client's. */
    public var units: Units? = null
}

/** Options for [Client.getAlertOutlines]. */
@OureaDsl
public class AlertOutlineOptions internal constructor() {
    /** Only these classes (the wire's `class`); default both. Sent sorted, as one comma list. */
    public var classes: List<AlertClass>? = null

    /** Only outlines that intersect this box. */
    public var bbox: BoundingBox? = null
}

/** What [ForecastOptions.members] asks for. */
public enum class Members(
    internal val wire: String,
) {
    /** No member values (`members=false`, the default). */
    Off("false"),

    /** Each member's values at every hour (`members=true`): [TimeSeriesPoint.members]. */
    Hourly("true"),

    /** The plume view (`members=plume`): [ForecastData.plume], columns instead of [ForecastData.timeseries]. */
    Plume("plume"),
}

/** A ranking's direction. */
public enum class SortOrder(
    internal val wire: String,
) {
    /** Highest first (`desc`, the default). */
    Descending("desc"),

    /** Lowest first (`asc`). */
    Ascending("asc"),
}

/** Which points of a place [Client.explore] ranks. */
public enum class ExplorePoints(
    internal val wire: String,
) {
    /** Each place's own point (`primary`, the default). */
    Primary("primary"),

    /** Every named elevation of a place as well (`all`). */
    All("all"),
}

/**
 * A point, degrees: sent as `lat,lon` with a literal comma, each number in its shortest form. Checked when
 * sent: [lat] within ±90, [lon] within ±180.
 */
public data class LatLon(
    /** Latitude, degrees. */
    public val lat: Double,
    /** Longitude, degrees (west negative). */
    public val lon: Double,
)

/**
 * A box, degrees, as RFC 7946's `bbox`: sent as `west,south,east,north` with literal commas. Checked when sent:
 * longitudes within ±180, latitudes within ±90, [west] ≤ [east] and [south] ≤ [north].
 */
public data class BoundingBox(
    /** The western edge (longitude). */
    public val west: Double,
    /** The southern edge (latitude). */
    public val south: Double,
    /** The eastern edge (longitude). */
    public val east: Double,
    /** The northern edge (latitude). */
    public val north: Double,
)

/** Options for the location lists ([Client.locations]). */
@OureaDsl
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

/** Options for the catalog location list ([Client.catalogLocations]). */
@OureaDsl
public class CatalogLocationOptions internal constructor() {
    /** An ISO 3166-2 admin1 filter (`US-OR`). */
    public var admin1: String? = null

    /** An ISO 3166-1 country filter (`US`). */
    public var country: String? = null

    /** A catalog type filter, snake_case (`ski_area`; see [LocationType]). */
    public var type: String? = null

    /** A template variant filter, snake_case (`mountain`; see [TemplateVariant]). */
    public var template: String? = null

    /** Page size (1..100, default 20). */
    public var limit: Int? = null

    /** The cursor from the previous page (page calls only). */
    public var cursor: String? = null
}

/** Options for the catalog station list ([Client.catalogStations]). */
@OureaDsl
public class CatalogStationOptions internal constructor() {
    /** An ISO 3166-2 admin1 filter. */
    public var admin1: String? = null

    /** An ISO 3166-1 country filter. */
    public var country: String? = null

    /** Networks, snake_case (`snotel`, `raws`; see [StationNetwork]). */
    public var network: List<String>? = null

    /** Capabilities every station must have (`swe`, `air_temp`). */
    public var capability: List<String>? = null

    /** A status filter, snake_case (`active`; see [StationStatus]). */
    public var status: String? = null

    /** Page size (1..100, default 20). */
    public var limit: Int? = null

    /** The cursor from the previous page (page calls only). */
    public var cursor: String? = null
}

/** Options for a catalog detail ([Client.getCatalogLocation], [Client.getCatalogStation]). */
@OureaDsl
public class CatalogDetailOptions internal constructor() {
    /** Opt-in extras: `content`, `template`. */
    public var include: List<String>? = null

    /** The BCP-47 locale for content (default `en`). */
    public var locale: String? = null
}
