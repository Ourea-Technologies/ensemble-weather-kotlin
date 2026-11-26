// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A short plain-language summary of what is coming at a place, written by the server from templates filled with served
 * values (never free text from a model).
 */
@Serializable
public data class PlaceSummary(
    /**
     * One sentence: the most important thing at the place, by priority (an NWS alert, precipitation starting or
     * stopping soon, snow coming, wind or a large temperature change, then the day's headline).
     */
    public val short: String,
    /** One to three sentences for the location page, each from a different kind of event. */
    public val long: List<String>,
    /**
     * The event behind each sentence of `long`, in the same order (`events[i]` wrote `long[i]`), so a client can
     * style a sentence (an alert chip, a snow icon) without parsing text.
     */
    public val events: List<SummaryEvent>,
    /** The served elevation the sentences describe: its display name where the catalog has one, else its name. */
    @SerialName("elevation_name") public val elevationName: String? = null,
    /** The height the sentences describe, in the response's length units (m, ft). */
    public val elevation: Double,
    /** The init of the forecast run the sentences were written from. */
    public val run: String,
    /** When the sentences were written (the request's now); the "updated" stamp. */
    @SerialName("generated_at") public val generatedAt: String,
)

/** The `SummaryEvent` shape. */
@Serializable
public data class SummaryEvent(
    /**
     * What the sentence is about. One of `alert`, `precip_starting`, `precip_stopping`, `snow_coming`, `wind`,
     * `temperature_change`, `day_headline`, `pass_snow_starts`, `pass_rain_to_snow`, `pass_freezing_on_wet_road`.
     */
    public val kind: String,
    /**
     * The phrasing used, `<template>.<variant>` (`snow_coming.v1.3`), stable across releases so sentences can be
     * counted, reviewed and later translated.
     */
    public val template: String,
    /** The hour the event begins (precipitation starting or stopping, an alert's onset, a pass event). */
    public val at: String? = null,
    /** When the event ends, where the sentence says so (an alert's end, a snow window's end). */
    public val until: String? = null,
    /** The `alerts[].id` an `alert` event states. */
    @SerialName("alert_id") public val alertId: String? = null,
    /** The served numbers the sentence states, unrounded, in the response's units. */
    public val values: SummaryValues? = null,
)

/** Each number a summary sentence states, as served. */
@Serializable
public data class SummaryValues(
    /**
     * The window's headline snowfall total (mm, in): for `snow_coming` the forecast's
     * `snow_outlook.<snowfall_window>`; for `pass_snow_starts` the window's `pass.snow_windows[].total`.
     */
    public val snowfall: Double? = null,
    /** The window's served p10 (`snow_outlook.quantiles.<window>.p10`, or the pass window's `p10`). */
    @SerialName("snowfall_p10") public val snowfallP10: Double? = null,
    /** The window's served p90. */
    @SerialName("snowfall_p90") public val snowfallP90: Double? = null,
    /** Which window `snowfall` totals. One of `next_24h`, `next_48h`, `next_7d`, `pass_snow_window`. */
    @SerialName("snowfall_window") public val snowfallWindow: String? = null,
    /** A day's served high (`daily[].high.mean`) a `day_headline` or `temperature_change` states. */
    public val temperature: Double? = null,
    /** The served hourly gust mean (`ensemble.wind_gust.mean`) a `wind` sentence states. */
    @SerialName("wind_gust") public val windGust: Double? = null,
    /** The served chance (0-1) behind the sentence's likelihood word. */
    @SerialName("precipitation_probability") public val precipitationProbability: Double? = null,
)

/**
 * "Is the pass getting weather?" at the pass's own point and elevation: forecast quantities only, in the response's
 * units.
 */
@Serializable
public data class PassForecast(
    /** The pass's elevation, in the response's length units (m, ft). */
    public val elevation: Double,
    /** The pass's elevation in metres, whatever `units` is. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The first hour of `hours` (the hour containing now); the 24 h and 48 h windows open here. */
    public val from: String,
    /** The snow 24h. */
    @SerialName("snow_24h") public val snow24h: PassSnow24h,
    /** The chances. */
    public val chances: PassChances,
    /** Each spell of snow at the pass in the next 48 h, in time order; empty when none. */
    @SerialName("snow_windows") public val snowWindows: List<PassSnowWindow>,
    /** The next 48 hours at the pass, from `from`, one per hour. */
    public val hours: List<PassHour>,
    /** The windows. */
    public val windows: PassTravelWindows,
    /** The official status. */
    @SerialName("official_status") public val officialStatus: PassOfficialStatus? = null,
    /**
     * The state DOT road-weather stations nearest the pass, nearest first: at most 3, within 25 km, each with its
     * newest reading in the last 3 h.
     */
    @SerialName("road_sensors") public val roadSensors: List<PassRoadSensor>? = null,
    /** The DOT's own cameras at the pass, in the order the pass's curation lists them. */
    public val cameras: List<PassCamera>? = null,
)

