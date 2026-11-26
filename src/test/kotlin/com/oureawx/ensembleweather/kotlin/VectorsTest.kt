// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import com.oureawx.ensembleweather.kotlin.internal.Deprecations
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.lang.reflect.Modifier
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Runs every shared vector (spec/vectors, format in BEHAVIOR.md section 13) through a
 * mock [HttpTransport], on kotlinx-coroutines-test virtual time so retry delays are
 * measured, not slept. The argument mapping in [perform] is the only Kotlin-specific part.
 * A vector this runner cannot run fails; none is skipped.
 */
class VectorsTest {
    @Test
    fun `every vector in the pinned spec is run`() {
        assertEquals(EXPECTED_VECTORS, vectorFiles().size, "spec/vectors should hold $EXPECTED_VECTORS cases")
    }

    @TestFactory
    fun vectors(): List<DynamicTest> =
        vectorFiles().map { file ->
            val case = Json.parseToJsonElement(file.readText()).jsonObject
            DynamicTest.dynamicTest(case.string("name")) { run(case) }
        }

    private fun run(case: JsonObject) =
        runTest {
            Deprecations.reset()
            val transport = MockTransport(this, case["responses"]?.jsonArray.orEmpty().map { it.jsonObject })
            val warnings = mutableListOf<DeprecationNotice>()
            val client = client(case["client"]?.jsonObject ?: JsonObject(emptyMap()), transport, warnings, this)
            val calls = case["calls"]?.jsonArray?.map { it.jsonObject } ?: listOf(case.getValue("call").jsonObject)
            val outcomes = calls.map { call -> outcome { perform(client, call) } }
            val expect = case["expect"]?.jsonObject ?: JsonObject(emptyMap())

            checkRequests(case, transport.requests)
            expect["requests"]?.let { assertEquals(it.jsonPrimitive.int, transport.requests.size, "requests made") }
            expect["delays_s"]?.let { expected ->
                val delays = transport.times.zipWithNext { a, b -> (b - a) / MILLIS }
                assertEquals(expected.jsonArray.map { it.jsonPrimitive.doubleOrNull }, delays, "retry delays (s)")
            }
            expect["warnings"]?.let { expected ->
                assertEquals(expected.jsonArray.size, warnings.size, "deprecation warnings: $warnings")
                expected.jsonArray.zip(warnings).forEach { (w, notice) ->
                    val needle = w.jsonObject.string("contains")
                    assertTrue(needle in notice.describe(), "warning '${notice.describe()}' should contain $needle")
                }
            }
            val results =
                expect["results"]?.jsonArray?.map { it.jsonObject } ?: listOfNotNull(expect["result"]?.jsonObject)
            results.forEachIndexed { i, expected -> checkResult(expected, outcomes[i], client) }
            expect["error"]?.let { checkError(it.jsonObject, outcomes.last()) }
            if (expect["error"] ==
                null
            ) {
                outcomes.forEach { it.error?.let { e -> throw AssertionError("unexpected $e", e) } }
            }
            expect["items"]?.let { expected ->
                @Suppress("UNCHECKED_CAST")
                val items = outcomes.single().value as List<PublicCatalogLocation>
                assertEquals(expected.jsonArray.map { it.jsonPrimitive.content }, items.map { it.slug })
            }
        }

    // ---- The client and the call mapping (the only per-language code) ---------------------

