# Changelog

All notable changes to this library are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the library uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Contributions from
outside the Ourea team are credited by GitHub handle.

## [Unreleased]

## [0.1.0] - unreleased

The first release on Maven Central (`com.oureawx:ensemble-weather-kotlin`).
Targets the v1 contract at ensemble-weather-spec `7954268` (the server's public
spec at 256cd770, byte for byte), and passes all 58 shared vectors.

### Added

- `Client`, the coroutine client for API v1 (contract 1.0.0): every
  operation except `getMapsToken` as a `suspend` function, the lists as `Flow`s
  that follow the cursor, and a DSL builder (`Client { apiKey = ...; units = ... }`).
- Probability terms (`Variable.Snowfall atLeast 6.inches within 24.hours`) and
  window totals (`Variable.Snowfall totalWithin 72.hours`), computed server-side and
  returned in the caller's order with their reference class; thresholds in any unit
  converted exactly.
- Hand-written immutable data-class models for every response schema, with open
  value-class enums that keep values added later.
- Canonical request URLs, keyless open routes, same-host redirects, retries with
  `Retry-After` and full-jitter backoff, an ETag cache on by default, deprecation
  notices, and rate-limit state on every response and as a `StateFlow`.
- A sealed `OureaException` hierarchy, one type per error category.
- `HttpTransport`: `java.net.http` by default on the JVM, and the hook for OkHttp or
  Ktor on Android (minSdk 24).

### Added since the v1 contract of 2026-10-27

- `explore(metric)` and `exploreItems(metric)` (a `Flow`): places ranked by an
  `ExploreMetric` (`Snow5d`, `BaseDepth`, `SeasonPrecip`, ...), with `order`,
  `type`, `state`, `near`, `radiusKm`, elevation bounds, `points` and cursor
  paging.
- `getSeason` (observed season-to-date precipitation), `getSnowTrend` (each
  recent run's daily snowfall; Plus or Pro), `getSnowpack` and
  `getSnowpackPlume` (beta), `getComposition` (an open route, sent without the
  key), and the GeoJSON routes `getAlertOutlines` (`classes`, `bbox`) and
  `getAvalancheZones`, passed through with the API's keys and raw coordinates.
- `runs()`, a `Flow` over every pinnable run.
- `searchLocations` takes `sort`, `order`, `near`, `radiusKm` and `units`; with
  a metric sort each hit carries `explore` (`ExploreValue`).
- `ForecastOptions.members` is `Members.Off`, `Hourly` or `Plume`; the plume
  view is `ForecastData.plume` (`MemberPlume`), and `MembersView.Pool`.
- `LatLon` and `BoundingBox`: numbers, checked before sending and sent with
  literal commas.
- New fields: `ForecastResponse.location` is a `ForecastLocation` (adds
  `elevation` and `elevations`), plus `avalanche` and `snowOnGround`;
  `snowLevelWetBulb` on `EnsembleStats`, `ModelMean`, `MemberForecast` and
  `DailyForecast`; `TimeSeriesPoint.measured`, `snowCharacter`,
  `membersSpread`, `members6h` and `precipitationByType`; ten new
  `DailyForecast` fields (`coveredFrom`, `measuredPrecipitation`,
  `snowCharacter`, `slr`, `snowLevelRange`, `snowLevelWetBulbRange`,
  `normal`, `departure`, `percentile`, ...); `precipitationOutlook` and `plume`
  on `ForecastData` and `ElevationForecast`; `ElevationForecast.displayName`,
  `nearestSensors` and `snowOnGround`; `SnowOutlook.next72h`;
  `WindowTotal.method`; `CurrentConditions.measured` and `cloudCover`;
  `CurrentStation.calm`; `CurrentResponse.summary` (`PlaceSummary`) and
  `pass` (`PassForecast`); `SensorObservation.distanceKm` and
  `elevationDiff`; ten NWS fields on `ActiveAlert`;
  `PrecipDisplay.snowAmount`; `ModelEntry.displayMembers`; `SearchHit.explore`;
  `PublicCatalogElevation.displayName`; `PublicCatalogStationDetail.indexable`.
- Enum values: `light_snow` and `heavy_snow` conditions and icons (and their
  night icons), `SourceKind.AvalancheForecast`, `StationNetwork.RainGauge` and
  `Surfrad`; new open enums `ExploreMetric`, `SnowCharacter`, `AlertClass` and
  `PassStatusLevel`. `PLUS_REQUIRED` is a `PermissionException`.
- `examples/SnowReport.kt`; the mock server serves every route; the nightly
  live contract run covers the new routes; the spec-drift test checks every
  field name against the BEHAVIOR.md 3.0 rename table.

### Changed

- No name stutters (BEHAVIOR.md 3.0): the client `EnsembleWeather` is `Client`
  (`Client { ... }`), `EnsembleWeatherException` is `OureaException`, and the
  DSL marker is `OureaDsl`.
- JSON fields that repeat a word of their own type have SDK names, the same in
  every Ourea SDK (BEHAVIOR.md 3.0); the decoder maps them and the encoder
  writes the JSON names back. `ForecastResponse.forecast` is `series` and
  `elevationForecasts` is `elevations`; `CurrentResponse.current` is `now`;
  `ProbabilityResult.probability` is `value`; the list rows are `items`
  (`alerts`, `locations`, `runs`, and nearby's `locations`, whose `location` is
  `origin`) and `alertCount` is `count`; `WindowTotal.windowStartHours`/
  `windowEndHours` are `startHours`/`endHours`; `CurrentConditions.condition*`
  are `code`/`text`/`icon`; an elevation's `elevation` is `height`. The README's
  "Names that differ from the API's JSON" has the whole table.
- `LocationElevation` is `ElevationSummary` (adds `displayName`).
- `MembersCumulative` holds `MemberQuantiles` (a distribution over members).
- Comma lists (`sources`, `exclude_sources`, `elevations`, `include`,
  `network`, `capability`, `class`) are sorted, de-duplicated and sent with
  literal commas (BEHAVIOR.md 3.1 rule 5); `capability` is one comma list, not
  a repeated parameter. A two-letter `state` is lowercased on search and
  explore too.
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

### Removed

- `getStormReports`/`getStormReport`, `StormReportCard` and
  `StormReportsResponse` (not in the public API).
- Catalog ads: `PublicCatalogLocationDetail.ads`, `PublicCatalogStationDetail.ads`
  and their models.
- `EnsembleStats.feelsLikeUtci`, `ptype` and `windDirectionCircular`,
  `DailyForecast.feelsLikeUtci*`, `ElevationForecast.isDefault`/`timeseries` and
  `ElevationTimeSeriesPoint`.

### Also added in the v1 contract of 2026-10-27

- `ProbabilityResult.term`/`nullReason`; `WindowTotal.term`, `aggregation`
  (`TotalAggregation`) and `nullReason` (`NullReason`).
- `ActiveAlert.certainty` (`AlertCertainty`); `CurrentStation.stationId`.
- `ApiException.docUrl` and `param`; `PermissionException.nearestLocation`
  (`NearestLocation`).
- `ForecastOptions.include` (`mslp`, `skin_temperature`), sent sorted.
- `getRuns(limit, cursor)` and `RunsResponse.nextCursor`.
- The spec-drift test also fails when the spec makes a field nullable and the
  model does not.

[Unreleased]: https://github.com/Ourea-Technologies/ensemble-weather-kotlin/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/Ourea-Technologies/ensemble-weather-kotlin/releases/tag/v0.1.0
