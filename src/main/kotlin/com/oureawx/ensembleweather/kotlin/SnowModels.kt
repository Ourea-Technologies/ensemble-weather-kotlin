// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Observed season-to-date precipitation at a location. */
@Serializable
public data class SeasonResponse(
    /** The location. */
    public val location: LocationRef,
    /** The water year by its ending year. */
    @SerialName("water_year") public val waterYear: Int,
    /** The first local day of the water year. */
    public val starts: String,
    /** The last local day with an observed row (the day before `starts` when there is none). */
    public val through: String,
    /** When the newest day row was computed; null with no rows. */
    @SerialName("as_of") public val asOf: String? = null,
    /** The units. */
    public val units: Units,
    /** Null until the location has an observed day. */
    public val precipitation: SeasonTotal? = null,
    /** Always null for now; season snowfall is not served yet. */
    public val snowfall: SeasonTotal? = null,
    /** The nearest SNOTEL or SCAN gauge within 25 km and 300 m with rows this water year, or null. */
    public val station: SeasonStation? = null,
    /** Only with `include=daily`. */
    public val daily: List<SeasonDay>? = null,
    /** The attribution. */
    public val attribution: ResponseAttribution,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** The `SeasonTotal` shape. */
@Serializable
public data class SeasonTotal(
    /** (JSON `total`) Sum over the covered days, in `unit`. */
    @SerialName("total") public val amount: Double,
    /** The unit. One of `mm`, `cm`, `in`. */
    public val unit: String,
    /**
     * `mrms_multisensor`: the gridded analysis at the location; `station_gauge`: the location is a gauge site and
     * this is the gauge's accumulator. One of `mrms_multisensor`, `nohrsc_nsa`, `station_gauge`.
     */
    public val source: String,
    /** The days covered. */
    @SerialName("days_covered") public val daysCovered: Int,
    /** Days since `starts` with no complete observation; not counted as dry. */
    @SerialName("days_missing") public val daysMissing: Int,
    /** Covered days still inside the 7-day restate window. */
    @SerialName("provisional_days") public val provisionalDays: Int,
    /** PRISM 1991-2020 over the covered days, in `unit`; null where no normal is loaded. */
    @SerialName("normal_to_date") public val normalToDate: Double? = null,
    /** The percent of normal. */
    @SerialName("percent_of_normal") public val percentOfNormal: Int? = null,
    /** The normal source. One of `prism_1991_2020`. */
    @SerialName("normal_source") public val normalSource: String? = null,
)

/** The `SeasonStation` shape. */
@Serializable
public data class SeasonStation(
    /** The NRCS station triplet. */
    public val id: String,
    /** The network. One of `snotel`, `scan`. */
    public val network: String,
    /** The name. */
    public val name: String,
    /** The distance, km. */
    @SerialName("distance_km") public val distanceKm: Double,
    /** The elevation, metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The gauge's water-year accumulator on `through` (mm or in). */
    @SerialName("precipitation_total") public val precipitationTotal: Double? = null,
    /** The precipitation percent of median. */
    @SerialName("precipitation_percent_of_median") public val precipitationPercentOfMedian: Int? = null,
    /** Snow water equivalent on `through` (mm or in). */
    public val swe: Double? = null,
    /** The swe percent of median. */
    @SerialName("swe_percent_of_median") public val swePercentOfMedian: Int? = null,
    /** Snow depth on `through` (cm or in). */
    @SerialName("snow_depth") public val snowDepth: Double? = null,
    /** The gauge's newest day. */
    public val through: String,
)

/** The `SeasonDay` shape. */
@Serializable
public data class SeasonDay(
    /** The date. */
    public val date: String,
    /** The day's total; null when an hour was not observed. */
    public val precipitation: Double? = null,
    /** Always null for now. */
    public val snowfall: Double? = null,
    /** The provisional. */
    public val provisional: Boolean,
)

/** Each recent run's daily snowfall at a location's point, by source. */
@Serializable
public data class SnowTrendResponse(
    /** The location. */
    public val location: LocationRef,
    /** The units. */
    public val units: Units,
    /** The point the grids are for. */
    public val elevation: SnowTrendElevation,
    /** How many days back runs are kept. */
    @SerialName("retention_days") public val retentionDays: Int,
    /** The columns, location-local dates ascending. */
    public val dates: List<String>,
    /** `ourea` first; with the `model_data` entitlement then `aifs`, `aigefs`, `gefs`, `ifs_ens` and `rrfs`. */
    public val grids: List<SnowTrendGrid>,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** The point the grids are for. */
@Serializable
public data class SnowTrendElevation(
    /** The named elevation; `null` for a location with one point. */
    public val name: String? = null,
    /** What that elevation is called, as `ElevationSummary.display_name`; absent where it has no name. */
    @SerialName("display_name") public val displayName: String? = null,
    /** (JSON `elevation`) Its elevation, in m (metric) or ft (imperial). */
    @SerialName("elevation") public val height: Double,
)

/** The `SnowTrendGrid` shape. */
@Serializable
public data class SnowTrendGrid(
    /** `ourea`, or a model id. */
    public val source: String,
    /** Oldest first; the last is the newest. */
    public val runs: List<SnowTrendRun>,
)

/** The `SnowTrendRun` shape. */
@Serializable
public data class SnowTrendRun(
    /**
     * `true`: re-rendered after the fact from the pinned run (`?run=<initialized_at>`) by the post-deploy backfill
     * -- today's calibration and no live nowcast, so it can differ slightly from what was served then.
     */
    public val backfilled: Boolean,
    /** `ourea`: the served body's `model_run.initialized_at`. */
    @SerialName("initialized_at") public val initializedAt: String,
    /** One entry per `dates` entry, in order; `null` where the run has no value. */
    public val days: List<SnowTrendDay?>,
)

/** The `SnowTrendDay` shape. */
@Serializable
public data class SnowTrendDay(
    /** The `dates` entry this day is for. */
    public val date: String,
    /** (JSON `snow`) The day's snowfall in mm (metric) or inches (imperial), at the served amount precision. */
    @SerialName("snow") public val amount: Double? = null,
    /** The run covers only part of this local day. */
    public val partial: Boolean,
    /** The range's low end. */
    public val p10: Double? = null,
    /** The range's high end, as `p10`. */
    public val p90: Double? = null,
    /** `ourea` only: the day's `precipitation_display.kind`. */
    public val kind: PrecipitationKind? = null,
)

/** The snowpack forecast of a location's spots. */
@Serializable
public data class SnowpackResponse(
    /** Always `true` while the product is a beta; fields may be added. */
    public val beta: Boolean,
    /** The location. */
    public val location: LocationRef,
    /** The units. */
    public val units: Units,
    /** The model run. */
    @SerialName("model_run") public val modelRun: SnowpackRun? = null,
    /** The newest run's blend cycle is more than 24 h old. */
    public val stale: Boolean,
    /** The location's own point first, then its named elevations in their display order. */
    public val spots: List<SnowpackSpot>,
    /** The definitions. */
    public val definitions: SnowpackDefinitions,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** The `SnowpackDefinitions` shape. */
@Serializable
public data class SnowpackDefinitions(
    /** Each surface class and what it means. */
    public val surface: Map<String, String>,
    /** Each layer form and what it is. */
    @SerialName("layer_form") public val layerForm: Map<String, String>,
)

/** The newest snowpack run among the spots served. */
@Serializable
public data class SnowpackRun(
    /** The blend cycle the snowpack model was driven with. */
    @SerialName("initialized_at") public val initializedAt: String,
    /** The generated at. */
    @SerialName("generated_at") public val generatedAt: String,
    /** The model. */
    public val model: SnowpackModel,
)

/** The `SnowpackModel` shape. */
@Serializable
public data class SnowpackModel(
    /** The name. */
    public val name: String,
    /** The by. */
    public val by: String,
    /** The license. */
    public val license: String,
    /**
     * The build that ran: the commits of Ourea's SNOWPACK and MeteoIO source it was built from, as `snowpack
     * <commit> meteoio <commit>`.
     */
    public val version: String? = null,
)

/** The `SnowpackSpot` shape. */
@Serializable
public data class SnowpackSpot(
    /** The named elevation (`location.elevations[].name`); `null` is the location's own point. */
    public val name: String? = null,
    /**
     * What the named elevation is called, as `ElevationSummary.display_name`; absent for the location's own point
     * and where the catalog has no name for the elevation.
     */
    @SerialName("display_name") public val displayName: String? = null,
    /** In the response's units (m or ft). */
    public val elevation: Double,
    /** The elevation, metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /**
     * The blend cycle this spot's forecast was driven with; older than `model_run.initialized_at` when the newest
     * run did not reach the spot.
     */
    @SerialName("initialized_at") public val initializedAt: String,
    /** The snow-depth sensor at the spot. */
    public val anchor: SnowpackAnchor? = null,
    /** The depth. */
    public val depth: SnowpackDepth,
    /**
     * The depth at `depth.start_time` less this spot's modelled depth 24 h before, in the depth's units and
     * rounding (cm or in; a loss is negative): the model's own change, snowfall, settlement and melt together.
     */
    @SerialName("depth_change_24h") public val depthChange24h: Double? = null,
    /** Hourly snow water equivalent (mm or in), parallel to `depth.p50`. */
    public val swe: List<Double>,
    /** One entry every six hours. */
    public val steps: List<SnowpackStep>,
    /** Buried layers at `depth.start_time`, at most three. */
    public val layers: List<SnowpackLayer>,
    /** The snow on a 38° slope at the spot facing each way, in the order north, east, south, west. */
    public val aspects: List<SnowpackAspect>? = null,
)

/** The snow-depth sensor at the spot. */
@Serializable
public data class SnowpackAnchor(
    /** The station's catalog slug. */
    public val station: String,
    /** The name. */
    public val name: String,
    /** The network. */
    public val network: String,
)

/** The `SnowpackStep` shape. */
@Serializable
public data class SnowpackStep(
    /** The valid time. */
    @SerialName("valid_time") public val validTime: String,
    /**
     * A key of `definitions.surface`: today `none`, `powder`, `wind_affected`, `settled`, `wet`, `crust` or
     * `ice_crust`.
     */
    public val surface: String,
    /** New snow of the 24 h to `valid_time`, as it lies (cm or in). */
    @SerialName("new_snow_24h") public val newSnow24h: Double,
    /** Its density. */
    @SerialName("new_snow_density_kg_m3") public val newSnowDensityKgM3: Double? = null,
    /** How far the snow on the ground 24 h earlier settled (cm or in). */
    @SerialName("settlement_24h") public val settlement24h: Double? = null,
)

/** The `SnowpackLayer` shape. */
@Serializable
public data class SnowpackLayer(
    /**
     * A key of `definitions.layer_form`: today `faceted_grains`, `depth_hoar`, `buried_surface_hoar`,
     * `melt_freeze_crust`, `ice_layer` or `rounding_facets`.
     */
    public val form: String,
    /** Cm or in. */
    public val thickness: Double,
    /** From the surface down to the layer's top (cm or in). */
    public val depth: Double,
    /** The formed. */
    public val formed: String,
)

/**
 * The snow on one slope at a spot, described as the spot's flat ground is: the model run on a slope of
 * `slope_angle_deg` facing `aspect`, with the flat ground's snowfall and the slope's own sun.
 */
@Serializable
public data class SnowpackAspect(
    /** The way the slope faces: today `north`, `east`, `south` or `west`. */
    public val aspect: String,
    /** How steep the slope is, degrees from horizontal. */
    @SerialName("slope_angle_deg") public val slopeAngleDeg: Double,
    /** Hourly snow depth on the slope, measured vertically as the flat depth is. */
    public val depth: SnowpackDepth,
    /**
     * As the spot's `depth_change_24h`, this slope's own (open loop on both sides), read from the same earlier run
     * as the spot's and from no other.
     */
    @SerialName("depth_change_24h") public val depthChange24h: Double? = null,
    /** One entry every six hours, at the spot's own `steps[].valid_time`. */
    public val steps: List<SnowpackStep>,
    /** Buried layers on the slope at `depth.start_time`, at most three. */
    public val layers: List<SnowpackLayer>,
)

/** Hourly snow depth from `start_time` (cm or in). */
@Serializable
public data class SnowpackDepth(
    /** The start time. */
    @SerialName("start_time") public val startTime: String,
    /** The interval, hours. */
    @SerialName("interval_hours") public val intervalHours: Int,
    /** `measured`: started from a sensor reading at the spot. */
    public val basis: String,
    /** The p10. */
    public val p10: List<Double>? = null,
    /** The p50. */
    public val p50: List<Double>,
    /** The p90. */
    public val p90: List<Double>? = null,
)

/** A location's snow-depth plume. */
@Serializable
public data class SnowpackPlumeResponse(
    /** Always `true` while the product is a beta; fields may be added. */
    public val beta: Boolean,
    /** The location. */
    public val location: LocationRef,
    /** The units. */
    public val units: Units,
    /** The run the plume is from: the 12Z blend cycle the members were read from. */
    @SerialName("model_run") public val modelRun: SnowpackRun? = null,
    /** The plume's blend cycle is more than 24 h old. */
    public val stale: Boolean,
    /** The members' depth at each step. */
    public val depth: SnowpackPlumeDepth? = null,
    /** Every member's own series, by model, each model to its own horizon. */
    public val models: List<SnowpackPlumeModel>? = null,
    /**
     * Every member's depth as ONE unlabelled pool, for Free and anonymous callers (absent for Plus and Pro, who are
     * served `models`).
     */
    @SerialName("member_depths") public val memberDepths: List<List<Double>>? = null,
    /** What the bands are, in a sentence a client may show beside them. */
    public val spread: String,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** The members' depth at each step. */
@Serializable
public data class SnowpackPlumeDepth(
    /** The time of the first value of every series in the body. */
    @SerialName("start_time") public val startTime: String,
    /** Hours between values (3). */
    @SerialName("interval_hours") public val intervalHours: Int,
    /** Members that reach each step; it falls where a model's horizon ends. */
    public val members: List<Int>,
    /** The members' median depth, in the response's units. */
    public val p50: List<Double>? = null,
    /** Served to every caller, as the bands are. */
    public val mean: List<Double>,
    /** Every caller (Plus and Pro only before). */
    public val p10: List<Double>,
    /** Every caller (Plus and Pro only before). */
    public val p25: List<Double>,
    /** Every caller (Plus and Pro only before). */
    public val p75: List<Double>,
    /** Every caller (Plus and Pro only before). */
    public val p90: List<Double>,
)

/** The `SnowpackPlumeModel` shape. */
@Serializable
public data class SnowpackPlumeModel(
    /** `aifs`, `aigefs`, `gefs` or `ifs_ens`; a client must tolerate one it does not know. */
    public val id: String,
    /** The model's own member numbers, in the order of `depth`. */
    @SerialName("member_ids") public val memberIds: List<Int>,
    /** One series per member, from `depth.start_time` every `depth.interval_hours`. */
    public val depth: List<List<Double>>,
    /**
     * The equal-weight mean of this model's members' `depth` at each of the model's own steps (from
     * `depth.start_time` every `depth.interval_hours`, to the model's horizon: the length of its members' series),
     * in the response's units.
     */
    @SerialName("depth_mean") public val depthMean: List<Double>,
    /** Members not run because the ground starts bare and they bring no snow; their series is 0 throughout. */
    @SerialName("members_skipped") public val membersSkipped: Int,
)