/**
 * The next 24 h of snow at the pass: exactly the default forecast's own `snow_outlook.next_24h` and
 * `snow_outlook.quantiles.next_24h` (unpinned, the nowcast on, the same cycle), so the pass page, the passes board and
 * search state one number.
 */
@Serializable
public data class PassSnow24h(
    /** `snow_outlook.next_24h` (mm, in); null where the forecast does not reach 24 h. */
    public val total: Double? = null,
    /** `snow_outlook.quantiles.next_24h.p10`; null without members covering the window. */
    public val p10: Double? = null,
    /** `snow_outlook.quantiles.next_24h.p90`. */
    public val p90: Double? = null,
    /** The start of the first snow spell (`snow_windows[0].start`) when it begins inside the 24 h; null otherwise. */
    @SerialName("starts_at") public val startsAt: String? = null,
)

/**
 * Chances at the pass over the next `window_hours` hours, each the share of the forecast's members meeting the event
 * (the `?prob=` machinery), except the gust chance (see its `method`).
 */
@Serializable
public data class PassChances(
    /** The window's length, 24. */
    @SerialName("window_hours") public val windowHours: Int,
    /** The from. */
    public val from: String,
    /** The to. */
    public val to: String,
    /** Snowfall total over the window of 1 in (25.4 mm) or more: `prob=snowfall:ge:<1 in>:24h`. */
    @SerialName("p_snow_ge_1in") public val pSnowGe1in: PassChance,
    /** Snowfall total over the window of 6 in (152.4 mm) or more. */
    @SerialName("p_snow_ge_6in") public val pSnowGe6in: PassChance,
    /**
     * Some hour of the window in which the member is at or below freezing (`threshold`: 0 C, 32 F) with
     * precipitation of at least `precipitation_threshold` (0.254 mm, 0.01 in) in the member's value ending at that
     * hour: a wet road at or below freezing.
     */
    @SerialName("p_wet_below_freezing") public val pWetBelowFreezing: PassChance,
    /** A gust over 40 mph (17.9 m/s; threshold fixed, does not change with units) in the window. */
    @SerialName("p_gust_gt_40mph") public val pGustGt40mph: PassChance,
)

/** The `PassChance` shape. */
@Serializable
public data class PassChance(
    /** 0-1 (0.001); null when it cannot be answered (`null_reason`). */
    public val probability: Double? = null,
    /** The event's threshold, in the response's units. */
    public val threshold: Double,
    /** For `p_wet_below_freezing`, the precipitation that makes an hour wet, in the response's units. */
    @SerialName("precipitation_threshold") public val precipitationThreshold: Double? = null,
    /** The members that voted (0 for `hourly_gust_distribution`). */
    public val members: Int,
    /** The method. One of `members`, `hourly_gust_distribution`. */
    public val method: String,
    /** The null reason. */
    @SerialName("null_reason") public val nullReason: NullReason? = null,
)

/** The `PassSnowWindow` shape. */
@Serializable
public data class PassSnowWindow(
    /** When the first snowing hour begins (its `valid_time` minus 1 h). */
    public val start: String,
    /** When the last snowing hour ends (its `valid_time`). */
    public val end: String,
    /** The sum of the hourly snowfall means over the spell (mm, in), as `snow_outlook` sums its windows. */
    public val total: Double,
    /** The p10 of the members' own totals over the spell (`?totals=` machinery); omitted without enough members. */
    public val p10: Double? = null,
    /** The p90 of the members' own totals over the spell. */
    public val p90: Double? = null,
    /**
     * The share of members whose own total over the spell is 1 in (25.4 mm) or more; threshold fixed, does not
     * change with units.
     */
    @SerialName("p_ge_1in") public val pGe1in: Double? = null,
)

