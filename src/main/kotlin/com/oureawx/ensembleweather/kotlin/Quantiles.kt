// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The one distribution shape: every ensemble statistic the API serves (each hour's
 * `ensemble.*`, the daily `high`/`low`/totals, window totals, snow-outlook ranges).
 *
 * Every field is the server's number in the request's [Units], and every field may
 * be absent: an absent percentile is `null`, never `0`. The SDK never computes a
 * percentile, sums quantiles or interpolates between them; ask the server for a
 * probability ([ForecastOptions.chance]) or a window total ([ForecastOptions.total])
 * instead. See <https://docs.oureawx.com/concepts/probabilities>.
 *
 * `wind_direction` quantiles describe an arc on the compass: `max` may be numerically
 * less than `min`, and `p90` less than `p10`, when the arc crosses north. That is
 * correct, not a bug; do not sort or clamp them.
 */
@Serializable
public data class Quantiles(
    /** The ensemble mean. For precipitation and snowfall prefer [p50] for display. */
    public val mean: Double? = null,
    /** The 10th percentile. */
    public val p10: Double? = null,
    /** The 25th percentile. */
    public val p25: Double? = null,
    /** The 50th percentile: the median. */
    public val p50: Double? = null,
    /** The 75th percentile. */
    public val p75: Double? = null,
    /** The 90th percentile. */
    public val p90: Double? = null,
    /** The ensemble minimum (the anticlockwise end of the arc for wind direction). */
    public val min: Double? = null,
    /** The ensemble maximum (the clockwise end of the arc for wind direction). */
    public val max: Double? = null,
    /**
     * Only on an hour's precipitation and snowfall: the server's probability of
     * occurrence (at least a trace). Absent everywhere else.
     */
    public val probability: Double? = null,
) {
    /** The median ([p50]), when served. It is the median, not "the forecast". */
    public val median: Double? get() = p50

    /**
     * The p10 to p90 range: the central 80 % of the members, when both are served.
     * These are the server's two percentiles as served; nothing is interpolated.
     */
    public val p10ToP90: QuantileRange? get() = QuantileRange.of(p10, p90)

    /** The p25 to p75 range: the central 50 % of the members, when both are served. */
    public val p25ToP75: QuantileRange? get() = QuantileRange.of(p25, p75)
}

/**
 * Two served percentiles, [low] and [high], as a pair. Only the pairs the server
 * serves exist ([Quantiles.p10ToP90], [Quantiles.p25ToP75]), so there is no way to ask
 * for an interpolated one. For wind direction [high] may be less than [low].
 */
public data class QuantileRange(
    /** The lower percentile. */
    public val low: Double,
    /** The upper percentile. */
    public val high: Double,
) {
    override fun toString(): String = "$low..$high"

    internal companion object {
        fun of(
            low: Double?,
            high: Double?,
        ): QuantileRange? = if (low != null && high != null) QuantileRange(low, high) else null
    }
}

/** The one location reference, the same wherever a place is named. */
@Serializable
public data class LocationRef(
    /** The catalog slug: with [state], the location's address. */
    public val slug: String,
    /** The lowercase USPS state code. */
    public val state: String,
    /** The display name. */
    public val name: String,
    /** Latitude, degrees. */
    public val lat: Double,
    /** Longitude, degrees (west negative). */
    public val lon: Double,
    /** Elevation in metres, whatever the units. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The primary location type. */
    public val type: LocationType,
    /** The IANA time zone the location's local days are kept in. */
    public val timezone: String,
)

/** The data sources a response drew on, and the credit their licences ask for. */
@Serializable
public data class ResponseAttribution(
    /** Source ids from [EnsembleWeather.getAttribution]. */
    public val sources: List<String>,
    /** One credit line per provider; carry it with any redistributed output. */
    public val credit: String,
    /** Path of the full attribution registry. */
    public val url: String,
)

/** One contributing model. */
@Serializable
public data class ModelEntry(
    /** The model id (`aifs`, `aigefs`, `gefs`, `ifs_ens`, `rrfs`; others may be added). */
    public val id: String,
    /** The model's own cycle init (ISO 8601), when it was read. */
    public val cycle: String? = null,
    /** How many of its members the forecast carries. */
    public val members: Int? = null,
)
