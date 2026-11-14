# 03 Candidate Venues

Status: ready-for-agent
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