    private fun client(
        config: JsonObject,
        transport: MockTransport,
        warnings: MutableList<DeprecationNotice>,
        scope: TestScope,
    ): Client =
        Client {
            val env = config["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content }.orEmpty()
            environment = { env[it] }
            config["api_key"]?.let { apiKey = it.jsonPrimitive.content }
            config["base_url"]?.let { baseUrl = it.jsonPrimitive.content }
            config["max_retries"]?.let { maxRetries = it.jsonPrimitive.int }
            config["cache"]?.let { if (it.jsonPrimitive.booleanOrNull == false) cache = null }
            config["send_key_on_open_routes"]?.let { sendKeyOnOpenRoutes = it.jsonPrimitive.booleanOrNull == true }
            this.transport = transport
            onDeprecation = { warnings += it }
            // Full jitter seeded to its upper bound (BEHAVIOR.md section 13).
            random = { 1.0 }
            clock = { scope.testScheduler.currentTime }
        }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private suspend fun perform(
        base: Client,
        call: JsonObject,
    ): Any {
        val client = call["with_api_key"]?.let { key -> base.config { apiKey = key.jsonPrimitive.content } } ?: base
        val args = call["args"]?.jsonObject ?: JsonObject(emptyMap())
        val location = args["location"]?.jsonPrimitive?.content
        return when (val operation = call.string("operation")) {
            "getLocationForecast" -> {
                val options: LocationForecastOptions.() -> Unit = {
                    forecastOptions(args)
                    args["elevations"]?.let { elevations = strings(it) }
                }
                if (location != null) {
                    client.getForecast(location, options)
                } else {
                    client.getForecast(args.string("state"), args.string("slug"), options)
                }
            }

            "getPointForecast" -> {
                client.getPointForecast(
                    args.double("lat"),
                    args.double("lon"),
                    args["elevation_m"]?.jsonPrimitive?.doubleOrNull,
                ) {
                    forecastOptions(args)
                }
            }

            "getCurrentConditions" -> {
                client.getCurrent(args.string("location"))
            }

            "getCurrentByCoordinates" -> {
                client.getPointCurrent(args.double("lat"), args.double("lon"))
            }

            "getLocationAlerts" -> {
                client.getAlerts(args.string("location"))
            }

            "getAlertsByCoordinates" -> {
                client.getPointAlerts(args.double("lat"), args.double("lon"))
            }

            "getNearbyLocations" -> {
                client.getNearby(args.string("location"))
            }

            "getNearbyByCoordinates" -> {
                client.getPointNearby(args.double("lat"), args.double("lon"))
            }

            "getLocations", "getAllLocations" -> {
                client.getLocationsPage(args["state"]?.jsonPrimitive?.content) {
                    args["limit"]?.let { limit = it.jsonPrimitive.int }
                }
            }

            "getAvailableRuns" -> {
                client.getRuns(limit = args["limit"]?.jsonPrimitive?.int)
            }

            "searchLocations" -> {
                client.searchLocations(args.string("q")) {
                    args["sort"]?.let { sort = ExploreMetric(it.jsonPrimitive.content) }
                    args["order"]?.let { order = sortOrder(it.jsonPrimitive.content) }
                    args["near"]?.let { near = latLon(it.jsonObject) }
                    args["radius_km"]?.let { radiusKm = it.jsonPrimitive.doubleOrNull }
                    args["state"]?.let { state = it.jsonPrimitive.content }
                    args["units"]?.let { units = Units(it.jsonPrimitive.content) }
                }
            }

            "explore" -> {
                client.explore(ExploreMetric(args.string("metric"))) {
                    args["order"]?.let { order = sortOrder(it.jsonPrimitive.content) }
                    args["type"]?.let { type = LocationType(it.jsonPrimitive.content) }
                    args["state"]?.let { state = it.jsonPrimitive.content }
                    args["near"]?.let { near = latLon(it.jsonObject) }
                    args["radius_km"]?.let { radiusKm = it.jsonPrimitive.doubleOrNull }
                    args["elevation_min"]?.let { elevationMin = it.jsonPrimitive.doubleOrNull }
                    args["elevation_max"]?.let { elevationMax = it.jsonPrimitive.doubleOrNull }
                    args["points"]?.let { points = explorePoints(it.jsonPrimitive.content) }
                    args["limit"]?.let { limit = it.jsonPrimitive.int }
                    args["cursor"]?.let { cursor = it.jsonPrimitive.content }
                    args["units"]?.let { units = Units(it.jsonPrimitive.content) }
                }
            }

            "getComposition" -> {
                client.getComposition()
            }

            "getLocationSeason" -> {
                client.getSeason(args.string("location")) {
                    args["water_year"]?.let { waterYear = it.jsonPrimitive.int }
                    args["include"]?.let { include = strings(it) }
                }
            }

            "getLocationSnowTrend" -> {
                client.getSnowTrend(args.string("location")) {
                    args["elevation"]?.let { elevation = it.jsonPrimitive.content }
                    args["units"]?.let { units = Units(it.jsonPrimitive.content) }
                }
            }

            "getLocationSnowpack" -> {
                client.getSnowpack(args.string("location"))
            }

            "getLocationSnowpackPlume" -> {
                client.getSnowpackPlume(args.string("location"))
            }

            "getAlertOutlines" -> {
                client.getAlertOutlines {
                    args["classes"]?.let { c -> classes = strings(c).map(::AlertClass) }
                    args["bbox"]?.jsonObject?.let {
                        bbox = BoundingBox(it.double("west"), it.double("south"), it.double("east"), it.double("north"))
                    }
                }
            }

            "getAvalancheZones" -> {
                client.getAvalancheZones()
            }

            "getCatalogLocation" -> {
                client.getCatalogLocation(args.string("slug")) {
                    args["include"]?.let { include = strings(it) }
                }
            }

            "getCatalogStation" -> {
                client.getCatalogStation(args.string("slug"))
            }

            "getAttribution" -> {
                client.getAttribution()
            }

            "listCatalogLocations" -> {
                if (args["iterate"]?.jsonPrimitive?.booleanOrNull == true) {
                    client.catalogLocations().toList()
                } else {
                    client.getCatalogLocationsPage()
                }
            }

            else -> {
                fail("the runner has no mapping for operation $operation")
            }
        }
    }

    private fun ForecastOptions.forecastOptions(args: JsonObject) {
        args["units"]?.let { units = Units(it.jsonPrimitive.content) }
        args["max_hours"]?.let { maxHours = it.jsonPrimitive.int }
        args["members"]?.let { members = members(it.jsonPrimitive) }
        args["models"]?.let { models = it.jsonPrimitive.booleanOrNull }
        args["nowcast"]?.let { nowcast = it.jsonPrimitive.booleanOrNull }
        args["include_past"]?.let { includePast = it.jsonPrimitive.booleanOrNull }
        args["daily"]?.let { daily = it.jsonPrimitive.booleanOrNull }
        args["hourly"]?.let { hourly = it.jsonPrimitive.booleanOrNull }
        args["sources"]?.let { sources = strings(it) }
        args["exclude_sources"]?.let { excludeSources = strings(it) }
        args["include"]?.let { include = strings(it) }
        args["prob"]?.jsonArray?.forEach { term ->
            val t = term.jsonObject
            val comparison = Comparison(t.string("op"))
            val threshold = threshold(t.getValue("threshold").jsonObject)
            var p = ProbabilityTerm(Variable(t.string("variable")), comparison, threshold)
            t["window"]?.let { p = p within window(it.jsonObject) }
            t["aggregation"]?.let { p = p using Aggregation(it.jsonPrimitive.content) }
            chance(p)
        }
        args["totals"]?.jsonArray?.forEach { term ->
            val t = term.jsonObject
            total(Variable(t.string("variable")) totalWithin window(t.getValue("window").jsonObject))
        }
    }

    /** `true`, `false` or `"plume"` (BEHAVIOR.md section 3). */
    private fun members(value: JsonPrimitive): Members =
        when {
            value.booleanOrNull == true -> Members.Hourly
            value.booleanOrNull == false -> Members.Off
            value.content == "plume" -> Members.Plume
            else -> fail("members takes true, false or plume, not $value")
        }

    private fun sortOrder(value: String): SortOrder = SortOrder.entries.single { it.wire == value }

    private fun explorePoints(value: String): ExplorePoints = ExplorePoints.entries.single { it.wire == value }

    private fun latLon(near: JsonObject): LatLon = LatLon(near.double("lat"), near.double("lon"))

    /** A list of strings, or one string (`"elevations": "all"`). */
    private fun strings(value: JsonElement): List<String> =
        (value as? JsonArray)?.map { it.jsonPrimitive.content } ?: listOf(value.jsonPrimitive.content)

    private fun threshold(t: JsonObject): Threshold {
        val unit =
            when (val u = t.string("unit")) {
                "mm" -> ThresholdUnit.Millimeters
                "in" -> ThresholdUnit.Inches
                "degC" -> ThresholdUnit.Celsius
                "degF" -> ThresholdUnit.Fahrenheit
                "K" -> ThresholdUnit.Kelvin
                "m" -> ThresholdUnit.Meters
                "ft" -> ThresholdUnit.Feet
                "m/s" -> ThresholdUnit.MetersPerSecond
                "mph" -> ThresholdUnit.MilesPerHour
                else -> fail("unknown threshold unit $u")
            }
        return Threshold(t.double("value"), unit)
    }

    private fun window(w: JsonObject): Window =
        w["hours"]?.let { Window.next(it.jsonPrimitive.int) }
            ?: Window(w.getValue("start_hours").jsonPrimitive.int, w.getValue("end_hours").jsonPrimitive.int)

    // ---- Requests ---------------------------------------------------------------------

    private fun checkRequests(
        case: JsonObject,
        sent: List<HttpRequest>,
    ) {
        val single = case["expect_request"]
        val expected: List<JsonObject> =
            when {
                case.containsKey("expect_requests") -> case.getValue("expect_requests").jsonArray.map { it.jsonObject }
                single == null || single is JsonNull -> emptyList()
                else -> listOf(single.jsonObject)
            }
        if (case.containsKey("expect_request") && single is JsonNull) assertTrue(sent.isEmpty(), "nothing may be sent")
        assertTrue(sent.size >= expected.size, "expected ${expected.size} requests, sent ${sent.size}")
        expected.forEachIndexed { i, e -> checkRequest(e, sent[i], i) }
    }

    private fun checkRequest(
        e: JsonObject,
        request: HttpRequest,
        index: Int,
    ) {
        val label = "request $index (${request.url})"
        val uri = URI(request.url)
        e["method"]?.let { assertEquals(it.jsonPrimitive.content, request.method, label) }
        e["path"]?.let { assertEquals(it.jsonPrimitive.content, uri.rawPath, "$label path") }
        e["url_prefix"]?.let { assertTrue(request.url.startsWith(it.jsonPrimitive.content), "$label url_prefix") }
        e["query_string"]?.let { assertEquals(it.jsonPrimitive.content, uri.rawQuery.orEmpty(), "$label query string") }
        val query = decodeQuery(uri.rawQuery.orEmpty())
        e["query"]?.jsonObject?.forEach { (name, value) ->
            assertEquals(value.jsonPrimitive.content, query[name], "$label query $name")
        }
        e["query_absent"]?.jsonArray?.forEach {
            assertFalse(it.jsonPrimitive.content in query, "$label sends ${it.jsonPrimitive.content}")
        }
        e["headers"]?.jsonObject?.forEach { (name, value) ->
            assertEquals(value.jsonPrimitive.content, request.header(name), "$label header $name")
        }
        e["headers_absent"]?.jsonArray?.forEach {
            assertNull(request.header(it.jsonPrimitive.content), "$label must not send ${it.jsonPrimitive.content}")
        }
        e["headers_match"]?.jsonObject?.forEach { (name, pattern) ->
            val value = assertNotNull(request.header(name), "$label header $name")
            assertTrue(Regex(pattern.jsonPrimitive.content).containsMatchIn(value), "$label header $name = $value")
        }
    }

    private fun decodeQuery(raw: String): Map<String, String> =
        raw.split('&').filter { it.isNotEmpty() }.associate {
            percentDecode(it.substringBefore('=')) to percentDecode(it.substringAfter('=', ""))
        }

    private fun percentDecode(s: String): String {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            if (s[i] == '%') {
                out.write(s.substring(i + 1, i + 3).toInt(HEX))
                i += 3
            } else {
                out.write(s[i].code)
                i++
            }
        }
        return out.toString(Charsets.UTF_8.name())
    }

