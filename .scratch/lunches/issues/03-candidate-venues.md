# 03 Candidate Venues

Status: resolved
Blocked by: 02
Spec: [../spec.md](../spec.md)

## What

A Member puts up Venues for a Planned Lunch and removes them again (see `GLOSSARY.md`: Candidate Venue).

- App: `Lunches.addCandidate(lunch, venue)` and `removeCandidate(lunch, venue)`. Visited Venues, a Venue that's already a Candidate Venue, and a Lunch that isn't planned are rejected. A Candidate Venue that becomes visited (a past Lunch is recorded there) stays listed with its Votes, marked visited, and can be removed.
- **Rename first:** the walking skeleton calls the Venue list "candidates". Rename `Venues.candidates()` to `newestFirst()`, `ul#candidates`/`li.candidate` to `ul#venues`/`li.venue`, and `venues.assertCandidate` / `VenueDriver.assertCandidate` to `assertListed`. That changes `ProposeVenueAcceptanceTest`, a reviewed contract: the human approves it in this ticket's red review.
- UI: the Lunch page lists its Candidate Venues with Remove, marks visited ones, and offers the unvisited Venues to add.
- API: `POST /api/lunches/{id}/candidates`, `DELETE /api/lunches/{id}/candidates/{venueId}`; `GET /api/lunches/{id}` gets `candidates: [{venue, visited}]`.

## Acceptance tests

```java
class CandidateVenuesAcceptanceTest extends AcceptanceTest {
    @Test
    void memberPutsUpAndRemovesCandidateVenues() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        venues.propose("name: Kunsthalle");
        lunches.plan("lunch: friday", "date: +7");

        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.addCandidate("lunch: friday", "venue: Kunsthalle");
        lunches.removeCandidate("lunch: friday", "venue: Kunsthalle");

        lunches.assertCandidates("lunch: friday", "venues: Sternen");
    }

    @Test
    void visitedVenueCannotBeACandidate() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");
        lunches.plan("lunch: friday", "date: +7");

        lunches.addCandidate("lunch: friday", "venue: Sternen", "expect: venueVisited");
    }

    @Test
    void candidateVenueThatBecomesVisitedStaysListedAsVisited() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");

        lunches.recordPast("lunch: spring", "date: -60", "venue: Sternen");

        lunches.assertCandidates("lunch: friday", "venues: Sternen", "visited: Sternen");
    }
}
```

## Done when

- Both drivers are green, including the renamed `ProposeVenueAcceptanceTest`.
- No "candidate" is left for the Venue list in code, ids or DSL.

## Comments

Notes for later tickets:

- **Rename:** the Venue list is no longer called "candidates": `Venues.newestFirst()`, `VenuesResource.all()`, `VenuesPage.VenueRow`, `li.venue`, message `venues_all` ("All Venues"), `venues.assertListed` / `VenueDriver.assertListed`, and `ProposeVenueAcceptanceTest.proposedVenueIsListed`. The list is `ul#venue-list`, not `ul#venues` as the ticket said: `main#venues` already has that id (and htmx swaps it), so a second `#venues` would be a duplicate id.
- **Application layer:** `Lunches.addCandidate(lunchId, venueId)` returns the new `CandidateVenue`; `removeCandidate(lunchId, venueId)`; `candidates(lunch)` lists `ListedCandidate(venue, visited)` by name (German `Collator`, then id; the same `BY_NAME` as `unvisitedVenues()`), which is where ticket 04 adds up/down/myVote and its own order; `addableVenues(lunch)` is the unvisited Venues that aren't up yet, by name, and none unless the Lunch is planned. Like `plan` and `recordPast`, they take no Member. Order of checks in `addCandidate`: `LunchNotFoundException`, `LunchNotPlannedException`, `VenueNotFoundException`, `VenueVisitedException`, `AlreadyCandidateException`. `removeCandidate`: `LunchNotFoundException`, `LunchNotPlannedException`, `NotACandidateException`. Both lock the Lunch row through the new private `plannedForUpdate(id)`, which `cancel` now uses too; moving, voting, picking and holding should go through it. A Candidate Venue that became visited stays and can be removed. Ticket 06 adds the `candidate-picked` check to `removeCandidate`. `Venues.venue(id)` (via the new `VenueRepository.find`) finds a Venue without locking it: putting one up needs no Venue lock, since a Candidate Venue may become visited.
- **Persistence:** `V9__candidate_venues.sql` adds `candidate_venue (id, lunch_id, venue_id)` with the unique constraint `candidate_venue_once (lunch_id, venue_id)` as a backstop (the Lunch row lock already serialises adds, so it isn't turned into `AlreadyCandidateException`). `Lunch.candidates` is a plain `@OneToMany(mappedBy)` like `comments`; `LunchRepository.addCandidate` / `removeCandidate` persist and delete. `PanacheLunchRepository.find` fetches the candidates and their Venues with a second query into the same Lunch, so that comments and candidates don't multiply each other; do the same for Votes rather than adding a third `join fetch` to one query. Votes (ticket 04) can reference `candidate_venue (id)`; removing a Candidate Venue must remove them first (or `on delete cascade`).
- **API:** `POST /api/lunches/{id}/candidates` `{venueId}` answers `201 {venue: {id, name}, visited: false}`, `400 venue-required` (not in the spec), `404` for an unknown Lunch or Venue, `409 lunch-not-planned`, `409 venue-visited`, `409 already-candidate`. `DELETE /api/lunches/{id}/candidates/{venueId}` answers `204`, `404` for an unknown Lunch, `409 lunch-not-planned`, and `409 not-a-candidate` for a Venue that isn't up (the spec lists no error for that case). The detail has `candidates: [{venue: {id, name}, visited}]` by name, before `comments`.
- **UI:** the Lunch page has `h2` "Candidate Venues", then `#no-candidate-venues` or `ul#candidate-venues > li.candidate-venue` (`.visited` when visited) with `.candidate-name`, `.candidate-visited`, and while planned a form with `button.remove-candidate` posting to `/lunches/{id}/candidates/{venueId}/remove`. While planned, `form#add-candidate-form` (`select#candidate-venue` of `addableVenues`, `#add-candidate-button`, posting to `/lunches/{id}/candidates`), or `#no-addable-venues` when nothing is left to put up. Errors: `#add-candidate-venue-required` (`422`), `#add-candidate-venue-visited` and `#add-candidate-already-candidate` (`409`); a Lunch that's no longer planned, or a Venue already removed, just comes back to the page (`303`). Each Candidate Venue's `li` is the place for vote counts and buttons (ticket 04) and the pick button (ticket 06).
- **DSL and drivers:** `lunches.addCandidate("lunch", "venue", "expect: success|venueVisited")`, `lunches.removeCandidate("lunch", "venue")`, `lunches.assertCandidates("lunch", "venues: A, B", "visited: A")` (comma-separated aliases, any order, exactly these, and exactly those visited). The UI driver can't put up a Visited Venue, so `assertAddCandidateRejectedAsVenueVisited` checks it isn't offered in `#candidate-venue` and isn't listed; the API driver checks the `409` and that the Lunch has no candidates. `UiLunchDriver.candidate(name)` finds a Candidate Venue's `li` on the page shown.
