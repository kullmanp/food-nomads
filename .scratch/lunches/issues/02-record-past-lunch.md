# 02 Record a Lunch from before the app

Status: ready-for-agent
Blocked by: 01
Spec: [../spec.md](../spec.md)

## What

A Member records a Lunch that took place before the app, so "never twice" holds from the first day (see `GLOSSARY.md`: Held Lunch, Visited Venue).

- App: `Lunches.recordPast(member, date, venue)` stores a Held Lunch with its Venue. The date must be before today in `Europe/Zurich`. A Venue that's already visited is rejected.
- Visited is derived: a Venue is visited when a Held Lunch has it. `lunches` depends on `venues`, never the reverse. Add an ArchUnit rule for that.
- Lock the Venue row while recording, so two Members recording the same Venue at once can't both succeed.
- UI: `/lunches` gets a "record a past Lunch" form (date, Venue from the unvisited ones) and lists Held Lunches below the Planned ones, latest first. The Venue list marks Visited Venues. A Held Lunch's page shows its Venue and its chat.
- API: `POST /api/lunches/past`, `400 date-not-past`, `409 venue-visited`. `GET /api/venues` gets `visited: true|false`.
- DSL: `LunchesDsl` and `VenuesDsl` share one `Aliases` instance per test, so `venue: Sternen` means the same Venue in both.

## Acceptance tests

```java
class RecordPastLunchAcceptanceTest extends AcceptanceTest {
    @Test
    void recordedLunchMakesItsVenueVisited() {
        members.logIn("member: founder");
        venues.propose("name: Sternen");

        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");

        lunches.assertHeld("lunch: spring", "date: -60", "venue: Sternen");
        venues.assertVisited("name: Sternen");
    }

    @Test
    void visitedVenueCannotBeRecordedAgain() {
        members.logIn("member: founder");
        venues.propose("name: Sternen");
        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");

        lunches.recordPast("lunch: summer", "date: -30", "venue: Sternen", "expect: venueVisited");
    }

    @Test
    void recordedLunchMustLieInThePast() {
        members.logIn("member: founder");
        venues.propose("name: Sternen");

        lunches.recordPast("lunch: now", "date: today", "venue: Sternen", "expect: dateNotPast");
        venues.assertNotVisited("name: Sternen");
    }
}
```

## Done when

- Both drivers are green.
- The date boundary (yesterday accepted, today rejected) is covered by unit tests with a `Clock`.

## Comments
