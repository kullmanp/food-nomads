# 02 Record a Lunch from before the app

Status: resolved
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

Notes for later tickets:

- **Application layer:** `Lunches.recordPast(date, venueId)` stores a Held Lunch with its Venue. It takes no Member, like `plan(date)`: a Lunch doesn't record who planned or recorded it, since the spec doesn't ask (the ticket's sketch said `recordPast(member, date, venue)`). Order of checks: `DateNotPastException` (not before today in Basel), then `Venues.lock(venueId)` (`VenueNotFoundException`), then `VenueVisitedException`. `Lunches.visitedVenueIds()` is the one place that decides "visited": the Venues a Held Lunch has. It filters `repository.all()` in memory, as Invites do for expiry. `Lunches.unvisitedVenues()` is the unvisited Venues by name (German `Collator`, then id). `Lunch.isHeld()` and `LunchState.HELD` are new. `Lunches` now takes `(LunchRepository, Venues, Clock)`.
- **Not done here:** "and so is the Venue the Planned Lunch has picked" (`409 venue-picked`) needs a pick, which comes with ticket 06. `recordPast` is where to add that check, after the lock. Ticket 06's hold should also call `Venues.lock` before `visitedVenueIds()`.
- **Lock:** `Venues.lock(id)` uses `VenueRepository.findForUpdate` (`PESSIMISTIC_WRITE`). Under read committed, a second recorder blocks on the row and then sees the first one's Held Lunch when it lists the Lunches. The race was not checked by hand, and there is no unique index behind it.
- **Persistence:** `V8__held_lunches.sql` adds the nullable `lunch.venue_id` (FK to `venue`, indexed). `Lunch.venue` is a lazy `@ManyToOne`; `PanacheLunchRepository.find` and `all` fetch it, `planned()` doesn't yet (ticket 06 will want it for the card once a Venue can be picked).
- **Unit tests:** `venues.InMemoryVenueRepository` is public now and records which ids were locked (`locked()`). Recording yesterday, rejecting today, and midnight in Basel are covered in `LunchesTest`.
- **ArchUnit:** `venuesDoNotKnowTheLunches` from ticket 01 is the rule for the direction. `VenuesPage` and `VenuesResource` ask `Lunches.visitedVenueIds()`; `venues` never asks `lunches`.
- **API:** `POST /api/lunches/past` `{date, venueId}` answers `201` with the summary (`state: held`, `venue: {id, name}`), `400 date-not-past`, `409 venue-visited`, `404` for an unknown Venue, and (not in the spec) `400 date-required` / `400 venue-required` for a missing field. Summaries and details now carry the Lunch's Venue. `GET /api/venues` items have `visited: true|false`; `POST /api/venues` answers `visited: false`.
- **UI:** `/lunches` ends with a bottom sheet `details#record.sheet` (`summary#record-open`) holding `form#record-form` (posts to `/lunches/past`; `input#record-date` with `max` yesterday, `select#record-venue` of the unvisited Venues by name, `#record-button`; errors `#record-date-not-past`, `#record-date-required`, `#record-venue-required` with `422`, and `#record-venue-visited` with `409`). Without unvisited Venues it shows `#record-no-venues` instead. Held Lunches are in `#earlier-lunches` with `.lunch-state[data-state=held]` and `.lunch-venue`. The Lunch page shows `#lunch-venue` (the name) instead of `#lunch-no-venue` when the Lunch has a Venue, and `#lunch-state[data-state=held]`. `/venues` marks visited ones as `li.candidate.visited` with `.venue-visited`. The cards on `/` and `/lunches` still always say "No Venue yet" (ticket 06).
- **DSL and drivers:** `lunches.recordPast("lunch", "date", "venue", "expect: success|venueVisited|dateNotPast")`, `lunches.assertHeld("lunch", "date", "venue")`, `venues.assertVisited("name")`, `venues.assertNotVisited("name")`. The API driver finds a Venue's id by name in `GET /api/venues`. The UI driver can't submit a Visited Venue, so `assertRecordPastRejectedAsVenueVisited` checks it isn't offered in `#record-venue`; the API driver checks the `409`. `UiLunchDriver.lunchIdFromPage()` is shared by plan and record.
