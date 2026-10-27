// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.kotlin

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.serializer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.hours

/**
 * Spec drift (SDK-STANDARD "Tests"), against the pinned spec/openapi/v1/openapi.yaml:
 * every operation is exposed and sends only parameters its route defines; every model
 * has exactly its schema's fields with the schema's optionality; every response
 * example round-trips through the models (or, for an error, through the error mapping).
 */
@Suppress("UNCHECKED_CAST")
class SpecDriftTest {
    private val spec: Map<String, Any?> =
        Yaml(LoaderOptions().apply { codePointLimit = Int.MAX_VALUE })
            .load(File(VectorsTest.SPEC, "openapi/v1/openapi.yaml").readText())
    private val components = spec.map("components")
    private val schemas = components.map("schemas")

    private val operations: Map<String, Map<String, Any?>> =
        spec
            .map("paths")
            .values
            .flatMap { (it as Map<String, Any?>).values }
            .map { it as Map<String, Any?> }
            .filter { "operationId" in it }
            .associateBy { it["operationId"] as String }

    // ---- Operations -----------------------------------------------------------------------

    /** Every in-scope operation, called with every option it has. */
    private val calls: Map<String, suspend (EnsembleWeather) -> Unit> =
        mapOf(
            "getLocationForecast" to { c ->
                c.getForecast("or/x") {
                    allForecastOptions()
                    elevations = LocationForecastOptions.ALL_ELEVATIONS
                }
            },
            "getPointForecast" to { c -> c.getPointForecast(1.0, 2.0, 3.0) { allForecastOptions() } },
            "getCurrentConditions" to { c -> c.getCurrent("or/x") { allCurrentOptions() } },
            "getCurrentByCoordinates" to { c -> c.getCurrentAt(1.0, 2.0, 3.0) { allCurrentOptions() } },
            "getLocationAlerts" to { c -> c.getAlerts("or/x") },
            "getAlertsByCoordinates" to { c -> c.getAlertsAt(1.0, 2.0) },
            "getNearbyLocations" to { c -> c.getNearby("or/x") { allNearbyOptions() } },
            "getNearbyByCoordinates" to { c -> c.getNearbyAt(1.0, 2.0) { allNearbyOptions() } },
            "searchLocations" to { c ->
                c.searchLocations("hood") {
                    state = "or"
                    county = "Hood River"
                    type = LocationType.SkiArea
                    limit = 5
                }
            },
            "getAllLocations" to { c -> c.getLocationsPage { allListOptions() } },
            "getLocations" to { c -> c.getLocationsPage("or") { allListOptions() } },
            "listCatalogLocations" to { c ->
                c.getCatalogLocationsPage {
                    admin1 = "US-OR"
                    country = "US"
                    type = "SkiArea"
                    template = "Mountain"
                    limit = 5
                    cursor = "c"
                }
            },
            "getCatalogLocation" to { c ->
                c.getCatalogLocation("x") {
                    include = listOf("content")
                    locale = "es"
                }
            },
            "listCatalogStations" to { c ->
                c.getCatalogStationsPage {
                    admin1 = "US-OR"
                    country = "US"
                    network = listOf("Snotel")
                    capability = listOf("swe", "air_temp")
                    status = "Active"
                    limit = 5
                    cursor = "c"
                }
            },
            "getCatalogStation" to { c ->
                c.getCatalogStation("x") {
                    include = listOf("content")
                    locale = "es"
                }
            },
            "getAvailableRuns" to { c -> c.getRuns(limit = 5) },
            "getStormReports" to { c -> c.getStormReports(limit = 5) },
            "getStormReport" to { c -> c.getStormReport(1) },
            "getAttribution" to { c -> c.getAttribution() },
        )

    @Test
    fun `every operation in the spec is exposed`() {
        assertEquals(operations.keys - OUT_OF_SCOPE, calls.keys, "operations the SDK should expose")
    }

