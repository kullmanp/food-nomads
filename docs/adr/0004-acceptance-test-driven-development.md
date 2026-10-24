# Acceptance-test-driven development with a DSL and swappable protocol drivers

Development follows Dave Farley's four-layer acceptance testing: **test cases → DSL → protocol drivers → system under test**. Every story starts with an acceptance test that a human has reviewed and that fails. Agents then implement until it passes. We use a Java DSL with Farley-style string parameters (`venues.propose("name: Sternen")`) instead of Gherkin. That keeps everything in one language, and the DSL handles defaults and aliasing, which gives functional isolation: tests share one running app and never reset it.

The acceptance tests live in their own Maven module and treat the packaged app as a black box. There are two protocol drivers: a **UI driver** (Playwright, phone viewport, virtual WebAuthn authenticator) and an **API driver** (REST against `/api`, with a software WebAuthn authenticator). Both run the identical test suite, chosen by `-Ddriver=ui|api`, and CI runs both. For that reason `/api` is a real, authenticated part of the product, not a test backdoor. A test-only API would let the two drivers exercise different code paths.

## Consequences

- Neither driver has an auth backdoor. Both go through the real passkey ceremony, and the first Member comes in through the bootstrap Invite.
- `main` is always green. Red acceptance tests exist only on feature branches.
- Tests that only make sense for one channel (e.g. PWA install) are tagged and skipped for the other driver.