/** The `PassHour` shape. */
@Serializable
public data class PassHour(
    /** The hour's `valid_time`; amounts are over the hour ending then. */
    public val time: String,
    /** `ensemble.temperature.mean`. */
    public val temperature: Double,
    /** `ensemble.precipitation.mean` (mm, in). */
    public val precipitation: Double,
    /** `ensemble.precipitation.probability`, 0-1. */
    @SerialName("precipitation_probability") public val precipitationProbability: Double? = null,
    /** The hour's served `precipitation_type.type`; omitted on a dry hour. */
    @SerialName("precipitation_type") public val precipitationType: PrecipitationKind? = null,
    /** `ensemble.snowfall.mean` (mm, in); 0 without snow. */
    public val snowfall: Double,
    /** `ensemble.snow_level.mean` (m, ft); omitted where not served. */
    @SerialName("snow_level") public val snowLevel: Double? = null,
    /**
     * (JSON `snow_level_above_pass`) `snow_level` minus the pass's elevation (m, ft): above 0 the snow level is
     * above the pass (rain at the pass when wet), below 0 it is under it.
     */
    @SerialName("snow_level_above_pass") public val snowLevelMargin: Double? = null,
    /**
     * The hour is at or below freezing (temperature mean at or below 0 C, 32 F) and wet: precipitation of at least
     * 0.254 mm (0.01 in) in this hour or one of the two before it.
     */
    @SerialName("freezing_on_wet_road") public val freezingOnWetRoad: Boolean,
    /** `ensemble.wind_speed.mean`. */
    @SerialName("wind_speed") public val windSpeed: Double,
    /** `ensemble.wind_gust.mean`; omitted where not served. */
    @SerialName("wind_gust") public val windGust: Double? = null,
    /** `wind_gust` is over 40 mph (17.9 m/s); threshold fixed, does not change with units. */
    @SerialName("gust_over_40mph") public val gustOver40mph: Boolean,
    /**
     * Gusts of 25 mph (11.2 m/s) or more at or below freezing with loose snow to move: snow falling this hour, 1 in
     * (25.4 mm) or more fallen in the 24 served hours before it, or snow on the ground at the pass
     * (`snow_on_ground`).
     */
    @SerialName("blowing_snow") public val blowingSnow: Boolean,
)

/**
 * The quietest and the busiest stretch of weather at the pass in the next `horizon_hours`, by one fixed rule on the
 * hours above: an hour has weather when it is snowing (`snowfall` at least 0.04 in, 1 mm), `freezing_on_wet_road`, or
 * `gust_over_40mph`.
 */
@Serializable
public data class PassTravelWindows(
    /** The horizon's length in hours, 24. */
    @SerialName("horizon_hours") public val horizonHours: Int,
    /** Null when every hour has weather. */
    @SerialName("least_weather") public val leastWeather: PassTravelWindow? = null,
    /** Null when no hour has weather. */
    @SerialName("most_weather") public val mostWeather: PassTravelWindow? = null,
)

/** The `PassTravelWindow` shape. */
@Serializable
public data class PassTravelWindow(
    /** When the run's first hour begins. */
    public val start: String,
    /** When its last hour ends. */
    public val end: String,
    /** The sum of the hourly snowfall means over the run (mm, in). */
    public val snowfall: Double,
    /** The freezing on wet road, hours. */
    @SerialName("freezing_on_wet_road_hours") public val freezingOnWetRoadHours: Int,
    /** The highest hourly gust mean in the run; omitted where no gust is served. */
    @SerialName("max_wind_gust") public val maxWindGust: Double? = null,
)

/**
 * The DOT's own status for the road over the pass -- chain controls and closures as the DOT publishes them, its wording
 * verbatim.
 */
@Serializable
public data class PassOfficialStatus(
    /** The DOT feed. One of `caltrans`, `wsdot`, `cdot`, `odot`. */
    public val source: String,
    /** Who to credit, as the client shows it. */
    public val attribution: String,
    /** The DOT's own page for this road, to link out to. */
    public val url: String,
    /**
     * How fresh the DOT's feed behind these segments was at Ourea's last read: the time the DOT wrote the file
     * Ourea last read (Caltrans rewrites it every minute; Ourea reads it every 5), and the older of two when the
     * segments span two DOT districts.
     */
    @SerialName("checked_at") public val checkedAt: String,
    /** `checked_at` is more than 30 minutes before `as_of`. */
    public val stale: Boolean,
    /** The level. */
    public val level: PassStatusLevel,
    /**
     * The DOT's chain-control and closure points on the road over the pass, each direction, in the pass's curated
     * order.
     */
    public val segments: List<PassStatusSegment>,
)

