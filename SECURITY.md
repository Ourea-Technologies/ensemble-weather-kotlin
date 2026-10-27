# Security policy

## Reporting a vulnerability

Please report vulnerabilities privately through GitHub's
[private vulnerability reporting](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/security/advisories/new)
for this repository. **Do not open a public issue**, pull request or discussion
for a security problem.

Include what you found, how to reproduce it, and the SDK version
(`EnsembleWeather.SDK_VERSION`). We aim to acknowledge a report within three
working days and will keep you informed until it is fixed and released. We credit
reporters who want it.

## Supported versions

Security fixes go into the latest 0.x release.

| Version | Supported |
| ------- | --------- |
| 0.1.x   | yes       |

## What the SDK guarantees

These are covered by tests (`ClientTest`, `VectorsTest` and the shared vectors):

- the API key never appears in exception messages, `toString` of the client, its
  builder or an `HttpRequest`, and is never sent as a query parameter;
- the key is never sent to another host on a redirect;
- TLS verification is never disabled, and there is no option to disable it;
- a response body over 64 MiB is an error, never an out-of-memory;
- JSON is decoded by kotlinx-serialization into the SDK's own classes only: no
  class named by the payload is ever instantiated (no polymorphic deserialisation),
  and nothing is evaluated;
- the only runtime dependencies are kotlinx-coroutines and kotlinx-serialization.

An `HttpTransport` you write yourself (OkHttp, Ktor) must keep these too; its
contract is in the KDoc of `HttpTransport`.

Issues in the Ourea API itself (not this SDK) can be reported the same way.
