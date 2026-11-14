# 01 Plan, cancel and talk over a Lunch

Status: resolved
Blocked by: none
Spec: [../spec.md](../spec.md)

## What

A Member plans the Lunch, the group talks it over in its Lunch Comments, and a Member can call it off (see `GLOSSARY.md`: Planned Lunch, Cancelled Lunch, Lunch Comment).

- App: new package `lunches` with `Lunches`, `Lunch` and a `LunchRepository` with an in-memory fake (ADR-0005). A Lunch has a date and a state, planned or cancelled for now. There is at most one Planned Lunch: planning while one exists is rejected, backed by a partial unique index on the planned state. Planning before today in `Europe/Zurich` is rejected. Cancelling makes it a Cancelled Lunch. A Lunch Comment has its author, text and time; a blank one is rejected. Comments work on cancelled Lunches too.
- UI: `/lunches` (logged in, linked from the home page) plans a Lunch when none is planned, and lists the Planned Lunch and the Cancelled ones. `/lunches/{id}` shows the date, "no Venue yet", Cancel and the chat with a comment form, oldest first. The home page shows the Planned Lunch and links to it, or offers to plan one.
- API: `POST /api/lunches`, `GET /api/lunches`, `GET /api/lunches/planned`, `GET /api/lunches/{id}`, `POST /api/lunches/{id}/cancelled`, `POST /api/lunches/{id}/comments`.
- Today comes from the injected `Clock` in `Europe/Zurich`, with unit tests around midnight in Basel.
- DSL: a new `lunches` entry point. Lunch aliases map to the id the driver got back. Dates are relative: `today`, `+7`, `-60`. `lunches.noPlannedLunch()` cancels whatever Lunch is planned, through the normal cancel.

## Acceptance tests

```java
class PlanLunchAcceptanceTest extends AcceptanceTest {
    @Test
    void plannedLunchHasNoVenueYet() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();

        lunches.plan("lunch: friday", "date: +7");

        lunches.assertPlanned("lunch: friday", "date: +7", "venue: none");
    }

    @Test
    void lunchCanBePlannedForToday() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();

        lunches.plan("lunch: now", "date: today");

        lunches.assertPlanned("lunch: now", "date: today", "venue: none");
    }

    @Test
    void lunchCannotBePlannedInThePast() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();

        lunches.plan("lunch: yesterday", "date: -1", "expect: dateInPast");
    }

    @Test
    void onlyOneLunchIsPlannedAtATime() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.plan("lunch: later", "date: +14", "expect: alreadyPlanned");

        lunches.assertPlanned("lunch: friday", "date: +7", "venue: none");
    }

    @Test
    void cancelledLunchMakesRoomForTheNextOne() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.cancel("lunch: friday");

        lunches.assertCancelled("lunch: friday");
        lunches.plan("lunch: later", "date: +14");
        lunches.assertPlanned("lunch: later", "date: +14", "venue: none");
    }

    @Test
    void membersTalkALunchOver() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        invites.create("invite: forAnna");
        lunches.plan("lunch: friday", "date: +7");
        lunches.comment("lunch: friday", "text: Only if we stay near Barfi");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        lunches.comment("lunch: friday", "text: Barfi suits me");

        lunches.assertComment("lunch: friday", "by: founder", "text: Only if we stay near Barfi");
        lunches.assertComment("lunch: friday", "by: Anna", "text: Barfi suits me");
    }

    @Test
    void commentNeedsText() {
        members.logIn("member: founder");
        lunches.noPlannedLunch();
        lunches.plan("lunch: friday", "date: +7");

        lunches.comment("lunch: friday", "text: ", "expect: textRequired");
    }

    @Test
    void lunchesAreNotAccessibleWithoutLogin() {
        lunches.assertNotAccessibleWithoutLogin();
    }
}
```

## Done when

- Both drivers are green.
- The oldest-first chat, the order of `/lunches` and planning today at the midnight boundary are covered by unit tests.

## Comments

Notes for later tickets:

