# 04 Votes

Status: resolved
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

Notes for later tickets:

- **Application layer:** `Lunches.vote(member, lunchId, venueId, VoteDirection)` returns the `Vote` (a new one, or the Member's existing one with the new direction); `withdrawVote(member, lunchId, venueId)` removes the Member's Vote and does nothing if they have none. Both lock the Lunch through `plannedForUpdate`. Order of checks: `LunchNotFoundException`, `LunchNotPlannedException`, `NotACandidateException` (also for an unknown Venue). The ticket's `vote(member, lunch, venue, direction)` takes ids, like the other methods. A Member's Vote is found by `member.id`, so a detached Member from `Membership.current` works. `removeCandidate` removes the Candidate Venue's Votes through `LunchRepository.removeVote` before removing it.
- **Listing:** `candidates(lunch)` became `candidates(lunch, member)`, and `ListedCandidate` is `(venue, visited, up, down, myVote)` with `myVote` a `VoteDirection` or null. The order is up minus down, highest first, then `BY_NAME` (German `Collator`, then id). Ticket 06 can mark the picked one there.
- **Persistence:** `V10__votes.sql` adds `vote (id, candidate_venue_id, member_id, direction)` with `vote_once unique (candidate_venue_id, member_id)` as a backstop behind the Lunch row lock (not turned into an exception). `direction` is `@Enumerated(STRING)`, so `UP` / `DOWN`. `CandidateVenue.votes` is a plain `@OneToMany(mappedBy = "candidate")`. `PanacheLunchRepository.find` loads the Votes with a third query (`from CandidateVenue c left join fetch c.votes where c.lunch.id = ?1`). `addVote` merges the detached Member, as `addComment` does. No `on delete cascade`: the application deletes Votes first.
- **API:** `PUT /api/lunches/{id}/candidates/{venueId}/vote` `{direction: up|down}` and `DELETE` of the same path answer `204`, `404` for an unknown Lunch, `409 lunch-not-planned`, `409 not-a-candidate`, and (not in the spec) `400 direction-required` for a missing or unknown direction. `DELETE` without a Vote is `204` too. `candidates` in the detail have `up`, `down` and `myVote` (`"up"`, `"down"` or `null`), in the listing order above. `GET /api/lunches/{id}` now looks up the calling Member (401 without one), since `myVote` depends on it.
- **UI:** each `li.candidate-venue` ends with `.candidate-votes`. While planned: two forms with `button.vote-button.vote-up` / `.vote-down`, `aria-pressed="true"` on the Member's own Vote, each holding the count in `.candidate-up` / `.candidate-down`. A button posts `direction` to `/lunches/{id}/candidates/{venueId}/vote`, the pressed one to `.../vote/withdraw`; both answer `303` to the Lunch page, also when the Lunch is no longer planned or the Candidate Venue is gone. Not planned: read-only `span.vote-count` (`.mine` for the Member's own) with the same `.candidate-up` / `.candidate-down`. Labels use messages with a `{count}` parameter (`lunch_vote_up(int)` etc.), the first parameterised messages in the bundle.
- **DSL and drivers:** `lunches.vote("lunch", "venue", "direction: up|down")`, `lunches.withdrawVote("lunch", "venue")`, `lunches.assertVotes("lunch", "venue", "up: n", "down: n")`. Both drivers check `myVote` after voting and withdrawing (API: `myVote` field; UI: `aria-pressed`). The UI driver's `vote` presses only if that direction isn't pressed yet, since pressing it again would withdraw. `ApiClient.put` is new.
