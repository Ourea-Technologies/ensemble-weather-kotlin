// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One served series' members in columns (`members=plume`). */
@Serializable
public data class MemberPlume(
    /** The headline. */
    public val headline: PlumeHeadline,
    /** The bands. */
    public val bands: PlumeBands,
    /** One entry per model with members in the served window, in the order aifs, aigefs, gefs, ifs_ens, rrfs. */
    public val models: List<ModelColumns>,
    /** (JSON `members`) The members. */
    @SerialName("members") public val pool: PooledColumns? = null,
)

/**
 * The lines a plume draws over its members, at every served hour (`valid_time`): the blend's `temperature` and
 * `snow_level` means (`ensemble.*.mean`) and its running totals (`cumulative_precipitation`, `cumulative_snowfall`: the
 * "Ourea forecast" line), and that snowfall line at a 10:1 snow-to-liquid ...
 */
@Serializable
public data class PlumeHeadline(
    /** The valid time. */
    @SerialName("valid_time") public val validTime: List<String>,
    /** The temperature. */
    public val temperature: List<Double?>,
    /** The snow level. */
    @SerialName("snow_level") public val snowLevel: List<Double?>,
    /**
     * The blend's snow level on the NWS scale (`ensemble.snow_level_wet_bulb.mean`, the height of the 0.5 C
     * wet-bulb); `snow_level` is the rain line.
     */
    @SerialName("snow_level_wet_bulb") public val snowLevelWetBulb: List<Double?>,
    /** The cumulative precipitation. */
    @SerialName("cumulative_precipitation") public val cumulativePrecipitation: List<Double?>,
    /**
     * The hourly series' `cumulative_snowfall`: the running sum of the day cards' snowfall
     * (`daily[].precipitation_display.snow_amount`), spread inside each local date by the blend's hourly mean
     * snowfall, so at the end of each date served whole it equals the ...
     */
    @SerialName("cumulative_snowfall") public val cumulativeSnowfall: List<Double?>,
    /**
     * The headline's running snowfall at a fixed 10:1 snow-to-liquid ratio, in the snowfall units (in / mm), at the
     * amount precision: each hour's served snowfall with its SLR (`ensemble.slr.mean`) replaced by 10 -- the liquid
     * that falls as snow, times 10 (0.1 in of ...
     */
    @SerialName("cumulative_snowfall_10to1") public val cumulativeSnowfall10to1: List<Double?>,
    /**
     * The forecast's hourly wind speed, the hourly series' `ensemble.wind_speed.mean`, in the wind unit (mph / km/h
     * / m/s as `units` says) at the wind precision (0.1), at every served hour.
     */
    @SerialName("wind_speed") public val windSpeed: List<Double?>,
    /**
     * The forecast's snowfall in the 6 hours ending at each 00/06/12/18Z hour, in the snowfall units (in / mm) at
     * the amount precision: the rise of `cumulative_snowfall` over `(valid_time - 6 h, valid_time]`, computed here
     * so no client differences the line.
     */
    @SerialName("snowfall_6h") public val snowfall6h: List<Double?>,
)

/**
 * The members' distributions, at the served hours that carry at least one (`valid_time`; the synoptic hours in
 * practice): `cumulative_precipitation` / `cumulative_snowfall` are the row form's `members_cumulative` (the spread of
 * the members' running totals, never a sum of hourly percentiles), ...
 */
@Serializable
public data class PlumeBands(
    /** The valid time. */
    @SerialName("valid_time") public val validTime: List<String>,
    /** The cumulative precipitation. */
    @SerialName("cumulative_precipitation") public val cumulativePrecipitation: BandColumns,
    /** The cumulative snowfall. */
    @SerialName("cumulative_snowfall") public val cumulativeSnowfall: BandColumns,
    /**
     * The members' running snowfall at 10:1 -- each member's snowfall with its SLR replaced by 10 (its snowing
     * liquid x 10, every phase gate of its `snowfall` kept, the `snowfall_total_10to1` map's member rule), totalled
     * as `cumulative_snowfall` is -- by ...
     */
    @SerialName("cumulative_snowfall_10to1") public val cumulativeSnowfall10to1: BandColumns,
    /** The temperature. */
    public val temperature: BandColumns,
    /** The snow level. */
    @SerialName("snow_level") public val snowLevel: BandColumns,
    /**
     * The row form's `members_spread.snow_level_wet_bulb` -- the members' snow level on the NWS scale, at the hours
     * `snow_level` carries a band.
     */
    @SerialName("snow_level_wet_bulb") public val snowLevelWetBulb: BandColumns,
    /** The precipitation 6h. */
    @SerialName("precipitation_6h") public val precipitation6h: BandColumns,
    /** The snowfall 6h. */
    @SerialName("snowfall_6h") public val snowfall6h: BandColumns,
    /** The slr. */
    public val slr: BandColumns? = null,
    /** `members_spread.wind_speed`. */
    @SerialName("wind_speed") public val windSpeed: BandColumns? = null,
)

