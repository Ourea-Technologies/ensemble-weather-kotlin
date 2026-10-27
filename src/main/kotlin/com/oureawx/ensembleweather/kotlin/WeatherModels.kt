// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Current conditions: the nearest sensors, active alerts and the "right now" block. */
@Serializable
public data class CurrentResponse(
    /** Where the conditions are for. */
    public val location: LocationRef,
    /** Active NWS watches, warnings and advisories. */
    public val alerts: List<ActiveAlert>,
    /** When the alerts snapshot was taken (ISO 8601). */
    @SerialName("alerts_as_of") public val alertsAsOf: String,
    /** Each sensor's latest readings, in the request's units. */
    public val sensors: List<SensorObservation>,
    /** How many sensors are listed. */
    @SerialName("sensor_count") public val sensorCount: Int,
    /** When this overlay was assembled (ISO 8601). */
    @SerialName("as_of") public val asOf: String,
    /** The unit system of every value. */
    public val units: Units,
    /** The data sources and their credit line. */
    public val attribution: ResponseAttribution,
    /** The init of the cycle the band was read from; `null` when no cycle is servable. */
    @SerialName("model_run_initialized_at") public val modelRunInitializedAt: String? = null,
    /** The obs-blended forecast hours around now (with `include = listOf("nowcast_hours")`). */
    @SerialName("nowcast_hours") public val nowcastHours: List<TimeSeriesPoint>? = null,
    /** "Right now", flattened; `null` when no served hour is within an hour of now. */
    public val current: CurrentConditions? = null,
    /** The live nowcast sources this body was built without. */
    @SerialName("nowcast_unavailable") public val nowcastUnavailable: List<NowcastSource>? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** A compact active NWS alert. */
@Serializable
public data class ActiveAlert(
    /** The NWS alert id. */
    public val id: String,
    /** The event (`Winter Storm Warning`). */
    public val event: String,
    /** The NWS severity. */
    public val severity: String,
    /** The NWS urgency. */
    public val urgency: String,
    /** The headline text. */
    public val headline: String,
    /** When it expires (ISO 8601). */
    public val expires: String,
    /** When it begins, when NWS gives one. */
    public val onset: String? = null,
    /** When it ends, when NWS gives one. */
    public val ends: String? = null,
)

/** One sensor's latest readings, in the request's units; a reading it does not take is `null`. */
@Serializable
public data class SensorObservation(
    /** The station's id in its network. */
    @SerialName("station_id") public val stationId: String,
    /** The station's name. */
    public val name: String,
    /** The observation network. */
    public val source: String,
    /** When it reported (ISO 8601). */
    @SerialName("observation_time") public val observationTime: String,
    /** Air temperature. */
    public val temperature: Double? = null,
    /** Dewpoint. */
    public val dewpoint: Double? = null,
    /** Relative humidity, percent. */
    @SerialName("relative_humidity") public val relativeHumidity: Double? = null,
    /** Wind speed. */
    @SerialName("wind_speed") public val windSpeed: Double? = null,
    /** Gust. */
    @SerialName("wind_gust") public val windGust: Double? = null,
    /** Wind direction, degrees. */
    @SerialName("wind_direction") public val windDirection: Double? = null,
    /** Station pressure (hPa, or inHg for imperial). */
    public val pressure: Double? = null,
    /** The last reporting period's precipitation. */
    public val precipitation: Double? = null,
    /** Radar precipitation rate, per hour. */
    @SerialName("precip_rate") public val precipRate: Double? = null,
    /** Radar reflectivity, dBZ. */
    public val reflectivity: Double? = null,
    /** Pavement temperature. */
    @SerialName("pavement_temp") public val pavementTemp: Double? = null,
    /** Subsurface temperature. */
    @SerialName("subsurface_temp") public val subsurfaceTemp: Double? = null,
    /** Visibility. */
    public val visibility: Double? = null,
    /** Cloud cover, percent. */
    @SerialName("cloud_cover") public val cloudCover: Double? = null,
    /** Ceiling height. */
    @SerialName("ceiling_height") public val ceilingHeight: Double? = null,
    /** Snow depth (cm, or in for imperial). */
    @SerialName("snow_depth") public val snowDepth: Double? = null,
    /** Snow water equivalent. */
    public val swe: Double? = null,
    /** SWE as a percent of the station's median for the date. */
    @SerialName("swe_percent_of_median") public val swePercentOfMedian: Double? = null,
    /** Snowfall. */
    public val snowfall: Double? = null,
    /** Solar radiation, W/m². */
    @SerialName("solar_radiation") public val solarRadiation: Double? = null,
    /** Road-weather pavement state, as the network reports it. */
    @SerialName("pavement_condition") public val pavementCondition: String? = null,
)

/** The active alerts for a location or point. */
@Serializable
public data class AlertsResponse(
    /** Where the alerts are for. */
    public val location: LocationRef,
    /** How many alerts. */
    @SerialName("alert_count") public val alertCount: Int,
    /** The snapshot time (ISO 8601). */
    @SerialName("as_of") public val asOf: String,
    /** The alerts (possibly none). */
    public val alerts: List<ActiveAlert>,
    /** The data sources and their credit line. */
    public val attribution: ResponseAttribution,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** A list, nearby or favorite row. */
@Serializable
public data class LocationSummary(
    /** The catalog slug. */
    public val slug: String,
    /** The lowercase USPS state code. */
    public val state: String,
    /** The display name. */
    public val name: String,
    /** Latitude. */
    public val lat: Double,
    /** Longitude. */
    public val lon: Double,
    /** Elevation in metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The primary type. */
    public val type: LocationType,
    /** The IANA time zone. */
    public val timezone: String,
    /** The county. */
    public val county: String,
    /** A catalog blurb. */
    public val description: String? = null,
    /** The named elevation points, the primary first. */
    public val elevations: List<LocationElevation>? = null,
)

/** A named elevation point of a location. */
@Serializable
public data class LocationElevation(
    /** Its name. */
    public val name: String,
    /** The elevation in the request's length unit. */
    public val elevation: Double,
    /** `true` on the primary only. */
    @SerialName("default") public val isDefault: Boolean? = null,
)

/** A nearby location, with its distance. */
@Serializable
public data class NearbyLocation(
    /** The catalog slug. */
    public val slug: String,
    /** The lowercase USPS state code. */
    public val state: String,
    /** The display name. */
    public val name: String,
    /** Latitude. */
    public val lat: Double,
    /** Longitude. */
    public val lon: Double,
    /** Elevation in metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The primary type. */
    public val type: LocationType,
    /** The IANA time zone. */
    public val timezone: String,
    /** The county. */
    public val county: String,
    /** Great-circle distance, km. */
    @SerialName("distance_km") public val distanceKm: Double,
    /** A catalog blurb. */
    public val description: String? = null,
    /** The named elevation points. */
    public val elevations: List<LocationElevation>? = null,
)

/** A location and its nearest neighbours. */
@Serializable
public data class NearbyLocationsResponse(
    /** The location itself. */
    public val location: LocationSummary,
    /** The radius applied, km. */
    @SerialName("radius_km") public val radiusKm: Double,
    /** How many neighbours. */
    public val count: Int,
    /** The neighbours, nearest first. */
    public val locations: List<NearbyLocation>,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** The nearest locations to a coordinate. */
@Serializable
public data class NearbyPointResponse(
    /** The point's latitude. */
    public val lat: Double,
    /** The point's longitude. */
    public val lon: Double,
    /** The radius applied, km. */
    @SerialName("radius_km") public val radiusKm: Double,
    /** How many locations. */
    public val count: Int,
    /** The locations, nearest first. */
    public val locations: List<NearbyLocation>,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One page of the location lists. */
@Serializable
public data class LocationsResponse(
    /** The scope's label (`Oregon`, or `all`). */
    public val state: String,
    /** Rows in this page. */
    public val count: Int,
    /** The rows, ordered by slug. */
    public val locations: List<LocationSummary>,
    /** The cursor for the next page; absent on the last one. */
    @SerialName("next_cursor") public val nextCursor: String? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** Search hits, best first. */
@Serializable
public data class SearchLocationsResponse(
    /** The hits. */
    public val items: List<SearchHit>,
    /** How many hits. */
    public val count: Int,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One search hit. Key results by [key]: bare slugs are unique only per state. */
@Serializable
public data class SearchHit(
    /** The state-qualified identity. */
    public val key: String,
    /** The catalog slug. */
    public val slug: String,
    /** The lowercase USPS state code. */
    public val state: String,
    /** The display name. */
    public val name: String,
    /** Latitude. */
    public val lat: Double,
    /** Longitude. */
    public val lon: Double,
    /** Elevation in metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The primary type. */
    public val type: LocationType,
    /** The IANA time zone. */
    public val timezone: String,
    /** The county. */
    public val county: String,
)

/** Every data source, grouped by provider, with its licence. */
@Serializable
public data class AttributionRegistry(
    /** The providers. */
    public val providers: List<AttributionProvider>,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One data provider and its licence terms. */
@Serializable
public data class AttributionProvider(
    /** The provider id. */
    public val id: String,
    /** The provider's name. */
    public val name: String,
    /** The credit line a response carries for it. */
    public val credit: String,
    /** The licence (an SPDX id where one exists). */
    public val licence: String,
    /** The licence URL. */
    @SerialName("licence_url") public val licenceUrl: String,
    /** The terms page the wording was taken from. */
    @SerialName("terms_url") public val termsUrl: String,
    /** Whether Ourea serves this provider's data modified. */
    public val modified: Boolean,
    /** Statements the terms require shown in full. */
    public val notices: List<String>,
    /** Its sources. */
    public val sources: List<AttributionSource>,
)

/** One data source. */
@Serializable
public data class AttributionSource(
    /** The source id. */
    public val id: String,
    /** Its name. */
    public val name: String,
    /** Its kind. */
    public val kind: SourceKind,
    /** Its provider's id. */
    public val provider: String,
)

/** The cycles a `run` can pin, and what a forecast serves now. */
@Serializable
public data class RunsResponse(
    /** What a forecast request serves now; `null` when no cycle is servable. */
    public val served: ServedRun? = null,
    /** Init times at least two models completed, newest first. */
    public val runs: List<RunInfo>,
    /** How many runs. */
    public val count: Int,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** What a forecast request serves now. */
@Serializable
public data class ServedRun(
    /** The reference init (ISO 8601). */
    @SerialName("initialized_at") public val initializedAt: String,
    /** Each contributing model and its own cycle. */
    public val models: List<ModelEntry>,
)

/** A pinnable run. */
@Serializable
public data class RunInfo(
    /** Its init time (ISO 8601). */
    @SerialName("initialized_at") public val initializedAt: String,
)

/** Storm report cards, most recent onset first. */
@Serializable
public data class StormReportsResponse(
    /** The cards. */
    public val reports: List<StormReportCard>,
    /** How many. */
    public val count: Int,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** A verified storm event: forecast against observed totals, with a skill score. */
@Serializable
public data class StormReportCard(
    /** The card id. */
    public val id: Long,
    /** The location's name. */
    @SerialName("location_name") public val locationName: String,
    /** The precipitation regime (`snowfall`, `rainfall`, ...). */
    @SerialName("event_type") public val eventType: String,
    /** Onset (ISO 8601). */
    @SerialName("onset_time") public val onsetTime: String,
    /** End (ISO 8601). */
    @SerialName("end_time") public val endTime: String,
    /** The forecast total, mm. */
    @SerialName("forecast_total_mm") public val forecastTotalMm: Double,
    /** The observed total, mm. */
    @SerialName("observed_total_mm") public val observedTotalMm: Double,
    /** The forecast snowfall, mm. */
    @SerialName("forecast_snowfall_mm") public val forecastSnowfallMm: Double? = null,
    /** The observed snowfall, mm. */
    @SerialName("observed_snowfall_mm") public val observedSnowfallMm: Double? = null,
    /** The best-scoring model. */
    @SerialName("best_model") public val bestModel: String? = null,
    /** Its lead time, hours. */
    @SerialName("best_lead_time_hours") public val bestLeadTimeHours: Int? = null,
    /** A 0..1 skill score (higher is better). */
    @SerialName("composite_score") public val compositeScore: Double? = null,
    /** A display headline. */
    public val headline: String? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}
