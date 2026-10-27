// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import java.io.IOException
import java.io.InputStream

/**
 * The HTTP layer: one GET in, one response out. The SDK does everything else
 * itself (canonical URLs, headers, redirects, retries, timeouts, caching), so a
 * transport stays a few lines.
 *
 * On the JVM (11+) the default is built on `java.net.http.HttpClient`. Android has no
 * `java.net.http`, so an Android app passes one built on the HTTP client it already
 * has. With OkHttp:
 *
 * ```kotlin
 * class OkHttpTransport(private val client: OkHttpClient) : HttpTransport {
 *     override suspend fun execute(request: HttpRequest): HttpResponse {
 *         val call = client.newCall(
 *             Request.Builder().url(request.url).apply {
 *                 request.headers.forEach { (name, value) -> header(name, value) }
 *             }.build(),
 *         )
 *         return call.executeAsync().use { response -> // okhttp-coroutines
 *             HttpResponse(
 *                 status = response.code,
 *                 headers = response.headers.toMultimap(),
 *                 body = HttpTransport.readBounded(response.body.byteStream()),
 *             )
 *         }
 *     }
 * }
 *
 * val ourea = EnsembleWeather {
 *     transport = OkHttpTransport(OkHttpClient.Builder().followRedirects(false).build())
 * }
 * ```
 *
 * The contract:
 * - **Never follow redirects.** Return the 3xx as it is; the SDK follows it and drops
 *   the API key when it leaves the API's host.
 * - **Never disable TLS verification.**
 * - Throw an [IOException] when there is no response (the SDK retries it); a
 *   `javax.net.ssl` verification failure is not retried.
 * - Bound the body: read it with [readBounded] (64 MiB).
 * - Be cancellable: when the calling coroutine is cancelled, cancel the call.
 * - **Never log the `X-API-Key` header.** With OkHttp's `HttpLoggingInterceptor`, call
 *   `redactHeader("X-API-Key")`; with Ktor's `Logging`, `sanitizeHeader { it == "X-API-Key" }`.
 *   [HttpRequest.toString] already redacts it.
 */
public fun interface HttpTransport {
    /** Sends [request] and returns the response, whatever its status. */
    public suspend fun execute(request: HttpRequest): HttpResponse

    public companion object {
        /** The largest response body the SDK accepts: 64 MiB. */
        public const val MAX_BODY_BYTES: Int = 64 * 1024 * 1024

        /**
         * Reads [input] to the end, refusing a body over [MAX_BODY_BYTES] with an
         * [IOException] (the SDK reports it as a [ConnectionException] and does not retry).
         */
        @Throws(IOException::class)
        public fun readBounded(input: InputStream): ByteArray =
            input.use { stream ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_BYTES)
                var total = 0L
                while (true) {
                    val n = stream.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > MAX_BODY_BYTES) throw BodyTooLargeException()
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }

        private const val BUFFER_BYTES = 16 * 1024
    }
}

/** A body over [HttpTransport.MAX_BODY_BYTES]. */
internal class BodyTooLargeException : IOException("response body exceeds 64 MiB")

/**
 * One GET request.
 *
 * @property url the absolute URL, already canonical and percent-encoded: send it as it is.
 * @property headers the headers to send, in order (names as the SDK spells them).
 */
public class HttpRequest(
    public val url: String,
    public val headers: List<Pair<String, String>>,
) {
    /** The method: always `GET` (every operation is a read). */
    public val method: String get() = "GET"

    /** The first value of header [name] (case-insensitive), if sent. */
    public fun header(name: String): String? = headers.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second

    /** The method, URL and headers, with `X-API-Key` redacted. */
    override fun toString(): String =
        "HttpRequest(GET $url, headers=" +
            headers.joinToString(prefix = "[", postfix = "]") { (name, value) ->
                "$name: ${if (name.equals(API_KEY_HEADER, ignoreCase = true)) "<redacted>" else value}"
            } + ")"

    internal companion object {
        const val API_KEY_HEADER = "X-API-Key"
    }
}

/**
 * One HTTP response.
 *
 * @property status the HTTP status code.
 * @property headers the response headers by name; names compare case-insensitively.
 * @property body the body bytes (empty for none).
 */
public class HttpResponse(
    public val status: Int,
    headers: Map<String, List<String>>,
    public val body: ByteArray = ByteArray(0),
) {
    public val headers: Map<String, List<String>> =
        java.util.TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER).apply { putAll(headers) }

    /** The first value of header [name] (case-insensitive), if present. */
    public fun header(name: String): String? = headers[name]?.firstOrNull()

    override fun toString(): String = "HttpResponse($status, ${body.size} bytes)"
}
