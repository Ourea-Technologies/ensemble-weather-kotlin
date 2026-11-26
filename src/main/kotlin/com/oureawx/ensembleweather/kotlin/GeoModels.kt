// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement

/**
 * A GeoJSON `FeatureCollection` (RFC 7946) of the active winter and wind alerts' outlines, with this response's
 * `attribution` as a foreign member.
 */
@Serializable
public data class AlertOutlineCollection(
    /** The type. One of `FeatureCollection`. */
    public val type: String,
    /** One Feature per alert, ordered by `properties.id`. */
    public val features: List<AlertOutlineFeature>,
    /** The attribution. */
    public val attribution: ResponseAttribution,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One alert's outline, a GeoJSON `Feature`. */
@Serializable
public data class AlertOutlineFeature(
    /** The type. One of `Feature`. */
    public val type: String,
    /** The geometry. */
    public val geometry: AlertOutlineGeometry,
    /** The properties. */
    public val properties: AlertOutlineProperties,
)

/** A GeoJSON `Polygon` or `MultiPolygon`. */
@Serializable
public data class AlertOutlineGeometry(
    /** The type. One of `Polygon`, `MultiPolygon`. */
    public val type: String,
    /** The coordinates. */
    public val coordinates: JsonElement,
)

/** The alert, under the names `ActiveAlert` uses for the same fields, and its `class`. */
@Serializable
public data class AlertOutlineProperties(
    /** NWS alert identifier (urn:oid:…), as `ActiveAlert.id`. */
    public val id: String,
    /** The event. */
    public val event: String,
    /** The class. */
    @SerialName("class") public val alertClass: AlertClass,
    /** As `ActiveAlert.severity`. */
    public val severity: AlertSeverity,
    /** As `ActiveAlert.urgency`. */
    public val urgency: AlertUrgency,
    /** As `ActiveAlert.certainty`. */
    public val certainty: AlertCertainty,
    /** The headline. */
    public val headline: String,
    /** When the hazard begins. */
    public val onset: String? = null,
    /** When NWS is due to re-issue this message; not the alert's end (see `ActiveAlert.expires`). */
    public val expires: String,
    /** When the hazard ends. */
    public val ends: String? = null,
    /** The NWS office. */
    @SerialName("sender_name") public val senderName: String? = null,
    /**
     * The NWS forecast page of a point inside the outline, which lists the alert and links to its text: where "view
     * on NWS" goes.
     */
    @SerialName("source_url") public val sourceUrl: String,
)

/**
 * A GeoJSON `FeatureCollection` (RFC 7946) of the avalanche centers' forecast zones, each with its published rating,
 * and this response's `attribution` as a foreign member.
 */
@Serializable
public data class AvalancheZoneCollection(
    /** The type. One of `FeatureCollection`. */
    public val type: String,
    /**
     * One Feature per zone, the less specific first: where zones overlap, the one drawn last is the one a point
     * there is served.
     */
    public val features: List<AvalancheZoneFeature>,
    /** The attribution. */
    public val attribution: ResponseAttribution,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One forecast zone, a GeoJSON `Feature`. */
@Serializable
public data class AvalancheZoneFeature(
    /** The type. One of `Feature`. */
    public val type: String,
    /** The zone's id, as `properties.zone_id`. */
    public val id: Long,
    /** The geometry. */
    public val geometry: AvalancheZoneGeometry,
    /** The properties. */
    public val properties: AvalancheZoneProperties,
)

/**
 * An avalanche zone's properties, with the API's keys untouched (GeoJSON is passed through, so `forecast_url` is
 * [forecastUrl], not renamed).
 */
@Serializable
public data class AvalancheZoneProperties(
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
    /** The center's full forecast for the zone. */
    @SerialName("forecast_url") public val forecastUrl: String,
    /** The center url. */
    @SerialName("center_url") public val centerUrl: String,
    /**
     * The credit to show with the block: "Avalanche forecast by <center> via avalanche.org", or "Avalanche forecast
     * by Colorado Avalanche Information Center via avalanche.state.co.us" for a CAIC area.
     */
    public val attribution: String,
)

/**
 * A GeoJSON `MultiPolygon`: an array of polygons, each an array of rings (the outer one first, then any holes), each
 * ring closed.
 */
@Serializable
public data class AvalancheZoneGeometry(
    /** The type. One of `MultiPolygon`. */
    public val type: String,
    /** The coordinates. */
    public val coordinates: JsonElement,
)
