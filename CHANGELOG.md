# Changelog

All notable changes to this library are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the library uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Contributions from
outside the Ourea team are credited by GitHub handle.

## [Unreleased]

Moves to the v1 contract live on api.oureawx.com (ensemble-weather-spec
`8b85fea`, the server's public spec byte for byte). Breaking (0.x).

### Changed

- The coordinate methods follow the operation ids: `getCurrentAt` is
  `getPointCurrent`, `getAlertsAt` is `getPointAlerts` and `getNearbyAt` is
  `getPointNearby`, beside `getPointForecast`. There are no aliases.
- `precip_*` is `precipitation_*` everywhere: `DailyForecast.totalPrecipitation`,
  `totalPrecipitationIfWet`, `precipitationProbability`,
  `precipitationOccurrenceProbability`, `precipitationType`,
  `precipitationDisplay`; `TimeSeriesPoint.precipitationType`;
  `CurrentConditions.precipitationProbability`;
  `SensorObservation.precipitationRate`; `ProbabilityThresholds.dailyPrecipitation*`.
- `EnsembleStats.windDirection` and `ModelMean.windDirection` are `CircularStats`
  (mean, stddev, `sector50`/`sector90`), not `Quantiles`.
- Probability and window-total answers are matched to the caller's terms by the
  `term` the server echoes, not by a rebuilt key.
- Errors: the 16 public codes map as BEHAVIOR.md section 8 says
  (`AUTH_UNAVAILABLE` is unavailable; `OUTSIDE_COVERAGE` is a 404); a retired
  code maps by status. `requestId` prefers the body's `request_id`.
  `Retry-After` is honoured on 504 as on 429 and 503.
- `ActiveAlert.severity`/`urgency` are open enums (`AlertSeverity`,
  `AlertUrgency`, snake_case); catalog `network`, `station_status`,
  `template_variant` and a template's `location_type` are open enums
  (`StationNetwork`, `StationStatus`, `TemplateVariant`, `LocationType`).
  `ConditionIcon` gains the night forms it lacked.
- `LocationRef.slug` and `type` are nullable (a coordinate forecast's location
  may have neither); `DailyWind` and `GapFlow` fields the spec requires are no
  longer nullable.
- `getForecast(state, slug)`: the location parameter is `slug` (was `name`), as
  in the spec's `{slug}` path parameter and the vectors.

### Added

- `ProbabilityResult.term`/`nullReason`; `WindowTotal.term`, `aggregation`
  (`TotalAggregation`) and `nullReason` (`NullReason`).
- `ActiveAlert.certainty` (`AlertCertainty`); `CurrentStation.stationId`.
- `ApiException.docUrl` and `param`; `PermissionException.nearestLocation`
  (`NearestLocation`).
- `ForecastOptions.include` (`mslp`, `skin_temperature`), sent sorted.
- `getRuns(limit, cursor)` and `RunsResponse.nextCursor`.
- The spec-drift test also fails when the spec makes a field nullable and the
  model does not.

### Removed

- `getStormReports`/`getStormReport`, `StormReportCard` and
  `StormReportsResponse` (not in the public API).
- Catalog ads: `PublicCatalogLocationDetail.ads`, `PublicCatalogStationDetail.ads`
  and their models.
- `EnsembleStats.feelsLikeUtci`, `ptype` and `windDirectionCircular`,
  `DailyForecast.feelsLikeUtci*`, `ElevationForecast.isDefault`/`timeseries` and
  `ElevationTimeSeriesPoint`.

## [0.1.0] - 2026-10-26

### Added

- `EnsembleWeather`, the coroutine client for API v1 (contract 1.0.0): every
  operation except `getMapsToken` as a `suspend` function, the lists as `Flow`s
  that follow the cursor, and a DSL builder (`EnsembleWeather { apiKey = ...; units = ... }`).
- Probability terms (`Variable.Snowfall atLeast 6.inches within 24.hours`) and
  window totals (`Variable.Snowfall totalWithin 72.hours`), computed server-side and
  returned in the caller's order with their reference class; thresholds in any unit
  converted exactly.
- Hand-written immutable data-class models for every response schema, with open
  value-class enums that keep values added later.
- Canonical request URLs, keyless open routes, same-host redirects, retries with
  `Retry-After` and full-jitter backoff, an ETag cache on by default, deprecation
  notices, and rate-limit state on every response and as a `StateFlow`.
- A sealed `EnsembleWeatherException` hierarchy, one type per error category.
- `HttpTransport`: `java.net.http` by default on the JVM, and the hook for OkHttp or
  Ktor on Android (minSdk 24).

[Unreleased]: https://github.com/Ourea-Technologies/ensemble-weather-kotlin/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Ourea-Technologies/ensemble-weather-kotlin/releases/tag/v0.1.0
