// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** One page of catalog locations. */
@Serializable
public data class PublicCatalogLocationPage(
    /** The rows. */
    public val items: List<PublicCatalogLocation>,
    /** The cursor for the next page; `null` at the end. */
    @SerialName("next_cursor") public val nextCursor: String? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** One page of catalog stations. */
@Serializable
public data class PublicCatalogStationPage(
    /** The rows. */
    public val items: List<PublicCatalogStation>,
    /** The cursor for the next page; `null` at the end. */
    @SerialName("next_cursor") public val nextCursor: String? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** A catalog location's public projection. */
@Serializable
public data class PublicCatalogLocation(
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
    public val county: String? = null,
    /** The ISO 3166-1 country code. */
    public val country: String? = null,
    /** Other names. */
    public val aliases: List<String>? = null,
    /** A short description. */
    @SerialName("description_short") public val descriptionShort: String? = null,
    /** A long description. */
    @SerialName("description_long") public val descriptionLong: String? = null,
    /** Microclimate notes. */
    @SerialName("microclimate_notes") public val microclimateNotes: String? = null,
    /** The page title. */
    @SerialName("meta_title") public val metaTitle: String? = null,
    /** The page description. */
    @SerialName("meta_description") public val metaDescription: String? = null,
    /** The best seasons to visit. */
    @SerialName("best_seasons") public val bestSeasons: List<String>? = null,
    /** Key weather factors. */
    @SerialName("key_factors") public val keyFactors: List<String>? = null,
    /** Tags. */
    public val tags: List<String>? = null,
    /** Free-form JSON of labelled links. */
    @SerialName("external_links") public val externalLinks: JsonElement? = null,
    /** Whether it is featured. */
    @SerialName("is_featured") public val isFeatured: Boolean? = null,
)

/** A catalog location with its elevations, photos, webcams and nearby stations. */
@Serializable
public data class PublicCatalogLocationDetail(
    /** The catalog slug (the canonical one, even when looked up by an old slug). */
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
    /** The elevation points. */
    public val elevations: List<PublicCatalogElevation>,
    /** Photos. */
    public val photos: List<PublicCatalogPhoto>,
    /** Webcams. */
    public val webcams: List<PublicCatalogWebcam>,
    /** Nearby stations. */
    @SerialName("nearby_stations") public val nearbyStations: List<PublicCatalogNearbyStation>,
    /** The county. */
    public val county: String? = null,
    /** The ISO 3166-1 country code. */
    public val country: String? = null,
    /** Other names. */
    public val aliases: List<String>? = null,
    /** A short description. */
    @SerialName("description_short") public val descriptionShort: String? = null,
    /** A long description. */
    @SerialName("description_long") public val descriptionLong: String? = null,
    /** Microclimate notes. */
    @SerialName("microclimate_notes") public val microclimateNotes: String? = null,
    /** The page title. */
    @SerialName("meta_title") public val metaTitle: String? = null,
    /** The page description. */
    @SerialName("meta_description") public val metaDescription: String? = null,
    /** The best seasons to visit. */
    @SerialName("best_seasons") public val bestSeasons: List<String>? = null,
    /** Key weather factors. */
    @SerialName("key_factors") public val keyFactors: List<String>? = null,
    /** Tags. */
    public val tags: List<String>? = null,
    /** Free-form JSON of labelled links. */
    @SerialName("external_links") public val externalLinks: JsonElement? = null,
    /** Whether it is featured. */
    @SerialName("is_featured") public val isFeatured: Boolean? = null,
    /** The resolved page template (with `include = listOf("template")`). */
    public val template: CatalogTemplateSummary? = null,
    /** Published content entries (with `include = listOf("content")`). */
    public val content: List<CatalogResolvedEntry>? = null,
    /** Resolved ad placements (with `include = listOf("ads")`, Free tier). */
    public val ads: List<CatalogResolvedAdSlot>? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** A catalog station's public projection. */
@Serializable
public data class PublicCatalogStation(
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
    /** The type (`observation_station`). */
    public val type: LocationType,
    /** The IANA time zone. */
    public val timezone: String,
    /** The county. */
    public val county: String? = null,
    /** The ISO 3166-1 country code. */
    public val country: String? = null,
    /** Other names. */
    public val aliases: List<String>? = null,
    /** The observation network (`Snotel`). */
    public val network: String? = null,
    /** The station's id in its network. */
    @SerialName("source_station_id") public val sourceStationId: String? = null,
    /** Elevation above mean sea level, metres. */
    @SerialName("elevation_msl_m") public val elevationMslM: Double? = null,
    /** Sensor height above ground, metres. */
    @SerialName("sensor_height_agl_m") public val sensorHeightAglM: Double? = null,
    /** What the station measures. */
    @SerialName("sensor_capabilities") public val sensorCapabilities: List<String>? = null,
    /** The station status (`Active`). */
    @SerialName("station_status") public val stationStatus: String? = null,
    /** When it last reported (ISO 8601). */
    @SerialName("last_report_at") public val lastReportAt: String? = null,
    /** Free-form JSON of the last report. */
    @SerialName("last_report_summary") public val lastReportSummary: JsonElement? = null,
    /** When it was installed (`yyyy-mm-dd`). */
    @SerialName("install_date") public val installDate: String? = null,
    /** When it was decommissioned (`yyyy-mm-dd`). */
    @SerialName("decommission_date") public val decommissionDate: String? = null,
    /** How often it reports, minutes. */
    @SerialName("update_frequency_minutes") public val updateFrequencyMinutes: Int? = null,
    /** The station's page. */
    @SerialName("station_url") public val stationUrl: String? = null,
    /** The station's data. */
    @SerialName("data_url") public val dataUrl: String? = null,
    /** A short description. */
    @SerialName("description_short") public val descriptionShort: String? = null,
    /** A long description. */
    @SerialName("description_long") public val descriptionLong: String? = null,
    /** The page title. */
    @SerialName("meta_title") public val metaTitle: String? = null,
    /** The page description. */
    @SerialName("meta_description") public val metaDescription: String? = null,
    /** Tags. */
    public val tags: List<String>? = null,
    /** Whether it is featured. */
    @SerialName("is_featured") public val isFeatured: Boolean? = null,
)

/** A catalog station with its photos, webcams and primary location. */
@Serializable
public data class PublicCatalogStationDetail(
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
    /** The type (`observation_station`). */
    public val type: LocationType,
    /** The IANA time zone. */
    public val timezone: String,
    /** Photos. */
    public val photos: List<PublicCatalogPhoto>,
    /** Webcams. */
    public val webcams: List<PublicCatalogWebcam>,
    /** The county. */
    public val county: String? = null,
    /** The ISO 3166-1 country code. */
    public val country: String? = null,
    /** Other names. */
    public val aliases: List<String>? = null,
    /** The observation network. */
    public val network: String? = null,
    /** The station's id in its network. */
    @SerialName("source_station_id") public val sourceStationId: String? = null,
    /** Elevation above mean sea level, metres. */
    @SerialName("elevation_msl_m") public val elevationMslM: Double? = null,
    /** Sensor height above ground, metres. */
    @SerialName("sensor_height_agl_m") public val sensorHeightAglM: Double? = null,
    /** What the station measures. */
    @SerialName("sensor_capabilities") public val sensorCapabilities: List<String>? = null,
    /** The station status. */
    @SerialName("station_status") public val stationStatus: String? = null,
    /** When it last reported (ISO 8601). */
    @SerialName("last_report_at") public val lastReportAt: String? = null,
    /** Free-form JSON of the last report. */
    @SerialName("last_report_summary") public val lastReportSummary: JsonElement? = null,
    /** When it was installed. */
    @SerialName("install_date") public val installDate: String? = null,
    /** When it was decommissioned. */
    @SerialName("decommission_date") public val decommissionDate: String? = null,
    /** How often it reports, minutes. */
    @SerialName("update_frequency_minutes") public val updateFrequencyMinutes: Int? = null,
    /** The station's page. */
    @SerialName("station_url") public val stationUrl: String? = null,
    /** The station's data. */
    @SerialName("data_url") public val dataUrl: String? = null,
    /** A short description. */
    @SerialName("description_short") public val descriptionShort: String? = null,
    /** A long description. */
    @SerialName("description_long") public val descriptionLong: String? = null,
    /** The page title. */
    @SerialName("meta_title") public val metaTitle: String? = null,
    /** The page description. */
    @SerialName("meta_description") public val metaDescription: String? = null,
    /** Tags. */
    public val tags: List<String>? = null,
    /** Whether it is featured. */
    @SerialName("is_featured") public val isFeatured: Boolean? = null,
    /** The station's parent location. */
    @SerialName("primary_location") public val primaryLocation: PublicCatalogPrimaryLocation? = null,
    /** The resolved page template. */
    public val template: CatalogTemplateSummary? = null,
    /** Published content entries. */
    public val content: List<CatalogResolvedEntry>? = null,
    /** Resolved ad placements. */
    public val ads: List<CatalogResolvedAdSlot>? = null,
) : ApiResponse {
    @Transient
    override var meta: ResponseMeta = ResponseMeta.None
        internal set
}

/** A station's parent location. */
@Serializable
public data class PublicCatalogPrimaryLocation(
    /** The catalog slug. */
    public val slug: String,
    /** The display name. */
    public val name: String,
    /** Latitude. */
    public val lat: Double,
    /** Longitude. */
    public val lon: Double,
    /** Elevation in metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** The type, as the catalog spells it. */
    public val type: String,
)

/** A catalog location's elevation point. */
@Serializable
public data class PublicCatalogElevation(
    /** Its label (`Mid-Mountain`). */
    public val label: String,
    /** Elevation in metres. */
    @SerialName("elevation_m") public val elevationM: Double,
    /** Whether it is the default to show. */
    @SerialName("is_default") public val isDefault: Boolean,
    /** Its own latitude, when it has one. */
    @SerialName("lat_override") public val latOverride: Double? = null,
    /** Its own longitude, when it has one. */
    @SerialName("lon_override") public val lonOverride: Double? = null,
)

/** A photo. */
@Serializable
public data class PublicCatalogPhoto(
    /** Its public URL. */
    public val url: String,
    /** A caption. */
    public val caption: String? = null,
    /** The credit. */
    public val credit: String? = null,
    /** Alternative text. */
    @SerialName("alt_text") public val altText: String? = null,
    /** The licence. */
    public val license: String? = null,
    /** The licence URL. */
    @SerialName("license_url") public val licenseUrl: String? = null,
)

/** A webcam. */
@Serializable
public data class PublicCatalogWebcam(
    /** Its name. */
    public val name: String,
    /** Its URL. */
    public val url: String,
    /** How to embed it. */
    @SerialName("embed_type") public val embedType: String? = null,
    /** Its provider. */
    public val provider: String? = null,
    /** How often it refreshes, seconds. */
    @SerialName("refresh_interval_seconds") public val refreshIntervalSeconds: Int? = null,
)

/** A station near a catalog location. */
@Serializable
public data class PublicCatalogNearbyStation(
    /** The station's catalog slug. */
    public val slug: String,
    /** Its name. */
    public val name: String,
    /** Its network. */
    public val network: String,
    /** Its status. */
    @SerialName("station_status") public val stationStatus: String,
    /** Whether it is the location's primary station. */
    @SerialName("is_primary") public val isPrimary: Boolean,
    /** Distance, km. */
    @SerialName("distance_km") public val distanceKm: Double? = null,
    /** Station elevation minus the location's, metres. */
    @SerialName("elevation_diff_m") public val elevationDiffM: Double? = null,
)

/** A resolved page template. */
@Serializable
public data class CatalogTemplateSummary(
    /** The template id. */
    public val id: String,
    /** The sections. */
    public val spec: List<CmsTemplateSection>,
    /** The location type it is for; `null` is a wildcard. */
    @SerialName("location_type") public val locationType: String? = null,
    /** The template variant; `null` is a wildcard. */
    @SerialName("template_variant") public val templateVariant: String? = null,
)

/** One section of a page template. */
@Serializable
public data class CmsTemplateSection(
    /** `weather_panel`, `content_slot`, `ad_slot` or `media`. */
    public val kind: String,
    /** The section key. */
    public val key: String,
    /** Client-defined render hints. */
    public val config: JsonElement? = null,
)

/** One published content entry. */
@Serializable
public data class CatalogResolvedEntry(
    /** The entry id. */
    public val id: String,
    /** The content type (`trail-report`). */
    @SerialName("content_type_key") public val contentTypeKey: String,
    /** The title. */
    public val title: String,
    /** The body, shaped by its content type. */
    public val body: JsonObject,
    /** The template slot it fills. */
    public val slot: String,
    /** Its order in the slot. */
    @SerialName("display_order") public val displayOrder: Int,
    /** The locale it resolved to. */
    @SerialName("resolved_locale") public val resolvedLocale: String,
    /** When it was updated (ISO 8601). */
    @SerialName("updated_at") public val updatedAt: String,
)

/** One filled ad slot. */
@Serializable
public data class CatalogResolvedAdSlot(
    /** The slot key. */
    @SerialName("slot_key") public val slotKey: String,
    /** `house` or `third_party`. */
    public val kind: String,
    /** The slot's rendering sizes. */
    public val sizes: List<List<Int>>,
    /** Present when [kind] is `house`. */
    public val house: CatalogHousePlacement? = null,
    /** Present when [kind] is `third_party`. */
    @SerialName("third_party") public val thirdParty: CatalogThirdPartyPlacement? = null,
)

/** A house ad placement. */
@Serializable
public data class CatalogHousePlacement(
    /** The campaign id. */
    @SerialName("campaign_id") public val campaignId: String,
    /** The creative id. */
    @SerialName("creative_id") public val creativeId: String,
    /** The creative. */
    public val content: CmsAdCreativeContent,
)

/** A house ad creative. */
@Serializable
public data class CmsAdCreativeContent(
    /** The headline. */
    public val headline: String? = null,
    /** The body text. */
    public val body: String? = null,
    /** The call-to-action label. */
    public val cta: String? = null,
    /** Where a click goes. */
    @SerialName("click_url") public val clickUrl: String? = null,
    /** The photo id. */
    @SerialName("photo_id") public val photoId: String? = null,
)

/** A third-party ad placement. */
@Serializable
public data class CatalogThirdPartyPlacement(
    /** The ad unit id. */
    @SerialName("unit_id") public val unitId: String,
    /** `adsense`, `gam` or `custom`. */
    public val provider: String,
    /** The network config, verbatim. */
    public val config: JsonObject,
)