/**
 * One band's statistics (Hyndman-Fan type 7 over the served member values), each an array aligned to
 * `PlumeBands.valid_time`, `null` where the band is absent at that hour.
 */
@Serializable
public data class BandColumns(
    /** The mean. */
    public val mean: List<Double?>,
    /** The min. */
    public val min: List<Double?>,
    /** The p10. */
    public val p10: List<Double?>,
    /** The p25. */
    public val p25: List<Double?>,
    /** The p50. */
    public val p50: List<Double?>? = null,
    /** The p75. */
    public val p75: List<Double?>,
    /** The p90. */
    public val p90: List<Double?>,
    /** The max. */
    public val max: List<Double?>,
)

/**
 * The members as ONE unlabelled pool, for a caller without the plume's paid detail (Free and anonymous; `members_view:
 * pool`).
 */
@Serializable
public data class PooledColumns(
    /** The valid time. */
    @SerialName("valid_time") public val validTime: List<String>,
    /** The temperature. */
    public val temperature: List<List<Double?>>,
    /** The precipitation. */
    public val precipitation: List<List<Double?>>,
    /**
     * `[member][step]`: the hours each `precipitation` and `snowfall` value covers: the grid step (6), less where a
     * member's first value covers less; `null` where the value is absent.
     */
    @SerialName("precipitation_hours") public val precipitationHours: List<List<Int?>>,
    /** The cumulative precipitation. */
    @SerialName("cumulative_precipitation") public val cumulativePrecipitation: List<List<Double?>>,
    /** The snow level. */
    @SerialName("snow_level") public val snowLevel: List<List<Double?>>,
    /** The snow level wet bulb. */
    @SerialName("snow_level_wet_bulb") public val snowLevelWetBulb: List<List<Double?>>,
    /** The snowfall. */
    public val snowfall: List<List<Double?>>,
    /** The cumulative snowfall. */
    @SerialName("cumulative_snowfall") public val cumulativeSnowfall: List<List<Double?>>,
    /** The slr. */
    public val slr: List<List<Double?>>,
    /**
     * The rows (indices into every column) in `bands.cumulative_precipitation`'s fixed population, ascending: that
     * band's `mean` is the mean of these rows' running totals.
     */
    @SerialName("cumulative_precipitation_band_members") public val cumulativePrecipitationBandMembers: List<Int>,
    /** Likewise for `bands.cumulative_snowfall`. */
    @SerialName("cumulative_snowfall_band_members") public val cumulativeSnowfallBandMembers: List<Int>,
    /** Likewise for `bands.cumulative_snowfall_10to1`. */
    @SerialName("cumulative_snowfall_10to1_band_members") public val cumulativeSnowfall10to1BandMembers: List<Int>,
)

/**
 * One model's members over its NATIVE steps inside the served window (`valid_time`: the hours at which a member of the
 * model carries a value -- every hour for RRFS, every sixth for the global ensembles; never padded to the hourly
 * series).
 */
