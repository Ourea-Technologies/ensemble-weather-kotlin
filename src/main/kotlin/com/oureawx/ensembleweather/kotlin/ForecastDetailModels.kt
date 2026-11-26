// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The forecast's `location` -- the one `LocationRef` (every field where it was) plus what a forecast client labels and
 * picks with, so the forecast body alone is enough to draw a plume or a meteogram at a named elevation (no `/nearby`
 * call, no client-side unit conversion).
 */
@Serializable
public data class ForecastLocation(
    /** The location's key: its catalog slug. */
    public val slug: String? = null,
    /** The lowercase USPS state code. */
    public val state: String,
    /** The display name. */
    public val name: String,
    /** Latitude, degrees. */
    public val lat: Double,
    /** Longitude, degrees (west negative). */
    public val lon: Double,
    /** Elevation in metres, whatever `units` is (0.1 m). */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The primary location type, snake_case. */
    public val type: LocationType? = null,
    /**
     * The IANA zone the location's local days and hours are kept in -- the zone `daily` dates, sunrise and sunset
     * are computed in.
     */
    public val timezone: String,
    /**
     * `elevation_m` in the request's length units (m for `metric`/`kelvin`, ft for `imperial`), to 0.1 -- the
     * elevation `forecast.timeseries` (and its members) is served at.
     */
    public val elevation: Double,
    /**
     * The location's named elevations -- exactly the list its `/nearby` and list rows serve
     * (`LocationSummary.elevations`): the primary first and its ONE `default: true`, then each other elevation
     * (ski-area base / mid / summit), in the request's length units.
     */
    public val elevations: List<ElevationSummary>? = null,
)

/**
 * One of a location's elevation points, as `/nearby`, the list rows and the forecast's `location.elevations` serve
 * it.
 */
@Serializable
public data class ElevationSummary(
    /** The elevation's id: the name to pass in `?elevations=` (case, `-`, `_` and spaces ignored). */
    public val name: String,
    /** What the place is called, for display ("K-1 Lodge", "FourRunner Quad top"). */
    @SerialName("display_name") public val displayName: String? = null,
    /** (JSON `elevation`) In the caller's length units (m, ft), to 0.1. */
    @SerialName("elevation") public val height: Double,
    /** Present (true) on the primary only. */
    @SerialName("default") public val isDefault: Boolean? = null,
)

/**
 * A distribution over the ensemble MEMBERS (`members_cumulative`, `members_spread`, `members_6h`): the `Quantiles`
 * shape.
 */
@Serializable
public data class MemberQuantiles(
    /** The mean. */
    public val mean: Double,
    /** The p10. */
    public val p10: Double,
    /** The p25. */
    public val p25: Double,
    /** The members' median. */
    public val p50: Double? = null,
    /** The p75. */
    public val p75: Double,
    /** The p90. */
    public val p90: Double,
    /** The min. */
    public val min: Double,
    /** The max. */
    public val max: Double,
    /** As `Quantiles.probability`, where the use says. */
    public val probability: Double? = null,
)

/**
 * The members' spread at this hour for the plume's instantaneous variables -- the Quantiles of every member's own
 * `temperature` (temperature unit) and `snow_level` (length unit) here, over the members as served.
 */
@Serializable
public data class MembersSpread(
    /** The temperature. */
    public val temperature: MemberQuantiles? = null,
    /** The snow level. */
    @SerialName("snow_level") public val snowLevel: MemberQuantiles? = null,
    /**
     * The Quantiles of the members' own `snow_level_wet_bulb` (the snow level on the NWS scale), over the members
     * `snow_level` is over.
     */
    @SerialName("snow_level_wet_bulb") public val snowLevelWetBulb: MemberQuantiles? = null,
    /**
     * The snow-to-liquid ratio over the members SNOWING at this hour (their own `snowfall` above 0), at
     * 00/06/12/18Z only, in both member views.
     */
    public val slr: MemberQuantiles? = null,
    /** The members' own wind speed at this hour, in the wind unit. */
    @SerialName("wind_speed") public val windSpeed: MemberQuantiles? = null,
)

/**
 * Rolling liquid-equivalent totals over the same windows as `snow_outlook`, for places where rain is the story (the
 * favorites cards' "0.6 in rain" with its range).
 */
@Serializable
public data class PrecipOutlook(
    /** Sum of the hourly precipitation means over the 24 h from the first served hour; `null` when not covered. */
    @SerialName("next_24h") public val next24h: Double? = null,
    /** The same over 48 h. */
    @SerialName("next_48h") public val next48h: Double? = null,
    /** The same over 72 h. */
    @SerialName("next_72h") public val next72h: Double? = null,
    /** The same over 168 h. */
    @SerialName("next_7d") public val next7d: Double? = null,
    /** The quantiles. */
    public val quantiles: SnowOutlookQuantiles? = null,
)

/**
 * This hour's headline precipitation (`ensemble.precipitation.mean`) split by phase, in the precipitation units --
 * `rain + snow + rain_snow_mix + freezing_rain` is the hour's mean (to the served precision), so a stacked bar is the
 * headline bar.
 */