    @TestFactory
    fun `each operation sends only parameters its route defines`(): List<DynamicTest> =
        calls.map { (id, call) ->
            DynamicTest.dynamicTest(id) {
                runTest {
                    val sent = mutableListOf<HttpRequest>()
                    val client =
                        EnsembleWeather {
                            apiKey = "ow_k"
                            units = Units.Imperial
                            transport =
                                HttpTransport {
                                    sent += it
                                    HttpResponse(200, emptyMap(), "{}".toByteArray())
                                }
                        }
                    try {
                        call(client)
                    } catch (_: ResponseFormatException) {
                        // `{}` is not a valid body; only the request matters here.
                    }
                    val allowed = queryParameters(operations.getValue(id))
                    val names =
                        URI(sent.single().url)
                            .rawQuery
                            .orEmpty()
                            .split('&')
                            .filter {
                                it.isNotEmpty()
                            }.map { it.substringBefore('=') }
                    names.forEach {
                        assertTrue(
                            it in allowed,
                            "$id sends '$it', which its route does not define ($allowed)",
                        )
                    }
                }
            }
        }

    private fun queryParameters(operation: Map<String, Any?>): Set<String> =
        (operation["parameters"] as List<Map<String, Any?>>?)
            .orEmpty()
            .map { p ->
                (p["\$ref"] as String?)?.let { components.map("parameters").map(it.substringAfterLast('/')) }
                    ?: p
            }.filter { it["in"] == "query" }
            .map { it["name"] as String }
            .toSet()

    // ---- Models -----------------------------------------------------------------------

    @TestFactory
    fun `each model has exactly its schema's fields`(): List<DynamicTest> =
        ROOTS.map { (schema, serializer) ->
            DynamicTest.dynamicTest(schema) {
                val problems = mutableListOf<String>()
                compare(schema, resolve(schemas.map(schema)), serializer.descriptor, problems)
                assertTrue(problems.isEmpty(), problems.joinToString("\n"))
            }
        }

    private fun compare(
        path: String,
        schema: Map<String, Any?>,
        descriptor: SerialDescriptor,
        problems: MutableList<String>,
    ) {
        val properties = properties(schema)
        val required = requiredOf(schema)
        val names = (0 until descriptor.elementsCount).map(descriptor::getElementName)
        (properties.keys - names.toSet()).forEach { problems += "$path.$it: in the spec, not in the model" }
        (names.toSet() - properties.keys).forEach { problems += "$path.$it: in the model, not in the spec" }
        for ((i, name) in names.withIndex()) {
            val property = properties[name] ?: continue
            val resolved = resolve(property)
            val nullable = isNullable(property)
            val optionalInModel = descriptor.isElementOptional(i)
            val where = "$path.$name"
            when {
                name !in required && !optionalInModel -> {
                    problems +=
                        "$where: optional in the spec, required in the model"
                }

                name in required && !nullable && optionalInModel && !lenient(descriptor, where) -> {
                    problems += "$where: required in the spec, optional in the model"
                }
            }
            val element = descriptor.getElementDescriptor(i)
            compareNested(where, resolved, element, problems)
        }
    }

    private fun compareNested(
        path: String,
        schema: Map<String, Any?>,
        descriptor: SerialDescriptor,
        problems: MutableList<String>,
    ) {
        if (path in OPAQUE) return
        when {
            schema["type"] == "array" || (schema["type"] as? List<*>)?.contains("array") == true -> {
                val items = schema["items"] as Map<String, Any?>? ?: return
                if (descriptor.kind != StructureKind.LIST) {
                    problems += "$path: an array in the spec, ${descriptor.kind} in the model"
                    return
                }
                compareNested("$path[]", resolve(items), descriptor.getElementDescriptor(0), problems)
            }

            properties(schema).isNotEmpty() -> {
                if (descriptor.kind != StructureKind.CLASS) {
                    problems += "$path: an object in the spec, ${descriptor.kind} in the model"
                    return
                }
                compare(path, schema, descriptor, problems)
            }
        }
    }

    // ---- Examples ---------------------------------------------------------------------

