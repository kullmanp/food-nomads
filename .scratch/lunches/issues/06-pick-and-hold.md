# 06 Pick a Venue and hold the Lunch

Status: resolved
Blocked by: 03
Spec: [../spec.md](../spec.md)

## What

A Member picks the Venue of the Planned Lunch, and marks it as held afterwards (see `GLOSSARY.md`: Planned Lunch, Held Lunch, Visited Venue).

- App: `Lunches.pick(lunch, venue)`, `clearPick(lunch)`, `hold(lunch)`. Only an unvisited Candidate Venue can be picked. A picked Candidate Venue can't be removed, and a past Lunch can't be recorded at it. Holding needs a Venue and a date of today or earlier in `Europe/Zurich`. Held is final: everything that changes the Lunch except comments is then rejected with `lunch-not-planned`.
- Lock the Venue row when holding (the same lock as recording a past Lunch), the Lunch row for every state change.
- UI: each Candidate Venue gets Pick; the Lunch page shows the Venue, Clear and Held (only on or after the date). After its date, the home page asks "Did this Lunch take place?" with Held and Cancel. A Held Lunch shows its Venue and keeps its chat, Votes and RSVPs read-only.
- API: `PUT`/`DELETE /api/lunches/{id}/venue`, `POST /api/lunches/{id}/held`.

## Acceptance tests

```java
class PickAndHoldAcceptanceTest extends AcceptanceTest {
    @Test
    void pickedVenueBecomesVisitedWhenTheLunchIsHeld() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: now", "date: today");
        lunches.addCandidate("lunch: now", "venue: Sternen");

        lunches.pick("lunch: now", "venue: Sternen");
        lunches.assertPlanned("lunch: now", "date: today", "venue: Sternen");

        lunches.hold("lunch: now");
        lunches.assertHeld("lunch: now", "date: today", "venue: Sternen");
        venues.assertVisited("name: Sternen");
    }

    @Test
    void lunchIsNotHeldBeforeItsDate() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.pick("lunch: friday", "venue: Sternen");

        lunches.hold("lunch: friday", "expect: notDue");
    }

    @Test
    void lunchIsNotHeldWithoutAVenue() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: now", "date: today");

        lunches.hold("lunch: now", "expect: noVenue");
    }

    @Test
    void pickedCandidateVenueCannotBeRemoved() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.pick("lunch: friday", "venue: Sternen");

        lunches.removeCandidate("lunch: friday", "venue: Sternen", "expect: candidatePicked");
    }

    @Test
    void cancelledLunchLeavesItsPickedVenueUnvisited() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: now", "date: today");
        lunches.addCandidate("lunch: now", "venue: Sternen");
        lunches.pick("lunch: now", "venue: Sternen");

        lunches.cancel("lunch: now");

        venues.assertNotVisited("name: Sternen");
    }
}
```

## Done when

- Both drivers are green.
- Holding the day before, on and after the date, picking a Candidate Venue that became visited, and recording a past Lunch at the picked Venue are covered by unit tests.

## Comments

Notes for later tickets:

- **Application layer:** `Lunches.pick(lunchId, venueId)`, `clearPick(lunchId)` and `hold(lunchId)` take ids and no Member, like the other state changes, and lock the Lunch through `plannedForUpdate`. Order of checks in `pick`: `LunchNotFoundException`, `LunchNotPlannedException`, `NotACandidateException` (also for an unknown Venue), then `Venues.lock`, then `VenueVisitedException`. `hold`: `LunchNotFoundException`, `LunchNotPlannedException`, `LunchNotDueException`, `NoVenueException`, then `Venues.lock`, then `VenueVisitedException`. `removeCandidate` throws `CandidatePickedException` after `NotACandidateException`; `recordPast` throws `VenuePickedException` after the Venue lock and the visited check. A Cancelled Lunch keeps its pick, which neither counts as visited nor blocks recording. `isDue(lunch)` (planned, date today or earlier in Basel: Held is offered) and `isOver(lunch)` (planned, date before today: the home page asks) are the one place for those rules. `ListedCandidate` is now `(venue, visited, picked, up, down, myVote)`.
- **Lock (not in the ticket):** `pick` locks the Venue too, not only `hold`. Without it, picking and recording a past Lunch at the same Venue could both succeed (each checks the other's uncommitted state), and the Lunch could then never be held. Lock order is always Lunch, then Venue; `recordPast` locks no Lunch, so there's no deadlock. `hold` still checks visited after its lock as a backstop; the unit test sets the Venue directly to reach it. The races were not checked by hand.
- **Persistence:** no migration: `lunch.venue_id` came with ticket 02. `PanacheLunchRepository.planned()` now fetches `l.venue` in the same query as `rsvps`.
- **API:** `PUT /api/lunches/{id}/venue` `{venueId}` answers `204`, `400 venue-required` (not in the spec), `404` for an unknown Lunch, `409 lunch-not-planned`, `409 not-a-candidate`, `409 venue-visited`. `DELETE /api/lunches/{id}/venue` answers `204`, `404`, `409 lunch-not-planned`. `POST /api/lunches/{id}/held` answers `204`, `404`, `409 lunch-not-planned`, `409 lunch-not-due`, `409 no-venue`, and `409 venue-visited` for the backstop above (not in the spec). `POST /api/lunches/past` adds `409 venue-picked`, `DELETE .../candidates/{venueId}` adds `409 candidate-picked`. The detail's `candidates` don't carry `picked`, as in the spec: the summary's `venue` says which one it is.
- **UI:** the Lunch page shows `#lunch-venue`; while planned, `form#clear-venue-form` / `#clear-venue-button` once a Venue is picked, and on or after the date `form#hold-lunch-form` / `#hold-lunch-button` (asks `confirm()` first), or `#lunch-hold-needs-venue` without a Venue. Each `li.candidate-venue` gets `.picked` and `span.candidate-picked` for the picked one; while planned, the others that aren't visited get `button.pick-candidate` (posting `venue` to `/lunches/{id}/venue`), and the picked one has no `.remove-candidate`. `POST /lunches/{id}/venue`, `/venue/clear` and `/held` answer `303` to the Lunch page, also when refused (a stale page). The cards on `/` and `/lunches` show the picked Venue in `.lunch-venue` (without `.no-venue`). After the date the home page shows `section#lunch-over` ("Did this Lunch take place?") with `#over-held-button` (or `#over-pick-venue`, a link to the Lunch page, without a Venue) and `#over-cancel-button`, both asking `confirm()`; no acceptance test reaches it, since a Lunch can't be planned in the past. The record form shows `#record-venue-picked` with `409`, and still offers the picked Venue. `LunchRow` got `due` and `over` and is built with `LunchRow.of(lunch, lunches)`; no template parameters were added.
- **DSL and drivers:** `lunches.pick("lunch", "venue")`, `lunches.hold("lunch", "expect: success|notDue|noVenue")`, `lunches.removeCandidate(..., "expect: success|candidatePicked")`. The UI driver can't press Held before the date or without a Venue, nor Remove on the picked Candidate Venue, so its `assertHoldRejectedAs…` and `assertRemoveCandidateRejectedAsPicked` check those buttons are gone (and `#lunch-hold-needs-venue` without a Venue); the API driver checks the `409`s. The UI driver's `hold` accepts the confirm dialog like `cancel`.
- **Feature done:** this was the last ticket of `lunches`; `spec.md` is `resolved`.
