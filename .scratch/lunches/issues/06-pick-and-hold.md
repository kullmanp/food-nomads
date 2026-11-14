# 06 Pick a Venue and hold the Lunch

Status: claimed
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
