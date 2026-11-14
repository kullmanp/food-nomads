# 05 RSVP and moving the date

Status: resolved
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

Notes for later tickets:

- **Application layer:** `Lunches.rsvp(member, lunchId, RsvpAnswer)` returns the `Rsvp` (a new one, or the Member's existing one with the new answer); `Lunches.move(lunchId, date)` sets the date and removes all RSVPs through `LunchRepository.removeRsvp`, keeping comments, Candidate Venues and Votes (unit test `movingKeepsTheCommentsCandidateVenuesAndVotes`). Both lock the Lunch through `plannedForUpdate`. Like the other methods, they take ids rather than the ticket's `rsvp(member, lunch, answer)` / `move(lunch, date)`. Order of checks in `move`: `LunchNotFoundException`, `LunchNotPlannedException`, then `DateInPastException`. **Not in the spec:** moving to the date the Lunch already has changes nothing and keeps the RSVPs, since it isn't "another date". There is no withdrawing an RSVP (the spec has none): answering again only replaces it.
- **Reading RSVPs:** `Lunch.answered(RsvpAnswer)` is the Members with that answer, by display name (German `Collator`; display names are unique, so there's no tie-break). `Lunch.inCount()` counts the `IN` RSVPs without touching their Members. `Lunch.answerOf(member)` is the Member's answer or null, found by `member.id`, so a detached Member from `Membership.current` works.
- **Persistence:** `V11__rsvps.sql` adds `rsvp (id, lunch_id, member_id, answer)` with `rsvp_once unique (lunch_id, member_id)` as a backstop behind the Lunch row lock (not turned into an exception). `answer` is `@Enumerated(STRING)`, so `IN` / `OUT`. `Lunch.rsvps` is a plain `@OneToMany(mappedBy)`. `PanacheLunchRepository.find` loads RSVPs and their Members with a fourth query. `planned()` and `all()` now fetch `rsvps` (without their Members) so the summaries can count outside the transaction. `planned()` uses `singleResultOptional`, because a collection fetch can't be limited to the first row in SQL; the partial unique index guarantees at most one. Ticket 06 adding `join fetch l.venue` to `planned()` fits into the same query.
- **API:** `PUT /api/lunches/{id}/rsvp` `{answer: in|out}` and `PUT /api/lunches/{id}/date` `{date}` answer `204`, `404` for an unknown Lunch, `409 lunch-not-planned`, `400 date-in-past` (move), and (not in the spec) `400 answer-required` for a missing or unknown answer and `400 date-required` for a missing date. The summary has `in: n`. The detail has `in` too (it is the summary plus more), and `rsvps: {in: [names], out: [names]}` by display name, between `candidates` and `comments`.
- **UI:** the Lunch page has `h2` "Who's coming", then while planned `button#rsvp-in` / `#rsvp-out` (`.rsvp-button`, `aria-pressed="true"` on the Member's own answer), each in a form posting `answer` to `/lunches/{id}/rsvp` (`303`, also when the Lunch is no longer planned). Then `dl#rsvps` with `ul#rsvps-in` / `ul#rsvps-out` of `li.rsvp-name`, or `#no-rsvps-in` / `#no-rsvps-out`. While planned, a bottom sheet `details#move.sheet` (`summary#move-open`) at the end of `main#lunch` holds `form#move-form` (`input#move-date` with `min` today and the current date as value, `#move-button`, posting to `/lunches/{id}/date`; errors `#move-date-in-past` and `#move-date-required` with `422`). The cards on `/` and `/lunches` show `.lunch-in` with `data-count`. `LunchesPage.Templates.lunch` hit Checkstyle's 7-parameter limit, so the move form is `MoveForm(today, draft)` and the Candidate Venue form became `CandidateForm(addable, draft)` (template: `candidateForm.addable`, `candidateForm.draft`). Ticket 06 should group its pick state the same way rather than add parameters.
- **DSL and drivers:** `lunches.rsvp("lunch", "answer: in|out", "expect: success|notPlanned")`, `lunches.assertRsvps("lunch", "in: a, b", "out: c")` (comma-separated Member names through `MembersDsl.realName`, empty for nobody, exactly these), `lunches.move("lunch", "date", "expect: success|dateInPast")`. The UI driver can't answer for a Lunch that isn't planned, so `assertRsvpRejectedAsNotPlanned` checks the buttons are gone; the API driver checks the `409`. `assertRsvps` also checks the `in` count: API on the detail and the list, UI on the home card's `data-count` when the Lunch is the Planned Lunch.