@Serializable
public data class PrecipitationByType(
    /** The rain. */
    public val rain: Double,
    /** The liquid that falls as snow (not snow depth; that is `ensemble.snowfall`). */
    public val snow: Double,
    /** The rain snow mix. */
    @SerialName("rain_snow_mix") public val rainSnowMix: Double,
    /** Liquid that freezes on contact (counted in `rain` before). */
    @SerialName("freezing_rain") public val freezingRain: Double,
)

/**
 * A day's snow level over its wet hours (m, or ft with `units=imperial`): the lowest and highest hourly ensemble-mean
 * snow level among them, and their precipitation-weighted mean.
 */
@Serializable
public data class SnowLevelRange(
    /** The lowest hourly snow level among the wet hours. */
    public val low: Double,
    /** The highest hourly snow level among the wet hours. */
    public val high: Double,
    /** The wet hours' snow level, weighted by their precipitation. */
    public val mean: Double,
)

/** What the snowpack model states is on the ground at a spot at one hour. */
@Serializable
public data class SnowOnGround(
    /** Always `true` while the product is a beta; fields may be added. */
    public val beta: Boolean,
    /** Snow depth: cm, or in (imperial). */
    public val depth: Double,
    /** `measured`: the run started from a snow-depth sensor at the spot. */
    public val basis: String,
    /**
     * The surface class at the hour; `/snowpack`'s `definitions.surface` carries the text of each value (an open
     * set).
     */
    public val surface: String? = null,
    /** The hour the depth is for. */
    @SerialName("valid_time") public val validTime: String,
    /** When the snowpack run that produced it finished. */
    @SerialName("generated_at") public val generatedAt: String,
)

/**
 * The day's climate normal at the forecast point: the mean, over the day of year +/-15 days of every year of the
 * station's record, of the very quantities this row states -- the local CALENDAR date's maximum hourly temperature
 * (`high`, the quantity `high` is), its minimum (`low`) and its precipitation ...
 */
@Serializable
public data class DayNormal(
    /** Mean local-calendar-day maximum temperature. */
    public val high: Double? = null,
    /** Mean local-calendar-day minimum temperature. */
    public val low: Double? = null,
    /** Mean local-calendar-day precipitation total, dry days included. */
    public val precipitation: Double? = null,
    /** A climatology station's own record, or the ERA5 cell of the point. One of `station`, `era5_cell`. */
    public val source: String,
    /** Great-circle distance from the point to the station (or the cell centre), km. */
    @SerialName("distance_km") public val distanceKm: Double,
)

/**
 * The served headline minus the normal, in the request's units: `high.p50 - normal.high` and
 * `low.p50 - normal.low`.
 */
@Serializable
public data class DayDeparture(
    /** The high. */
    public val high: Double? = null,
    /** The low. */
    public val low: Double? = null,
)

/**
 * How unusual the day is: where the served headline (`p50`) falls in the day's climatological distribution, as a
 * percentile 0-100, `Pr{climate <= forecast}` -- 95 for `precipitation` reads "wetter than 95% of this date's days".
 */
@Serializable
public data class DayPercentile(
    /** The high. */
    public val high: Double? = null,
    /** The low. */
    public val low: Double? = null,
    /** The precipitation. */
    public val precipitation: Double? = null,
)

/**
 * The avalanche center's danger rating for the forecast zone the location's point is in, from avalanche.org's public
 * map layer.
 */
@Serializable
public data class AvalancheForecast(
    /** The avalanche center's short id. */
    @SerialName("center_id") public val centerId: String,
    /** The center name. */
    @SerialName("center_name") public val centerName: String,
    /**
     * Avalanche.org's id for the forecast zone; for a CAIC forecast area, an id at or above 2^52 derived from
     * CAIC's area id (stable for that grouping of CAIC's polygons).
     */
    @SerialName("zone_id") public val zoneId: Long,
    /** The zone name. */
    @SerialName("zone_name") public val zoneName: String,
    /** The North American Public Avalanche Danger Scale level, 1-5; present only when `status` is `rated`. */
    @SerialName("danger_level") public val dangerLevel: Int? = null,
    /**
     * Display text: the scale's English name for `danger_level` (Low, Moderate, Considerable, High, Extreme),
     * derived from the level, never the center's own wording; `null` exactly when `danger_level` is.
     */
    @SerialName("danger_label") public val dangerLabel: String? = null,
    /** `rated`: a current rating. One of `rated`, `no_rating`, `off_season`, `unavailable`. */
    public val status: String,
    /** When the center's current product took effect (UTC), when stated. */
    @SerialName("valid_from") public val validFrom: String? = null,
    /** When the center's current product expires (UTC), when stated. */
    @SerialName("valid_until") public val validUntil: String? = null,
    /** (JSON `forecast_url`) The center's full forecast for the zone. */
    @SerialName("forecast_url") public val url: String,
    /** The center url. */
    @SerialName("center_url") public val centerUrl: String,
    /**
     * The credit to show with the block: "Avalanche forecast by <center> via avalanche.org", or "Avalanche forecast
     * by Colorado Avalanche Information Center via avalanche.state.co.us" for a CAIC area.
     */
    public val attribution: String,
)