    // ---- Results and errors -----------------------------------------------------------

    private class Outcome(
        val value: Any?,
        val error: Throwable?,
    )

    private suspend fun outcome(block: suspend () -> Any): Outcome =
        try {
            Outcome(block(), null)
        } catch (e: OureaException) {
            Outcome(null, e)
        }

    private fun checkResult(
        expected: JsonObject,
        outcome: Outcome,
        client: Client,
    ) {
        outcome.error?.let { throw AssertionError("expected a result, got $it", it) }
        val value = assertNotNull(outcome.value)
        val response = value as ApiResponse
        for ((path, want) in expected) {
            when {
                path == "revalidated" -> {
                    assertEquals(want.jsonPrimitive.booleanOrNull, response.meta.revalidated, path)
                }

                path.startsWith("rate_limit.") -> {
                    val limits = response.meta.rateLimit ?: client.rateLimit.value
                    val got =
                        when (path.removePrefix("rate_limit.")) {
                            "limit" -> limits?.limit
                            "remaining" -> limits?.remaining
                            "reset" -> limits?.resetSeconds
                            else -> fail("unknown rate_limit field $path")
                        }
                    assertEquals(want.jsonPrimitive.longOrNull, got, path)
                }

                want is JsonObject && want.containsKey("unknown") -> {
                    checkUnknownEnum(path, value, want.string("unknown"))
                }

                else -> {
                    assertMatches(want, navigate(value, path), path)
                }
            }
        }
    }

