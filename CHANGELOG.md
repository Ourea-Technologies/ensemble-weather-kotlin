# Changelog

All notable changes to this library are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the library uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html). Contributions from
outside the Ourea team are credited by GitHub handle.

## [Unreleased]

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