/** One DOT chain-control or closure point, as the DOT publishes it. */
@Serializable
public data class PassStatusSegment(
    /** The DOT's own id for the point. */
    public val id: String,
    /** The route. */
    public val route: String,
    /** The direction the point controls, as the DOT names it. */
    public val direction: String,
    /** The DOT's name for the point (its trailing field padding removed). */
    public val location: String,
    /** (JSON `status`) The DOT's status code, verbatim. */
    @SerialName("status") public val state: String,
    /** The level. */
    public val level: PassStatusLevel,
    /** (JSON `status_text`) The DOT's own description of the status, verbatim. */
    @SerialName("status_text") public val text: String,
    /** When the DOT set this status. */
    @SerialName("updated_at") public val updatedAt: String,
    /** The lat. */
    public val lat: Double,
    /** The lon. */
    public val lon: Double,
    /**
     * The DOT's own report of the road surface there, verbatim, where it gives one beside the restriction (WSDOT,
     * ODOT).
     */
    @SerialName("road_condition") public val roadCondition: String? = null,
    /** The DOT's own report of the weather there, verbatim (WSDOT, ODOT). */
    @SerialName("weather_condition") public val weatherCondition: String? = null,
)

/** One state DOT road-weather station's newest reading, in the response's units. */
@Serializable
public data class PassRoadSensor(
    /** The station's id in its network. */
    @SerialName("station_id") public val stationId: String,
    /** The station's registry name, else its id. */
    public val name: String,
    /** The feed: `rwis` (Caltrans CWWP2 RWIS) or `wsdot`. One of `rwis`, `wsdot`. */
    public val source: String,
    /** Who measured it, as the client credits it. */
    public val attribution: String,
    /** Straight-line distance from the pass, km. */
    @SerialName("distance_km") public val distanceKm: Double,
    /** The station's registry elevation in the response's length units (m, ft). */
    public val elevation: Double? = null,
    /** The station's registry elevation in metres. */
    @SerialName("elevation_m") public val elevationM: Double? = null,
    /** The observed at. */
    @SerialName("observed_at") public val observedAt: String,
    /** `observed_at` is more than 60 minutes before `as_of`. */
    public val stale: Boolean,
    /**
     * The pavement state the sensor reports (NTCIP 1204 essSurfaceStatus as `/current` sensors serve it: dry,
     * trace_moisture, wet, chemically_wet, ice_warning, ice_watch, snow_warning, snow_watch, absorption, dew,
     * frost, absorption_at_dewpoint).
     */
    @SerialName("pavement_condition") public val pavementCondition: String? = null,
    /** Pavement surface temperature: °C, °F (imperial) or K (kelvin). */
    @SerialName("pavement_temp") public val pavementTemp: Double? = null,
    /** As `pavement_temp`. */
    @SerialName("subsurface_temp") public val subsurfaceTemp: Double? = null,
    /** Air temperature, as `pavement_temp`. */
    public val temperature: Double? = null,
    /** M/s or mph (imperial). */
    @SerialName("wind_speed") public val windSpeed: Double? = null,
    /** As `wind_speed`. */
    @SerialName("wind_gust") public val windGust: Double? = null,
    /** Degrees clockwise from north. */
    @SerialName("wind_direction") public val windDirection: Double? = null,
    /** M, or statute miles (imperial). */
    public val visibility: Double? = null,
)

/** One DOT camera, a still image the DOT refreshes about once a minute. */
@Serializable
public data class PassCamera(
    /** The id. */
    public val id: String,
    /** The DOT's name for the camera. */
    public val title: String,
    /** The DOT's current still image. */
    @SerialName("image_url") public val imageUrl: String,
    /** The DOT's own page for the camera, to link out to. */
    public val url: String,
    /**
     * The image's time (its Last-Modified) when Ourea last checked it, every 15 minutes; the image at `image_url`
     * may be newer.
     */
    @SerialName("captured_at") public val capturedAt: String? = null,
    /** `captured_at` is unknown or more than 60 minutes before `as_of`: the camera may be frozen. */
    public val stale: Boolean,
    /** The source. One of `caltrans`, `wsdot`, `cdot`, `odot`. */
    public val source: String,
    /** The attribution. */
    public val attribution: String,
)