    /** `{"unknown": raw}`: the open enum at [path] keeps [raw] and is not a known value. */
    private fun checkUnknownEnum(
        path: String,
        value: Any,
        raw: String,
    ) {
        assertEquals(raw, navigate(value, path)?.toString(), path)
        val known =
            when (path) {
                "location.type" -> (value as ForecastResponse).location.type?.isKnown == true
                "units" -> (value as ForecastResponse).units.isKnown
                else -> fail("the runner has no typed accessor for the open enum at $path")
            }
        assertFalse(known, "$path should be the unknown case")
    }

    private fun checkError(
        expected: JsonObject,
        outcome: Outcome,
    ) {
        val error = assertNotNull(outcome.error, "expected an error, got ${outcome.value}")
        assertEquals(expected.string("category"), category(error), "error category of $error")
        val api = error as? ApiException
        expected["code"]?.let {
            assertEquals(
                (it as? JsonPrimitive)
                    ?.takeIf { p ->
                        p.isString
                    }?.content,
                api?.code,
                "code",
            )
        }
        expected["status"]?.let { assertEquals(it.jsonPrimitive.int, api?.status, "status") }
        expected["retry_after"]?.let {
            assertEquals(
                it.jsonPrimitive.longOrNull,
                (error as RateLimitException).retryAfter?.inWholeSeconds,
                "retry_after",
            )
        }
        expected["string_excludes"]?.let {
            val secret = it.jsonPrimitive.content
            assertFalse(secret in error.message.orEmpty(), "message leaks the key")
            assertFalse(secret in error.toString(), "toString leaks the key")
        }
    }

