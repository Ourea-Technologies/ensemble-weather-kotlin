// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin.internal

import com.oureawx.ensembleweather.kotlin.ApiException
import com.oureawx.ensembleweather.kotlin.AuthenticationException
import com.oureawx.ensembleweather.kotlin.BodyTooLargeException
import com.oureawx.ensembleweather.kotlin.CachedResponse
import com.oureawx.ensembleweather.kotlin.ConnectionException
import com.oureawx.ensembleweather.kotlin.DeprecationNotice
import com.oureawx.ensembleweather.kotlin.EnsembleWeather
import com.oureawx.ensembleweather.kotlin.HttpRequest
import com.oureawx.ensembleweather.kotlin.HttpResponse
import com.oureawx.ensembleweather.kotlin.HttpTransport
import com.oureawx.ensembleweather.kotlin.InvalidRequestException
import com.oureawx.ensembleweather.kotlin.NearestLocation
import com.oureawx.ensembleweather.kotlin.NotFoundException
import com.oureawx.ensembleweather.kotlin.OutsideCoverageException
import com.oureawx.ensembleweather.kotlin.PermissionException
import com.oureawx.ensembleweather.kotlin.RateLimit
import com.oureawx.ensembleweather.kotlin.RateLimitException
import com.oureawx.ensembleweather.kotlin.ResponseCache
import com.oureawx.ensembleweather.kotlin.ResponseFormatException
import com.oureawx.ensembleweather.kotlin.ResponseMeta
import com.oureawx.ensembleweather.kotlin.UnavailableException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** One operation: its OpenAPI id, and whether its route is credential-free (BEHAVIOR.md section 3.2). */
internal class Operation(
    val id: String,
    val open: Boolean = false,
)

/** A successful body and how it was obtained. */
internal class Fetched(
    val body: ByteArray,
    val meta: ResponseMeta,
)

/** Everything the transport loop needs; built once per client. No toString: it holds the API key. */
@Suppress("LongParameterList")
internal class Settings(
    val baseUrl: String,
    val apiKey: String?,
    val sendKeyOnOpenRoutes: Boolean,
    val timeout: Duration,
    val maxRetries: Int,
    val cache: ResponseCache?,
    val transport: HttpTransport,
    val userAgent: String,
    val onDeprecation: (DeprecationNotice) -> Unit,
    val random: () -> Double,
    val clock: () -> Long,
)

/**
 * Sends one GET with the SDK's rules: the key only where it belongs, redirects on the
 * same host, retries with backoff, the ETag cache and the deprecation notices.
 */
