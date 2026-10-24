# 01 Repo structure, CI and acceptance harness

Status: resolved
Blocked by: none
Spec: [../spec.md](../spec.md)

## What

Set up the structure every later story builds on, with the thinnest possible behaviour: the app answers.

- Multi-module Maven (parent, `app`, `acceptance-tests`), Java 25, current Quarkus LTS, Maven wrapper.
- `app`: a placeholder home page (Qute, English message bundle) and `GET /api/status`. Packaged as a container image. PostgreSQL + Flyway wired in, with an empty baseline migration.
- `acceptance-tests`: the four layers (test cases → DSL → protocol drivers → system under test). Includes the `AcceptanceTest` base class, the string-parameter parser with defaults and aliasing, and driver selection via `-Ddriver=ui|api` (default `api`).
- Harness: Testcontainers starts the built app image + PostgreSQL once per run.
- UI driver: Playwright for Java, phone viewport. API driver: plain HTTP client.
- GitHub Actions: build + unit tests + image, then the acceptance suite as two jobs (`api`, `ui`). On `main`, push the image to GHCR.

## Acceptance test

```java
class SystemAvailabilityAcceptanceTest extends AcceptanceTest {
    @Test
    void systemIsAvailable() {
        system.assertAvailable();
    }
}
```

## Done when

- `./mvnw verify` is green locally with both `-Ddriver=api` and `-Ddriver=ui`.
- Both CI acceptance jobs are green on `main`.
- `./mvnw quarkus:dev -pl app` starts with Dev Services Postgres.

## Comments

Notes for later tickets:

- **Harness:** `SystemUnderTest` starts PostgreSQL and the app image once per test JVM (holder idiom), and Ryuk stops them. The image name is `app.image` in the parent pom, which reaches Quarkus via `quarkus.container-image.image` in `app/pom.xml` and the suite via a Failsafe system property. Ticket 02 needs to read the Bootstrap Invite from the app container's log, so expose `app.getLogs()` (or a log consumer) from `SystemUnderTest`.
- **Drivers:** `ProtocolDriver.selected()` reads `-Ddriver` (default `api`) and makes one driver per test, closed in `@AfterEach`. UI: one Chromium per run, a fresh phone-sized context (390×844, mobile, touch) per test. API: one `java.net.http.HttpClient` per test, with no cookie handling yet, so add a `CookieManager` once sessions exist.
- **DSL:** `Params` (`"name: value"`, required/optional with defaults; rejects unknown, duplicate and missing params) and `Aliases` (alias → `"<alias> <6-char suffix>"`, one instance per test) are unit-tested but not yet wired into `AcceptanceTest`. Create an `Aliases` per test there when the first DSL that takes names needs it.
- **Build:** `./mvnw package` builds the Docker image (`quarkus-container-image-docker`, `src/main/docker/Dockerfile.jvm`, UBI 9 OpenJDK 25 runtime), so Docker is required for any build. Testcontainers pulls in `slf4j-api` 1.7, so the acceptance-tests pom pins it to 2.x.
- **CI:** the `build` job saves the image as an artifact, and `acceptance (api)` and `acceptance (ui)` load it and run only `-pl acceptance-tests`. `publish` pushes `:latest` and `:<sha>` to GHCR on `main` only. CI hasn't run yet: it runs on the first push.
