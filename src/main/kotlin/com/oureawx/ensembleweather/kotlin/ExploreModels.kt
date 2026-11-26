// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** One page of a ranking. */
@Serializable
public data class ExploreResponse(
    /** The metric. */
    public val metric: ExploreMetric,
    /** The order. One of `desc`, `asc`. */
    public val order: String,
    /** The units. */
    public val units: Units,
    /** The unit of every `value`, `p10` and `p90`. One of `mm`, `cm`, `in`, `percent`. */
    public val unit: String,
    /** The items. */
    public val items: List<ExploreItem>,
    /** Items on this page. */
    public val count: Int,
    /** Pass as `?cursor=` (same other parameters) for the next page; null at the end. */
    @SerialName("next_cursor") public val nextCursor: String? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One ranked point of a place. */
@Serializable
public data class ExploreItem(
    /** 1-based, across pages. */
    public val rank: Int,
    /** The location. */
    public val location: LocationRef,
    /** The named elevation; `null` = the place's own point. */
    @SerialName("elevation_name") public val elevationName: String? = null,
    /**
     * What the elevation `elevation_name` is called, as `ElevationSummary.display_name`; absent where it has no
     * name.
     */
    @SerialName("elevation_display_name") public val elevationDisplayName: String? = null,
    /** The point's elevation, m or ft by `units`. */
    public val elevation: Double,
    /** The metric's value as the place's page serves it, in `unit`. */
    public val value: Double,
    /**
     * Present only on `snow_today` (the day's served `total_snowfall.p10`, where it has a `snow_amount`) and
     * `precip_today` (`total_precipitation_if_wet.p10`, where the day is not `none`).
     */
    public val p10: Double? = null,
    /** As `p10`. */
    public val p90: Double? = null,
    /**
     * When the value was taken: the forecast windows when recorded from the served run, `base_depth` its hour,
     * `new_snow_24h` the snowpack run's first hour, the season its newest computed day.
     */
    @SerialName("as_of") public val asOf: String,
    /** Forecast windows only, the served run's `model_run.initialized_at`. */
    @SerialName("initialized_at") public val initializedAt: String? = null,
    /** The source. One of `ourea`, `snowpack_measured`, `snowpack_open_loop`, `mrms_multisensor`, `station_gauge`. */
    public val source: String,
    /** With `near` only, from it (0.1 km). */
    @SerialName("distance_km") public val distanceKm: Double? = null,
    /** The place's forecast route; add `?elevations=<elevation_name>` for a named point. */
    public val path: String,
)

/** A search hit's Explore value; the fields mean what an `ExploreItem`'s do. */
@Serializable
public data class ExploreValue(
    /** The metric. */
    public val metric: ExploreMetric,
    /** The value. */
    public val value: Double,
    /** The unit. One of `mm`, `cm`, `in`, `percent`. */
    public val unit: String,
    /** The p10. */
    public val p10: Double? = null,
    /** The p90. */
    public val p90: Double? = null,
    /** The as of. */
    @SerialName("as_of") public val asOf: String,
    /** The initialized at. */
    @SerialName("initialized_at") public val initializedAt: String? = null,
    /** The source. */
    public val source: String,
)

/** `GET /api/v1/composition`. */
@Serializable
public data class CompositionResponse(
    /** The reference init: the newest contributing cycle, a forecast's `model_run.initialized_at`. */
    @SerialName("initialized_at") public val initializedAt: String,
    /**
     * Each contributing model, its own cycle and its members (`members` absent only when the cycle's member count
     * is not known).
     */
    public val models: List<ModelEntry>,
    /** Entries in `models`. */
    @SerialName("model_count") public val modelCount: Int,
    /** The sum of `models[].members`: the members the blend draws on. */
    @SerialName("total_members") public val totalMembers: Int,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}
