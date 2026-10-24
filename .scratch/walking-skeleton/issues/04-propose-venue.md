# 04 Propose a Venue

Status: ready-for-agent
Blocked by: 03
Spec: [../spec.md](../spec.md)

## What

A Member proposes a Venue, and it appears among the candidates (see `GLOSSARY.md`: Venue).

- A Venue has a name (required), an address (optional), the proposing Member, and the time it was proposed.
- The candidate list shows all Venues, newest first. Votes and Visited Venues come later.
- UI: a phone-first list page with an htmx "propose" form. API: `GET/POST /api/venues`.
- Business rules live in the shared application layer, not in the adapters (ADR-0003).
- DSL: Venue names are aliased for functional isolation.

## Acceptance tests

```java
class ProposeVenueAcceptanceTest extends AcceptanceTest {
    @Test
    void proposedVenueAppearsAmongTheCandidates() {
        members.logIn("member: founder");

        venues.propose("name: Sternen", "address: Spalenberg 1, Basel");

        venues.assertCandidate("name: Sternen", "address: Spalenberg 1, Basel", "proposedBy: founder");
    }

    @Test
    void venueNeedsAName() {
        members.logIn("member: founder");

        venues.propose("name: ", "expect: rejected");
    }
}
```

## Done when

- Both drivers are green.
- The walking skeleton spec's goal is met.
