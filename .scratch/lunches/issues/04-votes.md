# 04 Votes

Status: claimed
Blocked by: 03
Spec: [../spec.md](../spec.md)

## What

Members vote up or down on the Candidate Venues of a Planned Lunch (see `GLOSSARY.md`: Vote).

- App: `Lunches.vote(member, lunch, venue, direction)` and `withdrawVote(...)`. At most one Vote per Member and Candidate Venue; voting again replaces it. Only Candidate Venues of the Planned Lunch take Votes. Removing a Candidate Venue removes its Votes.
- Candidate Venues are listed by up minus down, highest first, then by name (unit test).
- UI: each Candidate Venue shows its up and down counts, not who voted, and up/down buttons; the Member's own Vote is highlighted, and pressing it again withdraws it.
- API: `PUT` and `DELETE /api/lunches/{id}/candidates/{venueId}/vote`; `candidates` in the detail get `up`, `down`, `myVote`.

## Acceptance tests

```java
class VotesAcceptanceTest extends AcceptanceTest {
    @Test
    void votesAreCountedPerCandidateVenue() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        invites.create("invite: forAnna");
        venues.propose("name: Sternen");
        venues.propose("name: Kunsthalle");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.addCandidate("lunch: friday", "venue: Kunsthalle");
        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");
        lunches.vote("lunch: friday", "venue: Kunsthalle", "direction: down");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");

        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 2", "down: 0");
        lunches.assertVotes("lunch: friday", "venue: Kunsthalle", "up: 0", "down: 1");
    }

    @Test
    void memberChangesAndWithdrawsTheirVote() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");

        lunches.vote("lunch: friday", "venue: Sternen", "direction: down");
        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 0", "down: 1");

        lunches.withdrawVote("lunch: friday", "venue: Sternen");
        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 0", "down: 0");
    }

    @Test
    void removedCandidateVenueLosesItsVotes() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        venues.propose("name: Sternen");
        lunches.plan("lunch: friday", "date: +7");
        lunches.addCandidate("lunch: friday", "venue: Sternen");
        lunches.vote("lunch: friday", "venue: Sternen", "direction: up");

        lunches.removeCandidate("lunch: friday", "venue: Sternen");
        lunches.addCandidate("lunch: friday", "venue: Sternen");

        lunches.assertVotes("lunch: friday", "venue: Sternen", "up: 0", "down: 0");
    }
}
```

## Done when

- Both drivers are green.
- The order of Candidate Venues is covered by unit tests.

## Comments
