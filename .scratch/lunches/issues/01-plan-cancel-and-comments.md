# 01 Plan, cancel and talk over a Lunch

Status: ready-for-agent
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