    @TestFactory
    fun `every response example round-trips`(): List<DynamicTest> {
        val tests = mutableListOf<DynamicTest>()
        for ((id, operation) in operations) {
            if (id in OUT_OF_SCOPE) continue
            for ((status, response) in operation.map("responses")) {
                val r =
                    (response as Map<String, Any?>).let {
                        it["\$ref"]?.let { ref ->
                            components.map("responses").map((ref as String).substringAfterLast('/'))
                        }
                            ?: it
                    }
                val content =
                    (r["content"] as Map<String, Any?>?)?.get("application/json") as Map<String, Any?>? ?: continue
                val examples =
                    listOfNotNull(content["example"]?.let { "example" to it }) +
                        (content["examples"] as Map<String, Any?>?).orEmpty().map { (name, ex) ->
                            name to
                                (ex as Map<String, Any?>)["value"]
                        }
                for ((name, value) in examples) {
                    val label = "$id $status $name"
                    tests += DynamicTest.dynamicTest(label) { roundTrip(label, id, status.toInt(), toJson(value)) }
                }
            }
        }
        assertTrue(tests.size > MIN_EXAMPLES, "found only ${tests.size} examples")
        return tests
    }

    private fun roundTrip(
        label: String,
        operation: String,
        status: Int,
        example: JsonElement,
    ) {
        if (status >= HTTP_ERROR) return errorRoundTrip(label, operation, status, example as JsonObject)
        val schemaName = RESPONSE_SCHEMA.getValue(operation)
        val violations = mutableListOf<String>()
        validate("", example, resolve(schemas.map(schemaName)), violations)
        if (label in INVALID_EXAMPLES) {
            // The example breaks its own schema, so no conforming model can read it. Prove that,
            // so the entry is removed once the spec is fixed.
            assertTrue(violations.isNotEmpty(), "$label now matches its schema: remove it from INVALID_EXAMPLES")
            return
        }
        assertTrue(violations.isEmpty(), "$label breaks its own schema:\n${violations.joinToString("\n")}")
        val serializer = ROOTS.getValue(schemaName) as KSerializer<Any>
        val decoded = EnsembleWeather.JSON.decodeFromJsonElement(serializer, example)
        val encoded = EnsembleWeather.JSON.encodeToJsonElement(serializer, decoded)
        assertJsonEquivalent(example, encoded, label)
    }

    /** An error example goes through the error mapping: the right subtype, the code and message verbatim. */
    private fun errorRoundTrip(
        label: String,
        operation: String,
        status: Int,
        example: JsonObject,
    ) = runTest {
        val client =
            EnsembleWeather {
                maxRetries = 0
                transport = HttpTransport { HttpResponse(status, emptyMap(), example.toString().toByteArray()) }
            }
        val error =
            try {
                calls.getValue(operation)(client)
                fail("$label: no error")
            } catch (e: ApiException) {
                e
            }
        val code = (example["error"] as JsonPrimitive).content
        assertEquals(code, error.code, label)
        assertEquals((example["message"] as JsonPrimitive).content, error.message, label)
        assertEquals(status, error.status, label)
        val expected =
            when (code) {
                "OUTSIDE_COVERAGE" -> OutsideCoverageException::class
                "PRO_REQUIRED" -> PermissionException::class
                "INVALID_API_KEY" -> AuthenticationException::class
                "TIMEOUT" -> UnavailableException::class
                "NOT_FOUND" -> NotFoundException::class
                else -> InvalidRequestException::class
            }
        assertEquals(expected, error::class, label)
    }

