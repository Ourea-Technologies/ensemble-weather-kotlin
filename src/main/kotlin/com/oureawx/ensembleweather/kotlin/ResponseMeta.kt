// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

/**
 * A top-level response body. Its [meta] says how this answer was obtained; it is
 * not part of the body, so it takes no part in `equals`, `hashCode` or `toString`,
 * and a `copy()` does not carry it.
 */
public sealed interface ApiResponse {
    /** The rate-limit state, request id and cache outcome of the response that produced this body. */
    public val meta: ResponseMeta
}

/** Attaches the metadata of the response that produced this body (once, before it is returned). */
internal fun ApiResponse.attach(meta: ResponseMeta) {
    when (this) {
        is ForecastResponse -> this.meta = meta
        is CurrentResponse -> this.meta = meta
        is AlertsResponse -> this.meta = meta
        is NearbyLocationsResponse -> this.meta = meta
        is NearbyPointResponse -> this.meta = meta
        is LocationsResponse -> this.meta = meta
        is SearchLocationsResponse -> this.meta = meta
        is PublicCatalogLocationPage -> this.meta = meta
        is PublicCatalogStationPage -> this.meta = meta
        is PublicCatalogLocationDetail -> this.meta = meta
        is PublicCatalogStationDetail -> this.meta = meta
        is AttributionRegistry -> this.meta = meta
        is RunsResponse -> this.meta = meta
        is StormReportsResponse -> this.meta = meta
        is StormReportCard -> this.meta = meta
    }
}

/**
 * How a response was obtained.
 *
 * @property rateLimit the `RateLimit-*` headers of the response, when the server sent them
 *   (it sends none on a response a shared cache may store).
 * @property requestId the `X-Request-Id` header, when present.
 * @property revalidated `true` when the server answered `304 Not Modified` and this body is
 *   the SDK's cached copy.
 */
public data class ResponseMeta(
    public val rateLimit: RateLimit? = null,
    public val requestId: String? = null,
    public val revalidated: Boolean = false,
) {
    public companion object {
        /** No metadata: a body built in code rather than received. */
        public val None: ResponseMeta = ResponseMeta()
    }
}

/**
 * The rate-limit state of a response, read from its `RateLimit-*` headers
 * (BEHAVIOR.md section 9). Read it rather than hard-coding limits.
 *
 * @property limit `RateLimit-Limit`: the size of the budget this response counted against.
 * @property remaining `RateLimit-Remaining`: requests left before the binding limit refuses.
 * @property resetSeconds `RateLimit-Reset`: seconds until the binding limit is whole again.
 * @property policy `RateLimit-Policy`, verbatim (for example `60;w=60, 10000;w=86400`).
 */
public data class RateLimit(
    public val limit: Long? = null,
    public val remaining: Long? = null,
    public val resetSeconds: Long? = null,
    public val policy: String? = null,
) : java.io.Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * A `Deprecation` or `Sunset` notice the server sent for an operation
 * (BEHAVIOR.md section 11). The SDK reports each operation at most once per process.
 *
 * @property operation the operation's id in the OpenAPI spec (for example `getLocationForecast`).
 * @property deprecation the `Deprecation` header, verbatim.
 * @property sunset the `Sunset` header, verbatim.
 * @property link the `Link` URL with `rel="deprecation"`, when sent.
 */
public data class DeprecationNotice(
    public val operation: String,
    public val deprecation: String?,
    public val sunset: String?,
    public val link: String?,
) {
    /** One line for a log. */
    public fun describe(): String =
        buildString {
            append("Ourea API operation ").append(operation).append(" is deprecated")
            if (deprecation != null) append(" (Deprecation: ").append(deprecation).append(')')
            if (sunset != null) append("; sunset ").append(sunset)
            if (link != null) append("; see ").append(link)
        }
}
