# Contributing to ensemble-weather-kotlin

Thanks for helping make Ourea better. Bug reports, feature ideas, docs fixes and
pull requests are all welcome, and small ones count: a clearer error message or a
fixed typo in an example is a real improvement.

This SDK follows the shared [SDK standard](spec/SDK-STANDARD.md) and
[behaviour spec](spec/BEHAVIOR.md), pinned in `spec/`. The API's behaviour lives in
[ensemble-weather-spec](https://github.com/Ourea-Technologies/ensemble-weather-spec);
this repository expresses it in idiomatic Kotlin.

We aim to give every new issue and pull request a first response within a few
business days. A fix may take longer, but you will hear from us.

## From clone to green

You need a JDK (17 or newer to run Gradle; the build provisions JDK 11 for the
library itself) and nothing else: the Gradle wrapper fetches everything.

```sh
git clone https://github.com/Ourea-Technologies/ensemble-weather-kotlin.git
cd ensemble-weather-kotlin
./gradlew check runExamples
```

`check` runs the format check, lint, static analysis and every test, including the
43 shared vectors. If it passes, you are set up.

## The checks

Every one of these runs in CI and must pass before a merge:

```sh
./gradlew ktlintCheck                  # format (ktlint, ktlint_official style); ktlintFormat fixes it
./gradlew detekt detektMain detektTest # lint, with type resolution (incl. potential-bugs)
./gradlew spotbugsMain                 # SpotBugs + Find Security Bugs on the bytecode
./gradlew animalsnifferMain            # no JVM API missing on Android API 24
./gradlew test                         # unit tests, the 43 shared vectors, spec drift (JDK 11)
./gradlew test -PtestJdk=21            # the same on another JDK (CI: 11, 17, 21, 25)
./gradlew runExamples                  # every example against the local mock API
./gradlew dokkaGenerate                # the API reference, into build/dokka/html
./gradlew assemble                     # the jar, sources jar and Dokka javadoc jar
./gradlew checkDependencies            # every runtime dependency justified below
scripts/sync-spec --check              # spec/ matches spec/VERSION
```

Explicit API mode and warnings-as-errors are on, so every public declaration needs
a visibility modifier, a type and KDoc.

Tests and examples never call the real API: the vectors and unit tests use a mock
`HttpTransport` on kotlinx-coroutines-test virtual time, and the examples run
against `mock-server/` (`./gradlew :mock-server:run` serves it on port 8787; point
an example at it with `OUREA_BASE_URL=http://127.0.0.1:8787`).

## What makes a good bug report

Use the [bug report form](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/issues/new?template=bug_report.yml). The most useful reports have:

- the SDK version (`EnsembleWeather.SDK_VERSION`) and your runtime: JVM version or
  Android API level, and the `HttpTransport` you use;
- a minimal snippet that shows the problem;
- what you expected and what happened, with the exception's `toString()`;
- the `requestId` from the `ApiException` (or `meta.requestId`), if there is one:
  it lets us find the request on our side.

**Never paste an API key.** The SDK never prints one, so a log or exception you
copy is safe; a snippet you typed may not be.

## Feature ideas

Open a [feature request](https://github.com/Ourea-Technologies/ensemble-weather-kotlin/issues/new?template=feature_request.yml):
describe the problem first, then the API you would like. We weigh ideas by how
many developers they help and whether they keep the SDK small and the server the
one place that computes. Anything that needs the API to answer something new
belongs in ensemble-weather-spec, so we may move such a request there (label
`needs-spec`).

## The pinned spec

`spec/` is a copy of ensemble-weather-spec at the commit or tag in `spec/VERSION`.
Do not edit it by hand:

```sh
scripts/sync-spec <ref>        # pin a new ref and copy it (OUREA_SPEC_REPO=<clone> to use a local clone)
```

A behaviour change starts in ensemble-weather-spec (with its vectors), then lands
here by bumping `spec/VERSION`.

## Dependencies

### Runtime dependencies

Only kotlinx-coroutines and kotlinx-serialization, plus the Kotlin standard
library. There is no HTTP library: the default transport is the JDK's
`java.net.http`, and Android apps plug in their own (see `HttpTransport`).
`./gradlew checkDependencies` fails if the build declares a runtime dependency
without a line here. Adding one needs a justification line and Harrison's approval
first. The list, as `` - `group:name`: why ``:

<!-- runtime-dependencies:start -->
- `org.jetbrains.kotlin:kotlin-stdlib`: the Kotlin standard library every Kotlin program needs.
- `org.jetbrains.kotlinx:kotlinx-coroutines-core`: `suspend` calls, `Flow` paging, cooperative cancellation and timeouts (KAN-1057).
- `org.jetbrains.kotlinx:kotlinx-serialization-json`: compile-time JSON models with no reflection and no polymorphic deserialisation (KAN-1057).
<!-- runtime-dependencies:end -->

### Build and test dependencies

Pinned in `gradle/libs.versions.toml` and locked by the `*.lockfile`s
(`./gradlew resolveAndLockAll --write-locks` refreshes them):

- Kotlin Gradle plugin and the serialization compiler plugin: the compiler.
- ktlint (`org.jlleitschuh.gradle.ktlint`): formatting.
- detekt (`dev.detekt` 2.0, the line built for Kotlin 2.4): lint.
- SpotBugs and Find Security Bugs: static security analysis of the bytecode.
- Animal Sniffer with the Android API 24 signature: Android compatibility.
- Dokka: the API reference. Kover: coverage in CI.
- `com.vanniktech.maven.publish`: Central Portal publishing and signing.
- JUnit 5, kotlin-test and kotlinx-coroutines-test: tests on virtual time.
- SnakeYAML (tests only): reads the pinned OpenAPI document for the drift checks.
- foojay toolchain resolver: provisions the JDKs the build and test matrix use.

## Commits and pull requests

- One logical change per commit, with an imperative summary line and a short body
  saying what and why.
- `main` is protected and linear: rebase, do not merge.
- Add a line under "Unreleased" in `CHANGELOG.md`, crediting yourself by GitHub
  handle if you like: we credit every outside contributor there.
- Run the checks above; the pull request template lists them.

## Releasing (maintainers)

Bump `sdkVersion` in `gradle.properties` and `EnsembleWeather.SDK_VERSION`, move the
changelog entries under the new version, and push a `vX.Y.Z` tag on `main`.
`release.yml` builds and tests again, signs the artifacts and uploads them to the
Maven Central Portal (`com.oureawx`, verified by DNS TXT record), then creates the
GitHub release with the changelog section, an SBOM and build provenance. The
upload is held for a manual "Publish" in the Central Portal.

It needs these repository secrets (in the `release` environment):

| Secret | What |
| --- | --- |
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token name |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
| `SIGNING_KEY` | the ASCII-armoured PGP private key that signs the artifacts |
| `SIGNING_KEY_ID` | its short key id (last 8 hex digits) |
| `SIGNING_KEY_PASSWORD` | its passphrase |

The workflow passes them to Gradle as `ORG_GRADLE_PROJECT_mavenCentralUsername`,
`ORG_GRADLE_PROJECT_mavenCentralPassword`, `ORG_GRADLE_PROJECT_signingInMemoryKey`,
`ORG_GRADLE_PROJECT_signingInMemoryKeyId` and
`ORG_GRADLE_PROJECT_signingInMemoryKeyPassword`. The public key must be on a
keyserver (`keys.openpgp.org`) before the first release.

## Code of conduct

Everyone taking part follows the [Code of Conduct](CODE_OF_CONDUCT.md).