    /** A minimal JSON Schema check: required fields, and no fields the schema does not define. */
    private fun validate(
        path: String,
        value: JsonElement,
        schema: Map<String, Any?>,
        violations: MutableList<String>,
    ) {
        when (value) {
            is JsonObject -> {
                val properties = properties(schema)
                if (properties.isEmpty()) return
                (requiredOf(schema) - value.keys).forEach { violations += "$path.$it: required, missing" }
                for ((key, child) in value) {
                    val property = properties[key]
                    if (property == null) {
                        violations += "$path.$key: not in the schema"
                    } else if (child !is JsonNull) {
                        validate("$path.$key", child, resolve(property), violations)
                    }
                }
            }

            is JsonArray -> {
                (schema["items"] as Map<String, Any?>?)?.let { items ->
                    value.forEach { validate("$path[]", it, resolve(items), violations) }
                }
            }

            is JsonPrimitive -> {
                val enum = schema["enum"] as List<*>?
                if (enum != null && value.isString && value.content !in enum.map { it.toString() }) {
                    violations += "$path: '${value.content}' is not one of the schema's values"
                }
            }
        }
    }

    // ---- Schema helpers ----------------------------------------------------------------

    /** Follows `$ref`, merges `allOf`, and unwraps `anyOf [x, null]`. */
    private fun resolve(schema: Map<String, Any?>): Map<String, Any?> {
        (schema["\$ref"] as String?)?.let { return resolve(schemas.map(it.substringAfterLast('/'))) }
        (schema["allOf"] as List<Map<String, Any?>>?)?.let { parts ->
            val merged = parts.map(::resolve)
            return mapOf(
                "type" to "object",
                "properties" to
                    merged.fold(mapOf<String, Any?>()) { acc, p -> acc + properties(p) } + properties(schema),
                "required" to merged.flatMap { requiredOf(it) } + requiredOf(schema),
                "items" to merged.firstNotNullOfOrNull { it["items"] },
                "enum" to merged.firstNotNullOfOrNull { it["enum"] },
            ).filterValues { it != null }
        }
        (schema["anyOf"] as List<Map<String, Any?>>?)?.let { options ->
            options.firstOrNull { it["type"] != "null" }?.let { return resolve(it) }
        }
        return schema
    }

    private fun isNullable(schema: Map<String, Any?>): Boolean =
        (schema["type"] as? List<*>)?.contains("null") == true ||
            (schema["anyOf"] as List<Map<String, Any?>>?)?.any { it["type"] == "null" } == true

    private fun properties(schema: Map<String, Any?>): Map<String, Map<String, Any?>> =
        (schema["properties"] as Map<String, Map<String, Any?>>?).orEmpty()

    private fun requiredOf(schema: Map<String, Any?>): Set<String> =
        (schema["required"] as List<String>?).orEmpty().toSet()

    private fun Map<String, Any?>.map(key: String): Map<String, Any?> =
        this[key] as Map<String, Any?>? ?: fail("no '$key' in the spec")

