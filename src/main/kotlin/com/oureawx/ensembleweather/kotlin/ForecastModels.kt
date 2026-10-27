// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A forecast for a named location ([EnsembleWeather.getForecast]) or a point
 * ([EnsembleWeather.getPointForecast]). Every value is in [units].
 */
@Serializable
public data class ForecastResponse(
    /** Where the forecast is for. */
    public val location: LocationRef,
    /** The models and cycles it was blended from. */
    @SerialName("model_run") public val modelRun: ModelRunInfo,
    /** The unit system of every value in this body. */
    public val units: Units,
    /** The hourly series, daily summaries and snow outlook. */
    public val forecast: ForecastData,
    /** Whether the live nowcast layer is baked in. */
    @SerialName("nowcast_applied") public val nowcastApplied: Boolean,
    /** The data sources and their credit line. */
    public val attribution: ResponseAttribution,
    /** The event behind each served probability, in the request's units. */
    public val thresholds: ProbabilityThresholds? = null,
    /**
     * The answers to [ForecastOptions.chance], in the order you asked (a duplicate term
     * shares one answer). Absent when no probability was asked for.
     */
    public val probabilities: List<ProbabilityResult>? = null,
    /** The answers to [ForecastOptions.total], in the order you asked. Absent when none was asked for. */
    @SerialName("window_totals") public val windowTotals: List<WindowTotal>? = null,
    /** The options the server applied after its tier clamps. */
    public val applied: AppliedOptions? = null,
    /** Forecasts at the location's other elevations (ski areas), when asked for. */
    @SerialName("elevation_forecasts") public val elevationForecasts: List<ElevationForecast>? = null,
    /** The live nowcast sources this body was built without. */
    @SerialName("nowcast_unavailable") public val nowcastUnavailable: List<NowcastSource>? = null,
    /** Which member variables `members` carries, when members were asked for. */
    @SerialName("members_view") public val membersView: MembersView? = null,
    /** Columbia Gorge gap flow, for locations in the Gorge. */
    @SerialName("gap_flow") public val gapFlow: GapFlow? = null,
    /** Nearby observation sensors, ranked by an elevation-weighted distance. */
    @SerialName("nearest_sensors") public val nearestSensors: List<NearestSensor>? = null,
    /** Where a coordinate forecast was computed (point forecasts only). */
    public val point: PointInfo? = null,
    /** "Right now", read off the served hour nearest the request (nowcast layer only). */
    public val current: CurrentConditions? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** The composition the forecast was blended from. */
@Serializable
public data class ModelRunInfo(
    /** The reference init (ISO 8601): the newest contributing model's cycle. */
    @SerialName("initialized_at") public val initializedAt: String,
    /** How many members the blend carries. */
    @SerialName("total_members") public val totalMembers: Int,
    /** One entry per contributing model. */
    public val models: List<ModelEntry>,
)

/** The forecast body: hourly series, daily summaries and snow outlook. */
@Serializable
public data class ForecastData(
    /** The last served hour, in hours from the first served one. */
    @SerialName("max_hours") public val maxHours: Int,
    /** Hours between points. */
    @SerialName("interval_hours") public val intervalHours: Int,
    /**
     * The hourly points, ascending by valid time, starting at the current hour. Not
     * guaranteed contiguous: key on [TimeSeriesPoint.validTime], not on position.
     */
    public val timeseries: List<TimeSeriesPoint>? = null,
    /** One row per local calendar date. */
    public val daily: List<DailyForecast>? = null,
    /** Rolling snow totals for common windows (present exactly when [daily] is). */
    @SerialName("snow_outlook") public val snowOutlook: SnowOutlook? = null,
)

/**
 * Snow accumulation for common windows, in the request's units. A window the
 * forecast does not reach is `null`, never a shorter sum.
 */
@Serializable
public data class SnowOutlook(
    /** The next 24 hours. */
    @SerialName("next_24h") public val next24h: Double? = null,
    /** The next 48 hours. */
    @SerialName("next_48h") public val next48h: Double? = null,
    /** The next 7 days. */
    @SerialName("next_7d") public val next7d: Double? = null,
    /** Local 16:00 today to 09:00 tomorrow. */
    @SerialName("overnight_4pm_9am") public val overnight4pm9am: Double? = null,
    /** The range of each window's total, from each member's own total. */
    public val quantiles: SnowOutlookQuantiles? = null,
)

/** The range of each snow-outlook window's total; a window is `null` when not covered. */
@Serializable
public data class SnowOutlookQuantiles(
    /** The next 24 hours. */
    @SerialName("next_24h") public val next24h: Quantiles? = null,
    /** The next 48 hours. */
    @SerialName("next_48h") public val next48h: Quantiles? = null,
    /** The next 7 days. */
    @SerialName("next_7d") public val next7d: Quantiles? = null,
)

/** One forecast hour. */
@Serializable
public data class TimeSeriesPoint(
    /** The hour's valid time (ISO 8601, UTC). */
    @SerialName("valid_time") public val validTime: String,
    /** Hours from [ModelRunInfo.initializedAt]. */
    @SerialName("lead_time_hours") public val leadTimeHours: Int,
    /** Whether the hour is nighttime at the location. */
    @SerialName("is_night") public val isNight: Boolean,
    /** The condition code. */
    public val condition: Condition,
    /** The condition's display text (`Partly Cloudy`). */
    @SerialName("condition_text") public val conditionText: String,
    /** The icon hint. */
    @SerialName("condition_icon") public val conditionIcon: ConditionIcon,
    /** The hour's distributions. */
    public val ensemble: EnsembleStats,
    /** Whether the gust is worth showing beside the wind. */
    @SerialName("gust_notable") public val gustNotable: Boolean? = null,
    /** The precipitation type, when precipitation is expected. */
    @SerialName("precip_type") public val precipType: PrecipType? = null,
    /** A detected temperature inversion or cold pool. */
    public val inversion: InversionInfo? = null,
    /** The hour's freezing level. */
    @SerialName("freezing_level") public val freezingLevel: FreezingLevel? = null,
    /** Which post-processing shaped each variable (with `diagnostics = listOf("provenance")`). */
    public val provenance: HourProvenance? = null,
    /** Per-model statistics (with `models = true`, Pro). */
    @SerialName("model_means") public val modelMeans: List<ModelMean>? = null,
    /** Per-member values, by model (with `members = true`). */
    public val members: List<ModelMembers>? = null,
    /** The distribution of the members' running totals at this hour. */
    @SerialName("members_cumulative") public val membersCumulative: MembersCumulative? = null,
    /** The headline's running precipitation total, served (never compute it). */
    @SerialName("cumulative_precipitation") public val cumulativePrecipitation: Double? = null,
    /** The headline's running snowfall total, served. */
    @SerialName("cumulative_snowfall") public val cumulativeSnowfall: Double? = null,
)

/** One hour's distributions; every one is in the request's units. */
@Serializable
public data class EnsembleStats(
    /** Air temperature. */
    public val temperature: Quantiles,
    /** Precipitation in the hour, with its occurrence [Quantiles.probability]. */
    public val precipitation: Quantiles,
    /** Sustained wind speed. */
    @SerialName("wind_speed") public val windSpeed: Quantiles,
    /** Wind direction, degrees: an arc, see [Quantiles] and [windDirectionCircular]. */
    @SerialName("wind_direction") public val windDirection: Quantiles,
    /** Dewpoint. */
    public val dewpoint: Quantiles? = null,
    /** Snow level. */
    @SerialName("snow_level") public val snowLevel: Quantiles? = null,
    /** NWS apparent temperature. */
    @SerialName("feels_like") public val feelsLike: Quantiles? = null,
    /** UTCI (not served yet). */
    @SerialName("feels_like_utci") public val feelsLikeUtci: Quantiles? = null,
    /** Peak gust. */
    @SerialName("wind_gust") public val windGust: Quantiles? = null,
    /** Wind direction as a circular distribution. */
    @SerialName("wind_direction_circular") public val windDirectionCircular: CircularStats? = null,
    /** Snow-to-liquid ratio. */
    public val slr: Quantiles? = null,
    /** Snowfall in the hour, with its occurrence [Quantiles.probability]; absent means unknown. */
    public val snowfall: Quantiles? = null,
    /** Cloud cover, percent. */
    @SerialName("cloud_cover") public val cloudCover: Quantiles? = null,
    /** Skin (ground surface) temperature. */
    @SerialName("skin_temperature") public val skinTemperature: Quantiles? = null,
    /** Model-native precipitation-type code (with `diagnostics = listOf("ptype")`). */
    public val ptype: Quantiles? = null,
    /** Mean sea-level pressure (hPa, or inHg for imperial). */
    public val mslp: Quantiles? = null,
    /** Convective available potential energy, J/kg. */
    public val cape: Quantiles? = null,
    /** Relative humidity, percent. */
    @SerialName("relative_humidity") public val relativeHumidity: Quantiles? = null,
)

/** A clockwise arc on the compass, degrees; [to] may be less than [from] across north. */
@Serializable
public data class Sector(
    /** Where the arc starts. */
    public val from: Double,
    /** Where the arc ends, clockwise. */
    public val to: Double,
)

/** A bearing's distribution stated on the circle. */
@Serializable
public data class CircularStats(
    /** The resultant mean direction, degrees in [0, 360). */
    public val mean: Double,
    /** The central 50 % of the members, as an arc. */
    @SerialName("sector_50") public val sector50: Sector,
    /** The central 90 % of the members, as an arc. */
    @SerialName("sector_90") public val sector90: Sector,
    /** The members' angular standard deviation, degrees. */
    public val stddev: Double? = null,
)

/** The precipitation type of an hour. */
@Serializable
public data class PrecipType(
    /** The categorical type. */
    public val type: PrecipitationKind,
    /** The share of the hour's precipitation that falls as snow, 0..1. */
    @SerialName("snow_probability") public val snowProbability: Double,
    /** `1 - snowProbability`. */
    @SerialName("rain_probability") public val rainProbability: Double,
)

/** A surface-based cold pool or an elevated inversion. */
@Serializable
public data class InversionInfo(
    /** The kind of inversion. */
    public val kind: InversionKind,
    /** The layer's depth, in the request's length unit. */
    public val depth: Double,
    /** The temperature difference driving it, in the request's temperature unit. */
    public val strength: Double,
)

/** Which serve-path branch produced an hour: a diagnostic, not for display. */
@Serializable
public data class HourProvenance(
    /** The hour was interpolated between two native model steps. */
    public val densified: Boolean? = null,
    /** Variables corrected by a fitted model. */
    public val ngr: List<String>? = null,
    /** Variables served through the fallback widening. */
    public val fallback: List<String>? = null,
    /** Variables served from borrowed fits. */
    public val borrowed: List<String>? = null,
    /** Variables whose ladder was rebuilt at the spread floor. */
    @SerialName("spread_floored") public val spreadFloored: List<String>? = null,
    /** Variables truncated by the zero floor. */
    @SerialName("floor_clamped") public val floorClamped: List<String>? = null,
    /** Accumulations split evenly across hours. */
    @SerialName("bucket_split") public val bucketSplit: List<String>? = null,
    /** Variables with a two-parameter contributor. */
    @SerialName("distribution_parameters") public val distributionParameters: List<String>? = null,
    /** Variables whose spread was interpolated from bracketing hours. */
    @SerialName("spread_interpolated") public val spreadInterpolated: List<String>? = null,
    /** Variables with a contributor interpolated across leads. */
    @SerialName("interpolated_contributor") public val interpolatedContributor: List<String>? = null,
    /** Variables whose probability came from a moment-matched family. */
    @SerialName("moment_matched_family") public val momentMatchedFamily: List<String>? = null,
    /** Variables carried by the hour's own lapse rate. */
    @SerialName("lapse_profile") public val lapseProfile: List<String>? = null,
    /** Variables carried by a climatological lapse rate. */
    @SerialName("lapse_climatology") public val lapseClimatology: List<String>? = null,
    /** Variables carried by the PRISM precipitation-elevation regression. */
    @SerialName("lapse_prism") public val lapsePrism: List<String>? = null,
    /** Variables whose fitted spread was raised to the band's low edge. */
    @SerialName("spread_clamped_low") public val spreadClampedLow: List<String>? = null,
    /** Variables whose fitted spread was cut to the band's high edge. */
    @SerialName("spread_clamped_high") public val spreadClampedHigh: List<String>? = null,
    /** Variables with a gust dressed from a mean wind. */
    @SerialName("gust_dressed") public val gustDressed: List<String>? = null,
)

/** One model's statistics at an hour (with `models = true`). */
@Serializable
public data class ModelMean(
    /** The model id. */
    public val id: String,
    /** Temperature; absent when the model published nothing for the hour. */
    public val temperature: Quantiles? = null,
    /** Dewpoint. */
    public val dewpoint: Quantiles? = null,
    /** Precipitation. */
    public val precipitation: Quantiles? = null,
    /** Wind speed. */
    @SerialName("wind_speed") public val windSpeed: Quantiles? = null,
    /** Wind direction (an arc). */
    @SerialName("wind_direction") public val windDirection: Quantiles? = null,
    /** Snow level. */
    @SerialName("snow_level") public val snowLevel: Quantiles? = null,
    /** Snow-to-liquid ratio. */
    public val slr: Quantiles? = null,
    /** Snowfall. */
    public val snowfall: Quantiles? = null,
    /** `true` when these statistics were interpolated between the model's native steps. */
    public val interpolated: Boolean? = null,
)

/** One model's members at an hour. */
@Serializable
public data class ModelMembers(
    /** The model id. */
    public val id: String,
    /** Its members. */
    public val members: List<MemberForecast>,
)

/** One ensemble member at one hour; `null` means the member published nothing. */
@Serializable
public data class MemberForecast(
    /** The member's position on its model's ensemble axis, from 0. */
    @SerialName("member_id") public val memberId: Int,
    /** Temperature. */
    public val temperature: Double? = null,
    /** The total over the [precipitationHours] ending at this hour. */
    public val precipitation: Double? = null,
    /** How many hours [precipitation] and [snowfall] cover. */
    @SerialName("precipitation_hours") public val precipitationHours: Int? = null,
    /** Wind speed. */
    @SerialName("wind_speed") public val windSpeed: Double? = null,
    /** Wind direction. */
    @SerialName("wind_direction") public val windDirection: Double? = null,
    /** Snow level. */
    @SerialName("snow_level") public val snowLevel: Double? = null,
    /** Snow-to-liquid ratio. */
    public val slr: Double? = null,
    /** Snowfall over the [precipitationHours]. */
    public val snowfall: Double? = null,
    /** This member's running precipitation total over the served window. */
    @SerialName("cumulative_precipitation") public val cumulativePrecipitation: Double? = null,
    /** This member's running snowfall total over the served window. */
    @SerialName("cumulative_snowfall") public val cumulativeSnowfall: Double? = null,
)

/** The distribution of the members' running totals at an hour. */
@Serializable
public data class MembersCumulative(
    /** Running precipitation totals. */
    public val precipitation: Quantiles? = null,
    /** Running snowfall totals. */
    public val snowfall: Quantiles? = null,
)

/** A day's summary over one local calendar date. */
@Serializable
public data class DailyForecast(
    /** The local date (`yyyy-mm-dd`). */
    public val date: String,
    /** The day's condition code. */
    public val condition: Condition,
    /** The display text. */
    @SerialName("condition_text") public val conditionText: String,
    /** The icon hint. */
    @SerialName("condition_icon") public val conditionIcon: ConditionIcon,
    /** The calendar-day maximum temperature. */
    public val high: Quantiles,
    /** The calendar-day minimum temperature. */
    public val low: Quantiles,
    /** The day's precipitation total: each member's own day total. */
    @SerialName("total_precip") public val totalPrecip: Quantiles,
    /** P(day total ≥ 2.5 mm). */
    @SerialName("precip_probability") public val precipProbability: Double,
    /** P(day total ≥ 1.0 mm): the chance it precipitates at all. */
    @SerialName("precip_occurrence_probability") public val precipOccurrenceProbability: Double,
    /** P(day snowfall ≥ 2.5 mm). */
    @SerialName("snow_probability") public val snowProbability: Double,
    /** The day's precipitation headline, chosen by the server. */
    @SerialName("precip_display") public val precipDisplay: PrecipDisplay,
    /** Sun and moon. */
    public val astro: DailyAstro,
    /** The mean snow level. */
    @SerialName("snow_level_mean") public val snowLevelMean: Double? = null,
    /** The day's precipitation type; `null` on a dry day. */
    @SerialName("precip_type") public val precipType: PrecipitationKind? = null,
    /** The day's maximum feels-like temperature. */
    @SerialName("feels_like_high") public val feelsLikeHigh: Quantiles? = null,
    /** The day's minimum feels-like temperature. */
    @SerialName("feels_like_low") public val feelsLikeLow: Quantiles? = null,
    /** The day's maximum UTCI (not served yet). */
    @SerialName("feels_like_utci_high") public val feelsLikeUtciHigh: Quantiles? = null,
    /** The day's minimum UTCI (not served yet). */
    @SerialName("feels_like_utci_low") public val feelsLikeUtciLow: Quantiles? = null,
    /** The day's snowfall total: each member's own day total. */
    @SerialName("total_snowfall") public val totalSnowfall: Quantiles? = null,
    /** The day's snow level. */
    @SerialName("snow_level") public val snowLevel: Quantiles? = null,
    /** The day total given a wet day. */
    @SerialName("total_precip_if_wet") public val totalPrecipIfWet: Quantiles? = null,
    /** The day's snowfall given a snowy day. */
    @SerialName("total_snowfall_if_wet") public val totalSnowfallIfWet: Quantiles? = null,
    /** The mean cloud cover, percent. */
    @SerialName("cloud_cover_mean") public val cloudCoverMean: Double? = null,
    /** The day's wind summary. */
    public val wind: DailyWind? = null,
    /** How snow density changes over a storm day. */
    @SerialName("storm_orientation") public val stormOrientation: StormOrientation? = null,
    /** A display sentence for [stormOrientation]. */
    @SerialName("storm_orientation_description") public val stormOrientationDescription: String? = null,
    /** The seasonal temperature normal for the date. */
    @SerialName("temperature_climatology") public val temperatureClimatology: DayClimatology? = null,
)

/** A day's precipitation headline. */
@Serializable
public data class PrecipDisplay(
    /** [PrecipitationKind.None] under a 15 % chance of a wet day, else the day's type. */
    public val kind: PrecipitationKind,
    /** The chance of a wet day, 0..1. */
    public val chance: Double,
    /** The median amount if wet. */
    public val amount: Double? = null,
)

/** A day's wind summary. */
@Serializable
public data class DailyWind(
    /** The severity class. */
    public val status: WindStatus? = null,
    /** The expected peak wind. */
    @SerialName("peak_speed") public val peakSpeed: Double? = null,
    /** The "gusts could reach" upper band. */
    @SerialName("peak_speed_p90") public val peakSpeedP90: Double? = null,
    /** Whether [peakSpeed] is a gust. */
    @SerialName("is_gust") public val isGust: Boolean? = null,
    /** The peak sustained wind. */
    @SerialName("sustained_speed") public val sustainedSpeed: Double? = null,
)

/** The day's sun and moon. */
@Serializable
public data class DailyAstro(
    /** The moon phase. */
    @SerialName("moon_phase") public val moonPhase: MoonPhase,
    /** The illuminated fraction of the moon, 0..1. */
    @SerialName("moon_illumination") public val moonIllumination: Double,
    /** The astronomical season. */
    @SerialName("astronomical_season") public val astronomicalSeason: Season,
    /** Sunrise (ISO 8601, UTC). */
    public val sunrise: String? = null,
    /** Sunset (ISO 8601, UTC). */
    public val sunset: String? = null,
)

/** The seasonal temperature normal for a date. */
@Serializable
public data class DayClimatology(
    /** The all-hours seasonal mean temperature. */
    public val mean: Double,
    /** The seasonal standard deviation (including the diurnal range). */
    public val std: Double,
)

/** The thresholds behind each served built-in probability, in the request's units. */
@Serializable
public data class ProbabilityThresholds(
    /** The hourly precipitation occurrence threshold. */
    @SerialName("hourly_precipitation") public val hourlyPrecipitation: Double,
    /** The hourly snowfall occurrence threshold. */
    @SerialName("hourly_snowfall") public val hourlySnowfall: Double,
    /** `daily[].precip_probability`'s threshold. */
    @SerialName("daily_precip_probability") public val dailyPrecipProbability: Double,
    /** `daily[].precip_occurrence_probability`'s threshold. */
    @SerialName("daily_precip_occurrence_probability") public val dailyPrecipOccurrenceProbability: Double,
    /** `daily[].snow_probability`'s threshold. */
    @SerialName("daily_snow_probability") public val dailySnowProbability: Double,
)

/** The options the server applied, after its tier clamps. */
@Serializable
public data class AppliedOptions(
    /** Hours served past the first; `null` with `hourly = false`. */
    @SerialName("max_hours") public val maxHours: Int? = null,
    /** Daily rows served for; `null` with `daily = false`. */
    @SerialName("max_days") public val maxDays: Int? = null,
    /** The requested options the server changed (`max_hours`, `max_days`). */
    public val clamped: List<String> = emptyList(),
)

/**
 * One probability term, answered by the server from the full ensemble, with its
 * reference class: what was asked ([variable], [op], [threshold], the window) and
 * how many [members] voted.
 */
@Serializable
public data class ProbabilityResult(
    /** The variable. */
    public val variable: Variable,
    /** The comparison. */
    public val op: Comparison,
    /** The threshold, in the request's units at the variable's served precision. */
    public val threshold: Double,
    /** The window's first hour, from the first served hour. */
    @SerialName("window_start_hours") public val windowStartHours: Int,
    /** The window's end hour (exclusive). */
    @SerialName("window_end_hours") public val windowEndHours: Int,
    /** How the window was read. */
    public val aggregation: Aggregation,
    /** The window's first instant (ISO 8601). */
    public val from: String? = null,
    /** The window's end (ISO 8601, exclusive). */
    public val to: String? = null,
    /**
     * The share of members meeting the term, 0..1. `null` (absent, never 0) when the
     * forecast does not reach the window's end.
     */
    public val probability: Double? = null,
    /** How many members voted. */
    public val members: Int,
)

/** One window total, answered by the server: the distribution of each member's own total. */
@Serializable
public data class WindowTotal(
    /** [Variable.Precipitation] or [Variable.Snowfall]. */
    public val variable: Variable,
    /** The window's first hour. */
    @SerialName("window_start_hours") public val windowStartHours: Int,
    /** The window's end hour (exclusive). */
    @SerialName("window_end_hours") public val windowEndHours: Int,
    /** The window's first instant (ISO 8601). */
    public val from: String? = null,
    /** The window's end (ISO 8601, exclusive). */
    public val to: String? = null,
    /** The members' window totals; `null` (absent) when the window is not covered. */
    public val quantiles: Quantiles? = null,
    /** How many members cover the window. */
    public val members: Int,
)

/** A forecast at one of a location's additional elevations. */
@Serializable
public data class ElevationForecast(
    /** The elevation's name. */
    public val name: String,
    /** The elevation, in the request's length unit. */
    public val elevation: Double,
    /** Where it was forecast. */
    public val lat: Double,
    /** Where it was forecast. */
    public val lon: Double,
    /** Whether this is the default elevation to show. */
    @SerialName("default") public val isDefault: Boolean? = null,
    /** Legacy compact hourly points; prefer [hourly]. */
    public val timeseries: List<ElevationTimeSeriesPoint>? = null,
    /** The full hourly series at this elevation. */
    public val hourly: List<TimeSeriesPoint>? = null,
    /** Daily summaries at this elevation. */
    public val daily: List<DailyForecast>? = null,
    /** Rolling snow totals at this elevation. */
    @SerialName("snow_outlook") public val snowOutlook: SnowOutlook? = null,
)

/** A legacy compact hourly point of an [ElevationForecast]. */
@Serializable
public data class ElevationTimeSeriesPoint(
    /** The valid time (ISO 8601). */
    @SerialName("valid_time") public val validTime: String,
    /** Hours from the reference init. */
    @SerialName("lead_time_hours") public val leadTimeHours: Int,
    /** Temperature. */
    public val temperature: Quantiles,
    /** Precipitation. */
    public val precipitation: Quantiles,
    /** The condition code. */
    public val condition: Condition,
    /** The display text. */
    @SerialName("condition_text") public val conditionText: String,
    /** The icon hint. */
    @SerialName("condition_icon") public val conditionIcon: ConditionIcon,
    /** Elevation minus snow level. */
    @SerialName("snow_level_delta") public val snowLevelDelta: Quantiles? = null,
    /** Wind speed. */
    @SerialName("wind_speed") public val windSpeed: Quantiles? = null,
    /** Gust. */
    @SerialName("wind_gust") public val windGust: Quantiles? = null,
)

/** Columbia Gorge gap flow at the first served hour. */
@Serializable
public data class GapFlow(
    /** The strength class. */
    public val status: GapFlowStatus? = null,
    /** The pressure difference across the Gorge, hPa. */
    @SerialName("pressure_diff_hpa") public val pressureDiffHpa: Double? = null,
    /** The wind multiplier applied. */
    @SerialName("wind_multiplier") public val windMultiplier: Double? = null,
    /** The flow direction. */
    public val direction: String? = null,
)

/** A nearby observation sensor. */
@Serializable
public data class NearestSensor(
    /** The station's id in its network. */
    @SerialName("station_id") public val stationId: String,
    /** The station's name. */
    public val name: String,
    /** The observation network. */
    public val source: String,
    /** Straight-line distance, km. */
    @SerialName("distance_km") public val distanceKm: Double,
    /** Sensor elevation minus the forecast point's, in the request's length unit. */
    @SerialName("elevation_diff") public val elevationDiff: Double,
    /** The elevation-weighted distance used for ranking, km. */
    @SerialName("effective_distance_km") public val effectiveDistanceKm: Double,
    /** When it last reported (ISO 8601). */
    @SerialName("observation_time") public val observationTime: String,
)

/** Where a coordinate forecast was computed. */
@Serializable
public data class PointInfo(
    /** Latitude as sent. */
    @SerialName("requested_lat") public val requestedLat: Double,
    /** Longitude as sent. */
    @SerialName("requested_lon") public val requestedLon: Double,
    /** The quantized latitude. */
    public val lat: Double,
    /** The quantized longitude. */
    public val lon: Double,
    /** The quantization step, degrees. */
    @SerialName("resolution_deg") public val resolutionDeg: Double,
    /** The elevation the forecast was computed at, in the request's length unit. */
    public val elevation: Double,
    /** Where that elevation came from. */
    @SerialName("elevation_source") public val elevationSource: ElevationSource,
    /** The USPS code of the point's state. */
    public val state: String,
)

/** "Right now": a view of one served hour, never a second forecast. */
@Serializable
public data class CurrentConditions(
    /** The valid time of the hour it is read from. */
    public val time: String,
    /** Temperature (the hour's mean). */
    public val temperature: Double,
    /** The condition code. */
    public val condition: Condition,
    /** The display text. */
    @SerialName("condition_text") public val conditionText: String,
    /** The icon hint. */
    @SerialName("condition_icon") public val conditionIcon: ConditionIcon,
    /** Wind speed. */
    @SerialName("wind_speed") public val windSpeed: Double,
    /** Wind direction, degrees, where the wind blows from. */
    @SerialName("wind_direction") public val windDirection: Double,
    /** Whether a live observation carried weight. */
    public val source: ConditionsSource,
    /** Feels-like temperature. */
    @SerialName("feels_like") public val feelsLike: Double? = null,
    /** Gust. */
    @SerialName("wind_gust") public val windGust: Double? = null,
    /** Relative humidity, percent. */
    @SerialName("relative_humidity") public val relativeHumidity: Double? = null,
    /** Dewpoint. */
    public val dewpoint: Double? = null,
    /** The chance of at least a trace in the hour, 0..1. */
    @SerialName("precip_probability") public val precipProbability: Double? = null,
    /** The station whose reading anchored the hour. */
    public val station: CurrentStation? = null,
)

/** The surface station that anchored a "right now" block. */
@Serializable
public data class CurrentStation(
    /** The station's name. */
    public val name: String,
    /** When it reported (ISO 8601). */
    @SerialName("observation_time") public val observationTime: String,
)