internal class Requester(
    private val settings: Settings,
    private val rateLimit: MutableStateFlow<RateLimit?>,
) {
    private val baseOrigin = origin(settings.baseUrl)
    private val keyHash = settings.apiKey?.let { "key:" + sha256(it) } ?: "anonymous"

    suspend fun get(
        operation: Operation,
        path: String,
        query: String,
    ): Fetched {
        val url = settings.baseUrl + path + if (query.isEmpty()) "" else "?$query"
        val sendKey = settings.apiKey != null && (!operation.open || settings.sendKeyOnOpenRoutes)
        val cacheKey = "$url ${if (sendKey) keyHash else "anonymous"}"
        val cached = settings.cache?.get(cacheKey)
        if (cached != null && isFresh(cached)) return Fetched(cached.body, ResponseMeta())
        var retries = 0
        while (true) {
            val response =
                try {
                    send(url, sendKey, cached?.etag)
                } catch (failure: Failure) {
                    if (!failure.retryable || retries >= settings.maxRetries) throw failure.exception
                    retries++
                    delay(backoff(retries))
                    continue
                }
            val limits = rateLimitOf(response)
            if (limits != null) rateLimit.value = limits
            noteDeprecation(operation, response)
            val meta = ResponseMeta(limits, response.header(REQUEST_ID), revalidated = false)
            when {
                response.status in SUCCESS -> {
                    store(cacheKey, response)
                    return Fetched(response.body, meta)
                }

                response.status == NOT_MODIFIED -> {
                    return revalidated(cacheKey, cached, response, meta)
                }

                else -> {
                    val error = errorOf(response, limits)
                    val wait =
                        if (response.status in RETRYABLE &&
                            retries < settings.maxRetries
                        ) {
                            retryDelay(response, retries + 1)
                        } else {
                            null
                        }
                    if (wait == null) throw error
                    retries++
                    delay(wait)
                }
            }
        }
    }

    private fun isFresh(entry: CachedResponse): Boolean {
        val until = entry.freshUntilEpochMillis ?: return false
        return settings.clock() < until
    }

    /** A 304: the cached body, with the fresh headers' ETag and freshness (BEHAVIOR.md section 10). */
    private suspend fun revalidated(
        cacheKey: String,
        cached: CachedResponse?,
        response: HttpResponse,
        meta: ResponseMeta,
    ): Fetched {
        val entry = cached ?: throw ResponseFormatException("304 Not Modified for a request the SDK has not cached")
        settings.cache?.put(
            cacheKey,
            CachedResponse(response.header(ETAG) ?: entry.etag, entry.body, freshUntil(response)),
        )
        return Fetched(entry.body, meta.copy(revalidated = true))
    }

    /** One attempt: the request plus any same-host or cross-host redirects. */
    private suspend fun send(
        url: String,
        sendKey: Boolean,
        etag: String?,
    ): HttpResponse {
        var current = url
        var withKey = sendKey
        repeat(MAX_REDIRECTS + 1) {
            val headers =
                buildList {
                    add("User-Agent" to settings.userAgent)
                    add("Accept" to "application/json")
                    if (withKey) add(HttpRequest.API_KEY_HEADER to requireNotNull(settings.apiKey))
                    if (etag != null) add("If-None-Match" to etag)
                }
            val response = attempt(HttpRequest(current, headers))
            val location = response.header("Location")
            if (response.status !in REDIRECTS || location == null) return response
            current = resolve(current, location)
            // The key never leaves the API's own origin (BEHAVIOR.md section 3.3).
            if (origin(current) != baseOrigin) withKey = false
        }
        throw Failure(ConnectionException("more than $MAX_REDIRECTS redirects"), retryable = false)
    }

    // A custom transport may throw anything; it is reported, never swallowed.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun attempt(request: HttpRequest): HttpResponse {
        val response =
            try {
                withTimeoutOrNull(settings.timeout) { settings.transport.execute(request) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BodyTooLargeException) {
                throw Failure(ConnectionException(e.message ?: "response body too large", e), retryable = false)
            } catch (e: IOException) {
                val tls = isTlsVerificationFailure(e)
                val what = if (tls) "TLS verification failed" else "connection failed"
                throw Failure(ConnectionException("$what: ${e.javaClass.simpleName}", e), retryable = !tls)
            } catch (e: Exception) {
                throw Failure(
                    ConnectionException("the transport failed: ${e.javaClass.simpleName}", e),
                    retryable = false,
                )
            } ?: throw Failure(ConnectionException("no response within ${settings.timeout}"), retryable = true)
        if (response.body.size > HttpTransport.MAX_BODY_BYTES) {
            throw Failure(ConnectionException("response body exceeds 64 MiB"), retryable = false)
        }
        return response
    }

    private suspend fun store(
        key: String,
        response: HttpResponse,
    ) {
        val cache = settings.cache ?: return
        val etag = response.header(ETAG)
        val noStore = cacheDirectives(response).containsKey("no-store")
        if (etag == null || noStore) {
            cache.remove(key)
        } else {
            cache.put(key, CachedResponse(etag, response.body, freshUntil(response)))
        }
    }

    /**
     * The SDK is a private cache (RFC 9111): `max-age` only, never `s-maxage` or
     * `stale-while-revalidate`, which are for the edge.
     */
    private fun freshUntil(response: HttpResponse): Long? {
        val directives = cacheDirectives(response)
        if (directives.containsKey("no-cache") || directives.containsKey("no-store")) return null
        val maxAge = directives["max-age"]?.toLongOrNull() ?: return null
        return if (maxAge > 0) settings.clock() + maxAge * MILLIS else null
    }

    private fun noteDeprecation(
        operation: Operation,
        response: HttpResponse,
    ) {
        val deprecation = response.header("Deprecation")
        val sunset = response.header("Sunset")
        if (deprecation == null && sunset == null) return
        if (!Deprecations.firstFor(operation.id)) return
        val link =
            response.headers["Link"].orEmpty().flatMap { it.split(',') }.firstOrNull {
                "rel=\"deprecation\"" in
                    it
            }
        val url = link?.substringAfter('<', "")?.substringBefore('>')?.takeIf { it.isNotEmpty() }
        settings.onDeprecation(DeprecationNotice(operation.id, deprecation, sunset, url))
    }

    private fun backoff(retry: Int): Duration {
        val cap = min(MAX_BACKOFF_SECONDS, BASE_BACKOFF_SECONDS * 2.0.pow(retry - 1))
        return (settings.random() * cap * MILLIS).milliseconds
    }

    /** The wait before retry [retry], or `null` when the server asked for more than 60 s. */
    private fun retryDelay(
        response: HttpResponse,
        retry: Int,
    ): Duration? {
        if (response.status in HONOURS_RETRY_AFTER) {
            val after = retryAfter(response)
            if (after != null) return if (after > MAX_RETRY_AFTER) null else after
        }
        return backoff(retry)
    }

    private fun retryAfter(response: HttpResponse): Duration? {
        val value = response.header("Retry-After")?.trim() ?: return null
        value.toLongOrNull()?.let { return it.coerceAtLeast(0).seconds }
        return try {
            val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
            format.timeZone = TimeZone.getTimeZone("GMT")
            val at = format.parse(value)?.time ?: return null
            (at - settings.clock()).coerceAtLeast(0).milliseconds
        } catch (_: ParseException) {
            null
        }
    }

    private fun errorOf(
        response: HttpResponse,
        limits: RateLimit?,
    ): ApiException {
        val body = parseError(response.body)
        val code = body?.code
        val status = response.status
        val message = body?.message?.takeIf { it.isNotBlank() } ?: "HTTP $status"
        val requestId = body?.requestId ?: response.header(REQUEST_ID)
        val doc = body?.docUrl
        val param = body?.param
        return when (Category.of(code, status)) {
            Category.Authentication -> {
                AuthenticationException(status, code, message, requestId, limits, doc, param)
            }

            Category.Permission -> {
                PermissionException(status, code, message, requestId, limits, doc, param, body?.nearestLocation)
            }

            Category.NotFound -> {
                NotFoundException(status, code, message, requestId, limits, doc, param)
            }

            Category.OutsideCoverage -> {
                OutsideCoverageException(status, code, message, requestId, limits, doc, param)
            }

            Category.RateLimited -> {
                RateLimitException(status, code, message, requestId, limits, retryAfter(response), doc, param)
            }

            Category.Unavailable -> {
                UnavailableException(status, code, message, requestId, limits, doc, param)
            }

            Category.InvalidRequest -> {
                InvalidRequestException(status, code, message, requestId, limits, doc, param)
            }
        }
    }

    /** The JSON error envelope (`ErrorResponse`). */
    private class ErrorBody(
        val code: String,
        val message: String,
        val requestId: String?,
        val docUrl: String?,
        val param: String?,
        val nearestLocation: NearestLocation?,
    )

    private fun parseError(body: ByteArray): ErrorBody? {
        if (body.isEmpty()) return null
        return try {
            val obj = Json.parseToJsonElement(body.toString(Charsets.UTF_8)) as? JsonObject ?: return null

            fun text(key: String) = (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val code = text("error") ?: return null
            ErrorBody(
                code = code,
                message = (obj["message"] as? JsonPrimitive)?.content.orEmpty(),
                requestId = text("request_id"),
                docUrl = text("doc_url"),
                param = text("param"),
                nearestLocation = nearestLocation(obj["nearest_location"]),
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** A malformed `nearest_location` never hides the error itself. */
    private fun nearestLocation(value: JsonElement?): NearestLocation? =
        (value as? JsonObject)?.let {
            try {
                EnsembleWeather.JSON.decodeFromJsonElement(NearestLocation.serializer(), it)
            } catch (_: SerializationException) {
                null
            }
        }

    /** A failed attempt and whether it may be retried. */
    private class Failure(
        val exception: ConnectionException,
        val retryable: Boolean,
    ) : Exception(exception.message, exception)

    /** BEHAVIOR.md section 8: by code first, by status when the code is unknown or absent. */
    internal enum class Category {
        Authentication,
        Permission,
        NotFound,
        InvalidRequest,
        OutsideCoverage,
        RateLimited,
        Unavailable,
        ;

        companion object {
            private val byCode =
                mapOf(
                    "INVALID_API_KEY" to Authentication,
                    "UNAUTHORIZED" to Authentication,
                    "PRO_REQUIRED" to Permission,
                    "LOCATION_NOT_FOUND" to NotFound,
                    "NOT_FOUND" to NotFound,
                    "BAD_REQUEST" to InvalidRequest,
                    "INVALID_COORDINATES" to InvalidRequest,
                    "INVALID_ELEVATION" to InvalidRequest,
                    "ELEVATION_REQUIRED" to InvalidRequest,
                    "OUTSIDE_COVERAGE" to OutsideCoverage,
                    "TOO_MANY_REQUESTS" to RateLimited,
                    "FORECAST_UNAVAILABLE" to Unavailable,
                    "TIMEOUT" to Unavailable,
                    "INTERNAL_ERROR" to Unavailable,
                    "DATABASE_ERROR" to Unavailable,
                    "AUTH_UNAVAILABLE" to Unavailable,
                )

            @Suppress("MagicNumber")
            fun of(
                code: String?,
                status: Int,
            ): Category =
                code?.let(byCode::get) ?: when (status) {
                    401 -> Authentication
                    403 -> Permission
                    404, 410 -> NotFound
                    429 -> RateLimited
                    in 500..599 -> Unavailable
                    else -> if (status in 400..499) InvalidRequest else Unavailable
                }
        }
    }

    companion object {
        private val SUCCESS = 200..299
        private const val NOT_MODIFIED = 304
        private val HONOURS_RETRY_AFTER = setOf(429, 503, 504)
        private val RETRYABLE = setOf(429, 502, 503, 504)
        private val REDIRECTS = setOf(301, 302, 307, 308)
        private const val MAX_REDIRECTS = 5
        private const val BASE_BACKOFF_SECONDS = 0.5
        private const val MAX_BACKOFF_SECONDS = 8.0
        private val MAX_RETRY_AFTER = 60.seconds
        private const val MILLIS = 1000L
        private const val ETAG = "ETag"
        private const val REQUEST_ID = "X-Request-Id"

        fun rateLimitOf(response: HttpResponse): RateLimit? {
            val limit = response.header("RateLimit-Limit")
            val remaining = response.header("RateLimit-Remaining")
            val reset = response.header("RateLimit-Reset")
            val policy = response.header("RateLimit-Policy")
            if (listOf(limit, remaining, reset, policy).all { it == null }) return null
            return RateLimit(
                limit?.trim()?.toLongOrNull(),
                remaining?.trim()?.toLongOrNull(),
                reset?.trim()?.toLongOrNull(),
                policy,
            )
        }

        fun cacheDirectives(response: HttpResponse): Map<String, String> =
            response.headers["Cache-Control"]
                .orEmpty()
                .flatMap { it.split(',') }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .associate {
                    it.substringBefore('=').trim().lowercase(Locale.ROOT) to it.substringAfter('=', "").trim().trim('"')
                }

        fun origin(url: String): String =
            try {
                val uri = URI(url)
                val port = if (uri.port == -1) defaultPort(uri.scheme) else uri.port
                "${uri.scheme?.lowercase(Locale.ROOT)}://${uri.host?.lowercase(Locale.ROOT)}:$port"
            } catch (_: URISyntaxException) {
                url
            }

        private fun defaultPort(scheme: String?): Int =
            when (scheme?.lowercase(Locale.ROOT)) {
                "http" -> HTTP_PORT
                "https" -> HTTPS_PORT
                else -> -1
            }

        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443

        fun resolve(
            base: String,
            location: String,
        ): String =
            try {
                URI(base).resolve(URI(location)).toString()
            } catch (e: URISyntaxException) {
                throw ResponseFormatException("a redirect named an invalid Location", e)
            }

        fun sha256(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { (it.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(2, '0') }
        }

        private const val BYTE_MASK = 0xFF
        private const val HEX_RADIX = 16

        fun isTlsVerificationFailure(error: Throwable): Boolean =
            generateSequence(error) { it.cause }.take(DEPTH).any {
                it is SSLHandshakeException || it is SSLPeerUnverifiedException || it is CertificateException
            }

        private const val DEPTH = 8
    }
}

/** Deprecation notices go out once per operation per process (BEHAVIOR.md section 11). */
internal object Deprecations {
    private val seen = ConcurrentHashMap.newKeySet<String>()

    fun firstFor(operation: String): Boolean = seen.add(operation)

    /** For tests: forget every notice sent. */
    fun reset() = seen.clear()
}
