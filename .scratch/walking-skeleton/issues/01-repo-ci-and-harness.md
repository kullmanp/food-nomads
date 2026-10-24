# 01 Repo structure, CI and acceptance harness

Status: ready-for-agent
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
