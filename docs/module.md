# Module ensemble-weather-kotlin

Official Kotlin SDK for the [Ourea](https://oureawx.com) probabilistic weather API (v1).

Start with [com.oureawx.ensembleweather.kotlin.EnsembleWeather]: build a client with
`EnsembleWeather { ... }`, then call its `suspend` functions. Forecast values are
distributions ([com.oureawx.ensembleweather.kotlin.Quantiles]); ask the server for
probabilities with `chance(...)` and storm totals with `total(...)`.

Guides: <https://docs.oureawx.com/kotlin>.

# Package com.oureawx.ensembleweather.kotlin

The client, its options, the probability and window-total terms, the response
models, the errors, and the [com.oureawx.ensembleweather.kotlin.HttpTransport] and
[com.oureawx.ensembleweather.kotlin.ResponseCache] hooks.
