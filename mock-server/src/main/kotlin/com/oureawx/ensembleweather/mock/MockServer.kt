// SPDX-License-Identifier: BSD-2-Clause
package com.oureawx.ensembleweather.mock

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder

/**
 * A stand-in for the Ourea API on 127.0.0.1, built on the JDK's HTTP server and the
 * shared vector bodies. Its probabilities and totals are canned numbers, not forecasts.
 */
class MockServer(
    private val bodies: File,
    port: Int = 0,
) : AutoCloseable {
    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)

    val url: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange ->
            try {
                handle(exchange)
            } finally {
                exchange.close()
            }
        }
        server.start()
    }

    override fun close() = server.stop(0)

    private fun load(name: String): JsonObject =
        Json.parseToJsonElement(File(bodies, "$name.json").readText()).jsonObject

    private fun handle(exchange: HttpExchange) {
        val path = exchange.requestURI.rawPath
        val query = parseQuery(exchange.requestURI.rawQuery.orEmpty())
        val keyed = exchange.requestHeaders.getFirst("X-API-Key") != null
        val (status, body) = route(path, query, keyed)
        val bytes = body.toString().toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.responseHeaders.add("ETag", "\"mock-${body.hashCode()}\"")
        exchange.responseHeaders.add(
            "Cache-Control",
            if (keyed) "private, max-age=0" else "public, max-age=0, s-maxage=60",
        )
        if (keyed) {
            exchange.responseHeaders.add("RateLimit-Limit", "60")
            exchange.responseHeaders.add("RateLimit-Remaining", "59")
            exchange.responseHeaders.add("RateLimit-Reset", "1")
            exchange.responseHeaders.add("RateLimit-Policy", "60;w=60, 10000;w=86400")
        }
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.write(bytes)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun route(
        path: String,
        query: Map<String, List<String>>,
        keyed: Boolean,
    ): Pair<Int, JsonElement> {
        val segments = path.removePrefix("/api/v1/").split('/')
        val units = query["units"]?.firstOrNull() ?: "metric"
        return when {
            path == "/api/v1/forecast" -> {
                if (!keyed) {
                    return 403 to
                        error("PRO_REQUIRED", "Forecasts at arbitrary coordinates need a Pro API key.")
                }
                val lat = query["lat"]?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                val lon = query["lon"]?.firstOrNull()?.toDoubleOrNull() ?: 0.0
                if (lat !in 24.0..50.0 || lon !in -125.0..-66.0) {
                    return 404 to error("OUTSIDE_COVERAGE", "($lat, $lon) is outside the forecast coverage window")
                }
                200 to forecast(units, query, point(lat, lon, query["elevation_m"]?.firstOrNull()?.toDoubleOrNull()))
            }

            segments.size == 3 && segments[0] == "locations" -> {
                200 to forecast(units, query, null)
            }

            segments.size == 4 && segments[0] == "locations" && segments[3] == "current" -> {
                200 to current(units)
            }

            path == "/api/v1/current" -> {
                if (keyed) 200 to current(units) else 403 to error("PRO_REQUIRED", "Pro only.")
            }

            segments.size == 4 && segments[0] == "locations" && segments[3] == "alerts" -> {
                200 to alerts()
            }

            path == "/api/v1/alerts" -> {
                if (keyed) 200 to alerts() else 403 to error("PRO_REQUIRED", "Pro only.")
            }

            path == "/api/v1/search/locations" -> {
                200 to load("search")
            }

            path == "/api/v1/catalog/locations" -> {
                200 to load(if (query["cursor"]?.firstOrNull() == "c2") "catalog-page-2" else "catalog-page-1")
            }

            segments.size == 3 && segments[0] == "catalog" && segments[1] == "locations" -> {
                200 to
                    load("catalog-detail")
            }

            path == "/api/v1/attribution" -> {
                200 to load("attribution")
            }

            else -> {
                404 to error("NOT_FOUND", "No route for $path")
            }
        }
    }

    private fun forecast(
        units: String,
        query: Map<String, List<String>>,
        point: JsonObject?,
    ): JsonObject {
        val base = load(if (units == "imperial") "forecast-imperial" else "forecast-metric")
        val fields = base.toMutableMap()
        fields["units"] = JsonPrimitive(units)
        query["prob"]?.firstOrNull()?.let { prob ->
            fields["probabilities"] =
                buildJsonArray {
                    prob.split(',').forEachIndexed { i, term -> add(probability(term, i)) }
                }
        }
        query["totals"]?.firstOrNull()?.let { totals ->
            val canned =
                load(
                    "forecast-totals",
                )["window_totals"]!!.let { (it as JsonArray)[0].jsonObject["quantiles"]!! }
            fields["window_totals"] =
                buildJsonArray {
                    totals.split(',').forEach { term ->
                        val (variable, window) = term.split(':')
                        val (start, end) = window(window)
                        add(
                            buildJsonObject {
                                put("variable", variable)
                                put("window_start_hours", start)
                                put("window_end_hours", end)
                                put("from", "2026-11-15T00:00:00Z")
                                put("to", "2026-11-17T00:00:00Z")
                                put("quantiles", canned)
                                put("members", MEMBERS)
                            },
                        )
                    }
                }
        }
        if (point != null) fields["point"] = point
        return JsonObject(fields)
    }

    private fun probability(
        term: String,
        index: Int,
    ): JsonObject {
        val parts = term.split(':')
        val variable = parts[0]
        val (start, end) = window(parts.getOrNull(3) ?: "24h")
        val amount = variable == "precipitation" || variable == "snowfall"
        return buildJsonObject {
            put("variable", variable)
            put("op", parts[1])
            put("threshold", parts[2].toDouble())
            put("window_start_hours", start)
            put("window_end_hours", end)
            put("aggregation", parts.getOrNull(4) ?: if (amount) "total" else "any")
            put("from", "2026-11-15T00:00:00Z")
            put("to", "2026-11-16T00:00:00Z")
            put("probability", CANNED[index % CANNED.size])
            put("members", MEMBERS)
        }
    }

    private fun window(text: String): Pair<Int, Int> {
        val hours = text.removeSuffix("h")
        return if ('-' in
            hours
        ) {
            hours.substringBefore('-').toInt() to hours.substringAfter('-').toInt()
        } else {
            0 to hours.toInt()
        }
    }

    private fun point(
        lat: Double,
        lon: Double,
        elevation: Double?,
    ) = buildJsonObject {
        put("requested_lat", lat)
        put("requested_lon", lon)
        put("lat", lat)
        put("lon", lon)
        put("resolution_deg", 0.01)
        put("elevation", elevation ?: 1661.0)
        put("elevation_source", if (elevation == null) "dem_3dep_10m" else "request")
        put("state", "OR")
    }

    private val location: JsonElement get() = load("forecast-metric")["location"]!!
    private val attribution: JsonElement get() = load("forecast-metric")["attribution"]!!

    private fun alertList() =
        buildJsonArray {
            add(
                buildJsonObject {
                    put("id", "urn:oid:2.49.0.1.840.0.mock")
                    put("event", "Winter Storm Warning")
                    put("severity", "Severe")
                    put("urgency", "Expected")
                    put("headline", "Winter Storm Warning until Sunday 4 PM (mock)")
                    put("expires", "2026-11-16T00:00:00Z")
                },
            )
        }

    private fun current(units: String) =
        buildJsonObject {
            put("location", location)
            put("alerts", alertList())
            put("alerts_as_of", "2026-11-15T00:00:00Z")
            put(
                "sensors",
                buildJsonArray {
                    add(
                        buildJsonObject {
                            put("station_id", "651:OR:SNTL")
                            put("name", "Mt Hood Test Site")
                            put("source", "snotel")
                            put("observation_time", "2026-11-14T23:00:00Z")
                            put("temperature", if (units == "imperial") 26.6 else -3.0)
                        },
                    )
                },
            )
            put("sensor_count", 1)
            put("as_of", "2026-11-15T00:05:00Z")
            put("model_run_initialized_at", "2026-11-15T00:00:00Z")
            put(
                "current",
                buildJsonObject {
                    put("time", "2026-11-15T00:00:00Z")
                    put("temperature", if (units == "imperial") 28.4 else -2.0)
                    put("condition", "snow")
                    put("condition_text", "Snow")
                    put("condition_icon", "night_snow")
                    put("wind_speed", 6.0)
                    put("wind_direction", 5.0)
                    put("source", "nowcast")
                },
            )
            put("units", units)
            put("attribution", attribution)
        }

    private fun alerts() =
        buildJsonObject {
            put("location", location)
            put("alert_count", 1)
            put("as_of", "2026-11-15T00:00:00Z")
            put("alerts", alertList())
            put("attribution", attribution)
        }

    private fun error(
        code: String,
        message: String,
    ) = buildJsonObject {
        put("error", code)
        put("message", message)
    }

    private fun parseQuery(raw: String): Map<String, List<String>> =
        raw.split('&').filter { it.isNotEmpty() }.groupBy(
            { URLDecoder.decode(it.substringBefore('='), "UTF-8") },
            { URLDecoder.decode(it.substringAfter('=', "").replace("+", "%2B"), "UTF-8") },
        )

    private companion object {
        const val MEMBERS = 131
        val CANNED = listOf(0.42, 0.18, 0.73, 0.05)
    }
}

/** Serves the mock until stopped: `./gradlew :mock-server:run` (port 8787). */
fun main() {
    val server = MockServer(File(System.getProperty("ourea.bodies") ?: "spec/vectors/bodies"), port = 8787)
    println("mock Ourea API on ${server.url} (export OUREA_BASE_URL=${server.url})")
    Thread.currentThread().join()
}