@Serializable
public data class ModelColumns(
    /** The id. */
    public val id: String,
    /** The valid time. */
    @SerialName("valid_time") public val validTime: List<String>,
    /** The members, ascending (`MemberForecast.member_id`). */
    @SerialName("member_ids") public val memberIds: List<Int>,
    /** The temperature. */
    public val temperature: List<List<Double?>>,
    /** The precipitation. */
    public val precipitation: List<List<Double?>>,
    /** `[member][step]`: the hours each `precipitation` value covers; `null` where it is absent or its span unknown. */
    @SerialName("precipitation_hours") public val precipitationHours: List<List<Int?>>,
    /** The cumulative precipitation. */
    @SerialName("cumulative_precipitation") public val cumulativePrecipitation: List<List<Double?>>,
    /** The snow level. */
    @SerialName("snow_level") public val snowLevel: List<List<Double?>>,
    /**
     * `[Member][step]`, each member's `MemberForecast.snow_level_wet_bulb` (the snow level on the NWS scale);
     * `null` exactly where its `snow_level` is.
     */
    @SerialName("snow_level_wet_bulb") public val snowLevelWetBulb: List<List<Double?>>,
    /** The snowfall. */
    public val snowfall: List<List<Double?>>,
    /** The cumulative snowfall. */
    @SerialName("cumulative_snowfall") public val cumulativeSnowfall: List<List<Double?>>,
    /**
     * One value per `valid_time` step (not per member): the median of the model's members' `cumulative_snowfall`
     * there (Hyndman-Fan type 7, as `bands`: the middle value, or the midpoint of the two middle ones; a
     * single-member model's own total), at the amount ...
     */
    @SerialName("cumulative_snowfall_p50") public val cumulativeSnowfallP50: List<Double?>,
    /**
     * As `cumulative_snowfall_p50`, over the members' running snowfall at 10:1 (see
     * `PlumeBands.cumulative_snowfall_10to1`).
     */
    @SerialName("cumulative_snowfall_10to1_p50") public val cumulativeSnowfall10to1P50: List<Double?>,
    /** The model's mean lines. */
    @SerialName("temperature_mean") public val temperatureMean: List<Double?>,
    /** The mean of `precipitation` (see `temperature_mean`). */
    @SerialName("precipitation_mean") public val precipitationMean: List<Double?>,
    /** The mean of `cumulative_precipitation` (see `temperature_mean`). */
    @SerialName("cumulative_precipitation_mean") public val cumulativePrecipitationMean: List<Double?>,
    /** The mean of `snow_level` (see `temperature_mean`). */
    @SerialName("snow_level_mean") public val snowLevelMean: List<Double?>,
    /** The mean of `snow_level_wet_bulb` (see `temperature_mean`). */
    @SerialName("snow_level_wet_bulb_mean") public val snowLevelWetBulbMean: List<Double?>,
    /** The mean of `snowfall` (see `temperature_mean`). */
    @SerialName("snowfall_mean") public val snowfallMean: List<Double?>,
    /** The mean of `cumulative_snowfall` (see `temperature_mean`), beside its median `cumulative_snowfall_p50`. */
    @SerialName("cumulative_snowfall_mean") public val cumulativeSnowfallMean: List<Double?>,
    /**
     * The mean of the members' running snowfall at 10:1, beside `cumulative_snowfall_10to1_p50` (the members' 10:1
     * column itself is not served).
     */
    @SerialName("cumulative_snowfall_10to1_mean") public val cumulativeSnowfall10to1Mean: List<Double?>,
    /** The mean of `wind_speed` (see `temperature_mean`). */
    @SerialName("wind_speed_mean") public val windSpeedMean: List<Double?>? = null,
    /** Which of this model's `member_ids` are in `bands.cumulative_precipitation`, ascending. */
    @SerialName("cumulative_precipitation_band_member_ids") public val cumulativePrecipitationBandMemberIds: List<Int>,
    /** As `cumulative_precipitation_band_member_ids`, for `bands.cumulative_snowfall`. */
    @SerialName("cumulative_snowfall_band_member_ids") public val cumulativeSnowfallBandMemberIds: List<Int>,
    /** As `cumulative_precipitation_band_member_ids`, for `bands.cumulative_snowfall_10to1`. */
    @SerialName("cumulative_snowfall_10to1_band_member_ids") public val cumulativeSnowfall10to1BandMemberIds: List<Int>,
    /** Pro (`members_view: full`) only; absent in the plume view. */
    @SerialName("wind_speed") public val windSpeed: List<List<Double?>>? = null,
    /** Pro only; absent in the plume view. */
    @SerialName("wind_direction") public val windDirection: List<List<Double?>>? = null,
    /** In both views. */
    public val slr: List<List<Double?>>? = null,
)
