// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import com.oureawx.ensembleweather.kotlin.internal.Deprecations
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import javax.net.ssl.SSLHandshakeException
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class ClientTest {
    private val forecastBody = java.io.File(VectorsTest.VECTORS, "bodies/forecast-metric.json").readBytes()

    private class Recorder(
        private val scope: TestScope,
        private val respond: suspend (HttpRequest, Int) -> HttpResponse,
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        val times = mutableListOf<Long>()

        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests += request
            times += scope.testScheduler.currentTime
            return respond(request, requests.size)
        }
    }

    private fun TestScope.client(
        configure: EnsembleWeather.Builder.() -> Unit = {},
        respond: suspend (HttpRequest, Int) -> HttpResponse,
    ): Pair<EnsembleWeather, Recorder> {
        val recorder = Recorder(this, respond)
        val client =
            EnsembleWeather {
                environment = { null }
                random = { 1.0 }
                clock = { testScheduler.currentTime }
                transport = recorder
                configure()
            }
        return client to recorder
    }

    private fun ok(
        body: ByteArray,
        vararg headers: Pair<String, String>,
    ) = HttpResponse(200, headers.associate { it.first to listOf(it.second) }, body)

    @Test
    fun `the API key never appears in a string form`() =
        runTest {
            val (client, _) = client({ apiKey = "ow_secret_value" }) { _, _ -> ok(forecastBody) }
            assertFalse("ow_secret_value" in client.toString())
            val request =
                HttpRequest(
                    "https://x.test/",
                    listOf(
                        "X-API-Key" to "ow_secret_value",
                        "Accept" to "application/json",
                    ),
                )
            assertFalse("ow_secret_value" in request.toString())
            assertTrue("<redacted>" in request.toString())
        }

    @Test
    fun `the User-Agent names the SDK and the runtime, plus an app token`() =
        runTest {
            val (client, recorder) = client({ userAgentSuffix = "MyApp/1.2" }) { _, _ -> ok(forecastBody) }
            client.getForecast("or/mt-hood-meadows")
            val ua = assertNotNull(recorder.requests.single().header("User-Agent"))
            assertTrue(
                Regex("""^ensemble-weather-kotlin/0\.1\.0 \(Kotlin/[\d.]+; Java/.+\) MyApp/1\.2$""").matches(ua),
                ua,
            )
        }

    @Test
    fun `OUREA_BASE_URL is the default base URL`() =
        runTest {
            val (client, recorder) =
                client({
                    environment = {
                        if (it ==
                            "OUREA_BASE_URL"
                        ) {
                            "http://127.0.0.1:8787/"
                        } else {
                            null
                        }
                    }
                }) { _, _ -> ok(forecastBody) }
            client.getForecast("or/mt-hood-meadows")
            assertEquals("http://127.0.0.1:8787/api/v1/locations/or/mt-hood-meadows", recorder.requests.single().url)
        }

    @Test
    fun `an explicit null key is anonymous even with OUREA_API_KEY set`() =
        runTest {
            val (client, recorder) =
                client({
                    environment = { if (it == "OUREA_API_KEY") "ow_env" else null }
                    apiKey = null
                }) { _, _ -> ok(forecastBody) }
            client.getForecast("or/mt-hood-meadows")
            assertNull(recorder.requests.single().header("X-API-Key"))
        }

    @Test
    fun `a connection error is retried with backoff, then thrown`() =
        runTest {
            val (client, recorder) = client { _, _ -> throw IOException("reset") }
            val error = assertFailsWith<ConnectionException> { client.getForecast("or/mt-hood-meadows") }
            assertEquals(3, recorder.requests.size)
            assertEquals(listOf(500L, 1000L), recorder.times.zipWithNext { a, b -> b - a })
            assertTrue(error.cause is IOException)
        }

    @Test
    fun `a TLS verification failure is not retried`() =
        runTest {
            val (client, recorder) = client { _, _ -> throw SSLHandshakeException("bad certificate") }
            val error = assertFailsWith<ConnectionException> { client.getForecast("or/mt-hood-meadows") }
            assertEquals(1, recorder.requests.size)
            assertTrue("TLS" in error.message.orEmpty())
        }

    @Test
    fun `each attempt is bounded by the timeout on virtual time`() =
        runTest {
            val (client, recorder) = client({ timeout = 5.seconds }) { _, _ -> awaitCancellation() }
            assertFailsWith<ConnectionException> { client.getForecast("or/mt-hood-meadows") }
            assertEquals(3, recorder.requests.size)
            assertEquals(5000L + 500 + 5000 + 1000 + 5000, testScheduler.currentTime)
        }

    @Test
    fun `cancelling the caller cancels the request`() =
        runTest {
            val cancelled = CompletableDeferred<Boolean>()
            val started = CompletableDeferred<Unit>()
            val (client, _) =
                client { _, _ ->
                    started.complete(Unit)
                    try {
                        awaitCancellation()
                    } finally {
                        cancelled.complete(true)
                    }
                }
            val job = launch { client.getForecast("or/mt-hood-meadows") }
            started.await()
            job.cancelAndJoin()
            assertTrue(cancelled.await())
        }

    @Test
    fun `Retry-After as an HTTP date is honoured`() =
        runTest {
            val (client, recorder) =
                client { _, n ->
                    if (n == 1) {
                        // The virtual clock starts at the epoch, so this date is 4 s away.
                        HttpResponse(503, mapOf("Retry-After" to listOf("Thu, 01 Jan 1970 00:00:04 GMT")))
                    } else {
                        ok(forecastBody)
                    }
                }
            client.getForecast("or/mt-hood-meadows")
            assertEquals(listOf(4000L), recorder.times.zipWithNext { a, b -> b - a })
        }

    @Test
    fun `a 429 carries the rate-limit state and Retry-After`() =
        runTest {
            val (client, _) =
                client({ maxRetries = 0 }) { _, _ ->
                    HttpResponse(
                        429,
                        mapOf(
                            "Retry-After" to listOf("30"),
                            "RateLimit-Remaining" to listOf("0"),
                            "X-Request-Id" to listOf("req-1"),
                        ),
                        """{"error":"TOO_MANY_REQUESTS","message":"slow down"}""".toByteArray(),
                    )
                }
            val error = assertFailsWith<RateLimitException> { client.getForecast("or/mt-hood-meadows") }
            assertEquals(30.seconds, error.retryAfter)
            assertEquals(0L, error.rateLimit?.remaining)
            assertEquals("req-1", error.requestId)
            assertEquals(0L, client.rateLimit.value?.remaining)
        }

    @Test
    fun `a body max-age allows serving from the cache without a request`() =
        runTest {
            val (client, recorder) =
                client { _, _ ->
                    ok(
                        forecastBody,
                        "ETag" to "\"a\"",
                        "Cache-Control" to "private, max-age=60",
                    )
                }
            client.getForecast("or/mt-hood-meadows")
            client.getForecast("or/mt-hood-meadows")
            assertEquals(1, recorder.requests.size)
            testScheduler.advanceTimeBy(61.seconds)
            client.getForecast("or/mt-hood-meadows")
            assertEquals(2, recorder.requests.size)
            assertEquals("\"a\"", recorder.requests.last().header("If-None-Match"))
        }

    @Test
    fun `s-maxage is for the edge and never makes a body fresh here`() =
        runTest {
            val (client, recorder) =
                client { _, _ ->
                    ok(
                        forecastBody,
                        "ETag" to "\"a\"",
                        "Cache-Control" to "public, max-age=0, s-maxage=3600, stale-while-revalidate=300",
                    )
                }
            client.getForecast("or/mt-hood-meadows")
            client.getForecast("or/mt-hood-meadows")
            assertEquals(2, recorder.requests.size)
        }

    @Test
    fun `the in-memory cache is bounded by entries and bytes`() =
        runTest {
            val cache = InMemoryResponseCache(maxEntries = 2, maxBytes = 10)
            cache.put("a", CachedResponse("1", ByteArray(4)))
            cache.put("b", CachedResponse("2", ByteArray(4)))
            cache.put("c", CachedResponse("3", ByteArray(4)))
            assertEquals(2, cache.size())
            assertNull(cache.get("a"))
            cache.put("d", CachedResponse("4", ByteArray(11)))
            assertNull(cache.get("d"))
        }

    @Test
    fun `a present probability list missing a term is an error and an absent one is not`() =
        runTest {
            val body = java.io.File(VectorsTest.VECTORS, "bodies/forecast-prob-snow.json").readBytes()
            val (client, _) = client { _, _ -> ok(body) }
            assertFailsWith<ResponseFormatException> {
                client.getForecast("or/mt-hood-meadows") { chance(Variable.Snowfall atLeast 1.mm) }
            }
            val (plain, _) = client { _, _ -> ok(forecastBody) }
            assertNull(plain.getForecast("or/mt-hood-meadows") { chance(Variable.Snowfall atLeast 1.mm) }.probabilities)
        }

    @Test
    fun `a body that is not the documented JSON is a ResponseFormatException`() =
        runTest {
            val (client, _) = client { _, _ -> ok("<html>".toByteArray()) }
            assertFailsWith<ResponseFormatException> { client.getForecast("or/mt-hood-meadows") }
        }

    @Test
    fun `a malformed location is a usage error and sends nothing`() =
        runTest {
            val (client, recorder) = client { _, _ -> ok(forecastBody) }
            assertFailsWith<UsageException> { client.getForecast("mt-hood-meadows") }
            assertFailsWith<UsageException> { client.getForecast("or/a/b") }
            assertFailsWith<UsageException> { client.getForecast("or/x") { units = Units("furlongs") } }
            assertTrue(recorder.requests.isEmpty())
        }

    @Test
    fun `a state name passes through and a two-letter code is lowercased`() =
        runTest {
            val (client, recorder) = client { _, _ -> ok(forecastBody) }
            client.getForecast("New York", "x")
            client.getForecast("NY", "x")
            assertEquals(
                listOf("/api/v1/locations/New%20York/x", "/api/v1/locations/ny/x"),
                recorder.requests.map {
                    URI(it.url).rawPath
                },
            )
        }

    @Test
    fun `too many redirects is a connection error`() =
        runTest {
            val (client, recorder) = client { _, n -> HttpResponse(302, mapOf("Location" to listOf("/r$n"))) }
            assertFailsWith<ConnectionException> { client.getForecast("or/x") }
            assertEquals(6, recorder.requests.size)
        }

    @Test
    fun `the station list repeats capability and follows the cursor`() =
        runTest {
            val pages =
                listOf(
                    """{"items":[${station("a")}],"next_cursor":"c2"}""",
                    """{"items":[${station("b")}],"next_cursor":null}""",
                )
            val (client, recorder) = client { _, n -> ok(pages[n - 1].toByteArray()) }
            val slugs = client.catalogStations { capability = listOf("swe", "air_temp") }.toList().map { it.slug }
            assertEquals(listOf("a", "b"), slugs)
            assertEquals("capability=swe&capability=air_temp", URI(recorder.requests[0].url).rawQuery)
            assertEquals("capability=swe&capability=air_temp&cursor=c2", URI(recorder.requests[1].url).rawQuery)
        }

    private fun station(slug: String) =
        """{"slug":"$slug","state":"or","name":"S","lat":45.0,"lon":-121.0,"elevation_m":1.0,"type":"observation_station","timezone":"America/Los_Angeles"}"""

    @Test
    fun `deprecation is reported once per operation`() =
        runTest {
            Deprecations.reset()
            val notices = mutableListOf<DeprecationNotice>()
            val (client, _) =
                client({ onDeprecation = { notices += it } }) { _, _ ->
                    ok("""{"providers":[]}""".toByteArray(), "Sunset" to "Wed, 01 Dec 2027 00:00:00 GMT")
                }
            client.getAttribution()
            client.getAttribution()
            assertEquals(1, notices.size)
            assertEquals("getAttribution", notices.single().operation)
            assertNull(notices.single().link)
        }

    @Test
    fun `readBounded refuses a body over 64 MiB`() {
        assertContentEquals(byteArrayOf(1, 2), HttpTransport.readBounded(ByteArrayInputStream(byteArrayOf(1, 2))))
        val endless =
            object : InputStream() {
                override fun read(): Int = 0

                override fun read(
                    b: ByteArray,
                    off: Int,
                    len: Int,
                ): Int = len
            }
        assertFailsWith<IOException> { HttpTransport.readBounded(endless) }
    }

    @Test
    fun `quantile ranges exist only for served pairs`() {
        val q = Quantiles(p10 = 1.0, p50 = 2.0, p90 = 3.0)
        assertEquals(QuantileRange(1.0, 3.0), q.p10ToP90)
        assertNull(q.p25ToP75)
        assertEquals(2.0, q.median)
        assertNull(Quantiles().median)
    }

    @Test
    fun `freezing level parses every kind and keeps an unknown one`() {
        fun parse(json: String) = EnsembleWeather.JSON.decodeFromString(FreezingLevel.serializer(), json)
        assertEquals(FreezingLevel.BelowSurface, parse("""{"kind":"below_surface"}"""))
        assertEquals(
            FreezingLevel.Height(1200.0, true),
            parse("""{"kind":"height","height":1200,"above_freezing_layer_aloft":true}"""),
        )
        assertEquals(FreezingLevel.AboveProfile(5000.0), parse("""{"kind":"above_profile","top":5000}"""))
        val unknown = parse("""{"kind":"inverted","x":1}""")
        assertTrue(unknown is FreezingLevel.Unknown && unknown.kind == "inverted")
        val again = EnsembleWeather.JSON.encodeToString(FreezingLevel.serializer(), FreezingLevel.Height(1.0, false))
        assertEquals(FreezingLevel.Height(1.0, false), parse(again))
    }

    @Test
    fun `config shares the cache and swaps the key`() =
        runTest {
            val (client, recorder) =
                client({ apiKey = "ow_a" }) { _, _ -> ok(forecastBody, "ETag" to "\"a\"") }
            val other = client.config { apiKey = "ow_b" }
            client.getForecast("or/x")
            other.getForecast("or/x")
            client.getForecast("or/x")
            assertEquals(listOf("ow_a", "ow_b", "ow_a"), recorder.requests.map { it.header("X-API-Key") })
            assertEquals(listOf(null, null, "\"a\""), recorder.requests.map { it.header("If-None-Match") })
        }

    @Test
    fun `window totals answer in the caller's order`() =
        runTest {
            val body = java.io.File(VectorsTest.VECTORS, "bodies/forecast-totals.json").readBytes()
            val (client, _) = client { _, _ -> ok(body) }
            val f =
                client.getForecast("or/x") {
                    total(Variable.Snowfall totalWithin 240.hours, Variable.Precipitation totalWithin 48.hours)
                }
            assertEquals(listOf(Variable.Snowfall, Variable.Precipitation), f.windowTotals?.map { it.variable })
        }
}