    private fun category(error: Throwable): String =
        when (error) {
            is AuthenticationException -> "authentication"
            is PermissionException -> "permission"
            is NotFoundException -> "not_found"
            is InvalidRequestException -> "invalid_request"
            is OutsideCoverageException -> "outside_coverage"
            is RateLimitException -> "rate_limited"
            is UnavailableException -> "unavailable"
            is ConnectionException -> "connection"
            is UsageException -> "usage"
            else -> "other: ${error::class.simpleName}"
        }

    // ---- Paths over the models ------------------------------------------------------------

    /**
     * Follows a result path (BEHAVIOR.md section 13) over the decoded models, by the SDK names of section 3.0
     * in snake_case: each segment is the Kotlin property in camelCase, read through its getter. Raw JSON (GeoJSON
     * coordinates, free-form fields) and maps are followed by key.
     */
    private fun navigate(
        root: Any,
        path: String,
    ): Any? {
        var node: Any? = root
        for (segment in Regex("""[^.\[\]]+|\[\d+]""").findAll(path).map { it.value }) {
            node =
                when {
                    node == null || node is JsonNull -> return null
                    segment.startsWith("[") -> (node as List<*>).getOrNull(segment.trim('[', ']').toInt())
                    node is Map<*, *> -> node[segment]
                    else -> property(node, segment)
                }
        }
        return node
    }