    private fun toJson(value: Any?): JsonElement =
        when (value) {
            null -> JsonNull
            is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to toJson(v) })
            is List<*> -> JsonArray(value.map(::toJson))
            is Boolean -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }

    /** Equal as JSON, with numbers compared by value and `null` the same as absent. */
    private fun assertJsonEquivalent(
        want: JsonElement,
        got: JsonElement?,
        path: String,
    ) {
        when (want) {
            is JsonNull -> {
                assertTrue(got == null || got is JsonNull, "$path: expected null, got $got")
            }

            is JsonObject -> {
                val g = assertIs<JsonObject>(got, path)
                (want.keys + g.keys).forEach { assertJsonEquivalent(want[it] ?: JsonNull, g[it], "$path.$it") }
            }

            is JsonArray -> {
                val g = assertIs<JsonArray>(got, path)
                assertEquals(want.size, g.size, "$path size")
                want.indices.forEach { assertJsonEquivalent(want[it], g[it], "$path[$it]") }
            }

            is JsonPrimitive -> {
                val g = assertIs<JsonPrimitive>(got, path)
                if (want.isString) {
                    assertEquals(want.content, g.content, path)
                } else {
                    assertEquals(want.doubleOrNull ?: want.content, g.doubleOrNull ?: g.content, path)
                }
            }
        }
    }

    private fun ForecastOptions.allForecastOptions() {
        units = Units.Kelvin
        maxHours = 48
        includePast = true
        models = true
        members = true
        daily = false
        maxDays = 3
        hourly = false
        run = "2026-11-15T00:00:00Z"
        sources = listOf("snotel")
        excludeSources = listOf("raws")
        nowcast = false
        diagnostics = listOf("ptype")
        feelsLike = listOf("utci")
        chance(Variable.Snowfall atLeast 1.inches within 24.hours)
        total(Variable.Snowfall totalWithin 48.hours)
    }

    private fun CurrentOptions.allCurrentOptions() {
        units = Units.Kelvin
        sources = listOf("a")
        excludeSources = listOf("b")
        include = listOf("nowcast_hours")
    }

    private fun NearbyOptions.allNearbyOptions() {
        units = Units.Kelvin
        limit = 5
        radiusKm = 10.0
    }

    private fun LocationListOptions.allListOptions() {
        type = LocationType.SkiArea
        units = Units.Kelvin
        limit = 5
        cursor = "c"
    }

    companion object {
        private const val HTTP_ERROR = 400
        private const val MIN_EXAMPLES = 50

        /** BEHAVIOR.md section 3: a web-map credential, out of SDK scope for 0.x. */
        private val OUT_OF_SCOPE = setOf("getMapsToken")

        /** Each response schema and the model that reads it. */
        private val ROOTS: Map<String, KSerializer<*>> =
            mapOf(
                "ForecastResponse" to serializer<ForecastResponse>(),
                "CurrentResponse" to serializer<CurrentResponse>(),
                "AlertsResponse" to serializer<AlertsResponse>(),
                "NearbyLocationsResponse" to serializer<NearbyLocationsResponse>(),
                "NearbyPointResponse" to serializer<NearbyPointResponse>(),
                "SearchLocationsResponse" to serializer<SearchLocationsResponse>(),
                "LocationsResponse" to serializer<LocationsResponse>(),
                "PublicCatalogLocationPage" to serializer<PublicCatalogLocationPage>(),
                "PublicCatalogLocationDetail" to serializer<PublicCatalogLocationDetail>(),
                "PublicCatalogStationPage" to serializer<PublicCatalogStationPage>(),
                "PublicCatalogStationDetail" to serializer<PublicCatalogStationDetail>(),
                "AttributionRegistry" to serializer<AttributionRegistry>(),
                "RunsResponse" to serializer<RunsResponse>(),
                "StormReportsResponse" to serializer<StormReportsResponse>(),
                "StormReportCard" to serializer<StormReportCard>(),
            )

        private val RESPONSE_SCHEMA =
            mapOf(
                "getLocationForecast" to "ForecastResponse",
                "getPointForecast" to "ForecastResponse",
                "getAttribution" to "AttributionRegistry",
            )

        /**
         * Deliberate differences, each with its reason. Every `Quantiles` field is optional
         * (BEHAVIOR.md section 6: absent is never 0); `nowcast_hours` is required in the
         * schema but documented as present only with `include=nowcast_hours`.
         */
        private fun lenient(
            descriptor: SerialDescriptor,
            where: String,
        ): Boolean =
            descriptor.serialName.removeSuffix("?").endsWith(".Quantiles") || where == "CurrentResponse.nowcast_hours"

        /** Free-form JSON (`JsonElement`) and the `kind`-tagged FreezingLevel: checked by their own tests. */
        private val OPAQUE: Set<String> =
            setOf(
                "ForecastResponse.forecast.timeseries[].freezing_level",
                "ForecastResponse.elevation_forecasts[].hourly[].freezing_level",
                "CurrentResponse.nowcast_hours[].freezing_level",
            )

        /**
         * Spec examples that break their own schema (reported to the spec repo), so no
         * conforming model can read them: the abridged forecasts lack required fields
         * and still use pre-v1 spellings (`location.elevation`, `type: skiArea`).
         */
        private val INVALID_EXAMPLES =
            setOf(
                "getLocationForecast 200 dailyOnly",
                "getLocationForecast 200 plumes",
                "getPointForecast 200 point",
            )
    }
}
