# Good first issues (draft)

Three real starter issues to file with the `good first issue` label when 0.1.0 is
released. Each is small, self-contained and has a clear "done".

## 1. Add an `examples/NearbyLocations.kt` example

**What:** a runnable example that lists a location's nearest neighbours with
`ourea.getNearby("or/mt-hood-meadows") { limit = 5 }` and prints each one's name
and distance, then does the same around a coordinate with `getNearbyAt`.

**Done when:** the example has a header comment, `./gradlew runExamples` runs it
against the mock (add a `/nearby` route to `mock-server/` that returns a
`NearbyLocationsResponse`), and the README's examples table lists it.

## 2. Ship a ready-made OkHttp transport as a separate artifact

**What:** the README shows an OkHttp `HttpTransport` for Android. Package it as a
tiny module, `ensemble-weather-kotlin-okhttp`, so apps can depend on it instead of
copying it, keeping the core free of OkHttp.

**Done when:** a new Gradle module holds the transport with a test against OkHttp's
`MockWebServer` (redirects not followed, body bounded with `HttpTransport.readBounded`,
`X-API-Key` redacted from logging), and CONTRIBUTING documents the module.

## 3. Examples for the storm reports and model runs

**What:** an `examples/StormReports.kt` that prints the latest storm report cards
(`getStormReports(limit = 5)`, forecast against observed totals) and the runs a
Pro forecast can pin (`getRuns()`).

**Done when:** the mock serves `/api/v1/storm-reports` and `/api/v1/runs` with
bodies that match the schemas, `./gradlew runExamples` passes, and the example is
in the README's table.
