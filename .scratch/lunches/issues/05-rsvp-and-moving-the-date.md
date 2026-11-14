# 05 RSVP and moving the date

Status: claimed
Blocked by: 01
Spec: [../spec.md](../spec.md)

## What

Members say whether they're coming, and a Member can move the Lunch to another date, which clears the answers (see `GLOSSARY.md`: RSVP, Planned Lunch).

- App: `Lunches.rsvp(member, lunch, answer)` with `in` or `out`; answering again replaces it. `Lunches.move(lunch, date)` to today or later clears all RSVPs and keeps comments, Candidate Venues and Votes. Only the Planned Lunch takes RSVPs and moves.
- UI: the Lunch page has In and Out buttons, with the Member's own answer highlighted, and lists who is in and who is out by display name. It has a form to change the date. The home page shows how many are in.
- API: `PUT /api/lunches/{id}/rsvp`, `PUT /api/lunches/{id}/date`; the detail gets `rsvps: {in, out}`, the summary `in`.

## Acceptance tests

```java
class RsvpAcceptanceTest extends AcceptanceTest {
    @Test
    void membersAnswerInOrOut() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        invites.create("invite: forAnna");
        lunches.plan("lunch: friday", "date: +7");
        lunches.rsvp("lunch: friday", "answer: in");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        lunches.rsvp("lunch: friday", "answer: out");

        lunches.assertRsvps("lunch: friday", "in: founder", "out: Anna");
    }

    @Test
    void memberChangesTheirAnswer() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");
        lunches.rsvp("lunch: friday", "answer: in");

        lunches.rsvp("lunch: friday", "answer: out");

        lunches.assertRsvps("lunch: friday", "in: ", "out: founder");
    }

    @Test
    void movingTheLunchClearsTheAnswersButKeepsTheChat() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");
        lunches.rsvp("lunch: friday", "answer: in");
        lunches.comment("lunch: friday", "text: Friday is bad for me after all");

        lunches.move("lunch: friday", "date: +8");

        lunches.assertPlanned("lunch: friday", "date: +8", "venue: none");
        lunches.assertRsvps("lunch: friday", "in: ", "out: ");
        lunches.assertComment("lunch: friday", "by: founder", "text: Friday is bad for me after all");
    }

    @Test
    void lunchCannotBeMovedIntoThePast() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.move("lunch: friday", "date: -1", "expect: dateInPast");
    }

    @Test
    void cancelledLunchTakesNoAnswers() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");
        lunches.cancel("lunch: friday");

        lunches.rsvp("lunch: friday", "answer: in", "expect: notPlanned");
    }
}
```

## Done when

- Both drivers are green.
- Moving keeps Candidate Venues and Votes: covered by unit tests (they arrive in tickets 03 and 04; whichever lands later adds the test).

## Comments
