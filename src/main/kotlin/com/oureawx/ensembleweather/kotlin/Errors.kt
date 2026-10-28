// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Every error this SDK throws (BEHAVIOR.md section 8). Catch the subtype you can
 * handle, or switch on it:
 *
 * ```kotlin
 * try {
 *     ourea.getPointForecast(lat = 51.5, lon = -0.1)
 * } catch (e: OutsideCoverageException) {
 *     println("${e.code}: ${e.message}")
 * }
 * ```
 *
 * No message or string form ever contains the API key.
 */
public sealed class EnsembleWeatherException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * The server answered with an error status. [code] is the API's error code, kept
 * verbatim even when this SDK does not know it (`null` when the body was not the
 * JSON error envelope); the subtype is chosen by code first, then by [status].
 */
public sealed class ApiException(
    /** The HTTP status. */
    public val status: Int,
    /** The API error code (`OUTSIDE_COVERAGE`), verbatim; `null` for a body that is not JSON. */
    public val code: String?,
    message: String,
    /**
     * The request's id (the error body's `request_id`, else the `X-Request-Id` header):
     * quote it in a support request.
     */
    public val requestId: String?,
    /** The response's rate-limit state, when sent. */
    public val rateLimit: RateLimit?,
    /** The code's explanation on the docs errors page (cause, fix, whether to retry), when sent. */
    public val docUrl: String?,
    /** On a 400 about one query parameter, its name (`max_hours`, `prob`); else `null`. */
    public val param: String?,
) : EnsembleWeatherException(message) {
    override fun toString(): String =
        buildString {
            append(this@ApiException::class.simpleName).append(": HTTP ").append(status)
            if (code != null) append(' ').append(code)
            append(": ").append(message)
            if (requestId != null) append(" (request ").append(requestId).append(')')
        }
}

/** The API key is missing, invalid, revoked or suspended (`INVALID_API_KEY`, 401). */
public class AuthenticationException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    docUrl: String? = null,
    param: String? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param)

/**
 * Your plan does not allow this request (`PRO_REQUIRED`, 403). From a coordinate route
 * it may name the [nearestLocation] whose forecast answers without a key.
 */
public class PermissionException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    docUrl: String? = null,
    param: String? = null,
    nearestLocation: NearestLocation? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param) {
    /**
     * On `PRO_REQUIRED` from a coordinate route, the nearest named location whose forecast
     * answers without a key (within 10 km and 300 m of elevation); `null` when none qualifies.
     * Not kept when the exception is serialized.
     */
    @Transient
    public val nearestLocation: NearestLocation? = nearestLocation
}

/** No such location or route (`LOCATION_NOT_FOUND`, `NOT_FOUND`, 404, 410). */
public class NotFoundException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    docUrl: String? = null,
    param: String? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param)

/**
 * The server rejected a parameter (`BAD_REQUEST`, `INVALID_COORDINATES`,
 * `INVALID_ELEVATION`, `ELEVATION_REQUIRED`, 400); [param] names it when it is one.
 */
public class InvalidRequestException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    docUrl: String? = null,
    param: String? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param)

/** The point is outside the forecast domain (`OUTSIDE_COVERAGE`, a 404), mapped by code. */
public class OutsideCoverageException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    docUrl: String? = null,
    param: String? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param)

/**
 * Over a rate-limit budget (`TOO_MANY_REQUESTS`, 429). Thrown once retries are spent,
 * or at once when the server asks for a wait over 60 s.
 */
public class RateLimitException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    retryAfter: Duration?,
    docUrl: String? = null,
    param: String? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param) {
    // Kept as milliseconds so the exception stays java.io.Serializable.
    private val retryAfterMillis: Long? = retryAfter?.inWholeMilliseconds

    /** How long the server asked you to wait (`Retry-After`), when it said. */
    public val retryAfter: Duration? get() = retryAfterMillis?.milliseconds
}

/** The server could not answer (`FORECAST_UNAVAILABLE`, `TIMEOUT`, `AUTH_UNAVAILABLE`, 5xx). */
public class UnavailableException internal constructor(
    status: Int,
    code: String?,
    message: String,
    requestId: String?,
    rateLimit: RateLimit?,
    docUrl: String? = null,
    param: String? = null,
) : ApiException(status, code, message, requestId, rateLimit, docUrl, param)

/**
 * No response: DNS, TLS, a reset connection, the per-attempt timeout, too many
 * redirects, or a body over 64 MiB. Retried (except TLS verification failures and
 * oversized bodies) before it is thrown.
 */
public class ConnectionException internal constructor(
    message: String,
    cause: Throwable? = null,
) : EnsembleWeatherException(message, cause)

/**
 * A successful response the SDK could not read: a body that is not the documented
 * JSON, or an answer that does not match the probability or total terms asked.
 */
public class ResponseFormatException internal constructor(
    message: String,
    cause: Throwable? = null,
) : EnsembleWeatherException(message, cause)

/**
 * Refused before sending: an invalid probability or total term, a malformed
 * location, a window out of range. Nothing was sent.
 */
public class UsageException internal constructor(
    message: String,
) : EnsembleWeatherException(message)