    private fun property(
        node: Any,
        segment: String,
    ): Any? {
        val name = KOTLIN_NAMES[segment] ?: segment.replace(Regex("_([a-z0-9])")) { it.groupValues[1].uppercase() }
        val getter = "get" + name.replaceFirstChar { it.uppercase() }
        // A value-class property's getter is mangled (`getUnits-<hash>`) and returns the underlying string; the
        // static `get...$annotations` stubs of annotated properties are not getters.
        val method =
            node.javaClass.methods.firstOrNull {
                it.parameterCount == 0 &&
                    !Modifier.isStatic(it.modifiers) &&
                    '$' !in it.name &&
                    (it.name == getter || it.name == name || it.name.startsWith("$getter-"))
            } ?: fail("${node.javaClass.simpleName} has no property $name (path segment $segment)")
        return method.invoke(node)
    }

    /** A primitive expected at a path: `null` is absent, a string matches as an instant where both parse as one. */
    private fun assertMatches(
        want: JsonElement,
        got: Any?,
        path: String,
    ) {
        if (want is JsonNull) {
            assertTrue(got == null || got is JsonNull, "$path should be absent, got $got")
            return
        }
        val w = want.jsonPrimitive
        assertNotNull(got, "$path is absent")
        val g =
            (got as? JsonPrimitive)?.let { if (it.isString) it.content else it.booleanOrNull ?: it.doubleOrNull } ?: got
        when {
            w.isString -> {
                val text = g.toString()
                if (text != w.content) assertEquals(instant(w.content), instant(text), path)
            }

            w.booleanOrNull != null -> {
                assertEquals(w.booleanOrNull, g as? Boolean, path)
            }

            else -> {
                assertEquals(w.doubleOrNull, (g as? Number)?.toDouble(), path)
            }
        }
    }

    private fun instant(text: String): Instant? =
        try {
            OffsetDateTime.parse(text).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.double(key: String): Double =
        getValue(key).jsonPrimitive.doubleOrNull ?: fail("$key is not a number")

    /** Serves a vector's responses in order and records each request and its virtual time. */
    private class MockTransport(
        private val scope: TestScope,
        private val responses: List<JsonObject>,
    ) : HttpTransport {
        val requests = mutableListOf<HttpRequest>()
        val times = mutableListOf<Long>()

        override suspend fun execute(request: HttpRequest): HttpResponse {
            requests += request
            times += scope.testScheduler.currentTime
            val r =
                responses.getOrNull(requests.size - 1)
                    ?: throw AssertionError("no canned response for request ${requests.size}")
            val headers = r["headers"]?.jsonObject?.mapValues { listOf(it.value.jsonPrimitive.content) }.orEmpty()
            val body =
                r["body_file"]?.let { File(VECTORS, it.jsonPrimitive.content).readBytes() }
                    ?: r["body"]?.jsonPrimitive?.content?.toByteArray()
                    ?: ByteArray(0)
            return HttpResponse(r.getValue("status").jsonPrimitive.int, headers, body)
        }
    }

    companion object {
        private const val EXPECTED_VECTORS = 58

        /** Kotlin names that are not the camelCase of the SDK name, for a language reason. */
        private val KOTLIN_NAMES = mapOf("class" to "alertClass", "default" to "isDefault")
        private const val MILLIS = 1000.0
        private const val HEX = 16
        val SPEC = File(System.getProperty("ourea.specDir") ?: "spec")
        val VECTORS = File(SPEC, "vectors")

        fun vectorFiles(): List<File> =
            VECTORS
                .walkTopDown()
                .filter { it.isFile && it.extension == "json" && it.parentFile.name != "bodies" }
                .sortedBy { it.path }
                .toList()
    }
}
