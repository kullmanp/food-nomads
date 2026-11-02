# Repository interfaces as the seam for unit tests

The application layer (e.g. `Venues`) holds the business rules, so that is where we unit-test inside `app`. Those tests run as plain JUnit, with no Quarkus and no database, so they stay fast enough for test-first work and mutation testing. To make that possible, an application service doesn't call Panache or the `EntityManager` directly. It talks to a small repository interface per aggregate (`VenueRepository`), injected through the constructor. Production uses a Panache implementation, and tests use an in-memory fake. Entities stay Panache entities: we only move the persistence calls behind the interface, and we don't introduce a separate domain model.

## Consequences

- Repository interfaces stay minimal: one method per query the application layer actually needs. They hold no rules.
- The Panache implementations aren't unit-tested. The acceptance tests cover them through both drivers.
- `@Transactional` stays on the application service. Repositories assume a caller's transaction.
- Services that still talk to Panache directly (e.g. `Membership`) move behind a repository the next time a ticket changes their rules.