- **Application layer:** `lunches.Lunches` holds the rules: `plan(date)`, `cancel(id)`, `comment(lunchId, author, text)`, `planned()`, `lunch(id)` (comments and authors loaded) and `all()` (the Planned Lunch first, then the others by date, latest first, then latest planned first). `today()` is the date in `Europe/Zurich` from the injected `Clock`. `Lunch.chat()` is the one place that orders comments, oldest first (then by id). Exceptions: `DateInPastException`, `LunchAlreadyPlannedException`, `LunchNotPlannedException`, `LunchNotFoundException`, `CommentTextRequiredException`. `cancel` locks the row (`LunchRepository.findForUpdate`); later state changes of the Planned Lunch (move, pick, hold) should go through it too. A Lunch doesn't record who planned or cancelled it, since the spec doesn't ask.
- **Persistence:** tables `lunch` and `lunch_comment` come from `V7__lunches.sql`. `LunchState` is stored as `@Enumerated(STRING)`, so the values are upper case: the partial unique index is `lunch_one_planned ... where state = 'PLANNED'`, and `PanacheLunchRepository.add` turns its violation into `LunchAlreadyPlannedException` as `PanacheMemberRepository` does. There is no check constraint on `state`, so `HELD` needs no migration of the column. The race was not checked by hand. `LunchComment` needs `@Table(name = "lunch_comment")`: the naming strategy keeps entity names as they are. `Lunch.comments` is a plain `@OneToMany(mappedBy)` without cascade; `Lunches.comment` adds to it and `LunchRepository.addComment` persists (merging the detached author, as for Venues).
- **ArchUnit:** `venuesDoNotKnowTheLunches` is in place for the "visited is derived" direction.
- **Unit tests:** `members.MutableClock` is public now, so other packages can use it. Midnight in Basel is covered in winter (UTC+1) and summer (UTC+2) in `LunchesTest`.
- **API:** as in the spec, plus `400 {"error":"date-required"}` for a missing date (not in the spec). The summary is `{id, date, state, venue}` with `venue` always `null` and no `in` yet (ticket 05). The detail is the summary plus `comments: [{id, author, text, writtenAt}]`, oldest first; `candidates` and `rsvps` aren't there yet. Unknown ids answer `404`.
- **UI:** a fourth tab, `#lunches-link`. `/lunches` (`main#lunches`) shows the Planned Lunch as `a#planned-lunch.lunch-card` (`.lunch-date` `time` with an ISO `datetime`, `.lunch-venue.no-venue`), or `form#plan-form` (`input#lunch-date` type date with `min` today, `novalidate`; `#plan-button`; errors `#plan-date-in-past`, `#plan-date-required`, and `#plan-already-planned` with `409` when someone planned in between). Below: `ul#earlier-lunches > li.lunch` with `a.lunch-link` and `.lunch-state[data-state]`. `/lunches/{id}` (`main#lunch`): `#lunch-date` (`time`), `#lunch-state[data-state=planned|cancelled]`, `#lunch-no-venue`, `form#cancel-lunch-form` / `#cancel-lunch-button` only while planned (asks `confirm()` first; without JavaScript it doesn't ask), `ol#lunch-comments > li.lunch-comment` (`.comment-author`, `time`, `.comment-text`), `form#comment-form` (`textarea#comment-text`, `#comment-button`, `#comment-error` with `422`). The home page shows the same `a#planned-lunch` card or `a#plan-lunch-link`. The Lunch pages use plain form posts answering `303` (no htmx). The Venue on the cards is the place to show a picked Venue later (`#lunch-venue` on the Lunch page is what the UI driver expects then).
- **DSL and drivers:** `lunches.noPlannedLunch()`, `plan("lunch", "date", "expect: success|dateInPast|alreadyPlanned")`, `assertPlanned("lunch", "date", "venue: none|<alias>")`, `cancel`, `assertCancelled`, `comment("lunch", "text", "expect: success|textRequired")`, `assertComment("lunch", "by", "text")`, `assertNotAccessibleWithoutLogin()`. Lunch aliases map to ids in `LunchesDsl`. `VenuesDsl` and `LunchesDsl` already share one `Aliases` for Venue names, so `venue: Sternen` resolves like in `venues.propose`. The UI driver can't submit the plan form while a Lunch is planned, so `assertPlanRejectedAsAlreadyPlanned` checks the form is gone and the Planned Lunch is shown; the API driver checks the `409`. The UI driver's cancel accepts the confirm dialog with `page.onceDialog`.
