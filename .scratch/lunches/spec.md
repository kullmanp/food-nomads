# Lunches

Status: ready-for-agent

## Goal

Members arrange their Lunches in the app: someone plans a date, the group talks it over, puts up Venues and votes on them, says who's coming, picks a Venue and finally marks the Lunch as held. The rule "never the same place twice" is enforced from then on, including for the Lunches before the app (see `GLOSSARY.md`: Lunch, Planned Lunch, Held Lunch, Cancelled Lunch, Candidate Venue, Vote, RSVP, Lunch Comment, Visited Venue).

All Members have equal rights: anything below that "a Member" can do, any Member can do, also on a Lunch someone else planned.

## Behaviour

### The Planned Lunch

- There is at most one Planned Lunch. A Member plans one, with a date and nothing else, when there is none. Planning while one exists is rejected, and the page shows the existing one instead of the form.
- The date is today or later. "Today" is the date in Basel (`Europe/Zurich`). There's no time: that's arranged in the chat.
- A Member moves the Planned Lunch to another date, today or later. Its comments, Candidate Venues and Votes stay. Its RSVPs are cleared, because "in" for Tuesday says nothing about Thursday.
- After its date, the Lunch stays planned until a Member marks it held or cancels it, and no new one can be planned until then. The home page then asks "Did this Lunch take place?" and offers Held and Cancel.
- A Member cancels the Planned Lunch. It becomes a Cancelled Lunch, stays visible with its comments, Votes and RSVPs, and leaves its Venues unvisited.
- The home page shows the Planned Lunch (its date, its Venue or "no Venue yet", how many Members are in) and links to it. Without one, it offers to plan one.
- `/lunches` shows the Planned Lunch, and below it the Held and Cancelled Lunches, latest first.

### Lunch Comments

- A Lunch's page shows its Lunch Comments as a chat, oldest first, each with its author and time.
- A Member can comment on any Lunch: planned, held or cancelled. A blank comment is rejected.
- Comments can't be edited or deleted.
- No notifications of any kind. The group's own chat stays the place to say "there's a new Lunch in the app".

### Recording a Lunch from before the app

- A Member records a Held Lunch directly, with a date before today and a Venue. It has no Candidate Venues, Votes or RSVPs, but can be commented on. It doesn't count as the Planned Lunch, so it can be recorded while one is planned.
- The Venue becomes a Visited Venue. A Venue that's already visited is rejected, and so is the Venue the Planned Lunch has picked.
- The Venue list marks Visited Venues.

### Candidate Venues

- A Member puts up any unvisited Venue as a Candidate Venue of the Planned Lunch. A visited one, or one that's already a Candidate Venue, is rejected.
- A Member removes a Candidate Venue that isn't picked. Its Votes go with it.
- A Candidate Venue that becomes visited (a past Lunch is recorded there) stays listed with its Votes and is marked visited. It can't be picked, and a Member can remove it.
- Only the Planned Lunch takes Candidate Venues.

### Votes

- A Member votes up or down on a Candidate Venue. They have at most one Vote per Candidate Venue and can change it to the other direction or withdraw it.
- Each Candidate Venue shows its number of up and down Votes and the Member's own Vote, but not who voted. Candidate Venues are listed by up minus down, highest first, then by name.
- Votes guide the pick. They decide nothing.
- Only the Planned Lunch takes Votes. Afterwards they stay visible as they were.

### RSVP

- A Member answers in or out for the Planned Lunch, and can change their answer. A Member who hasn't answered has no RSVP.
- The Lunch shows who is in and who is out, by display name.
- Only the Planned Lunch takes RSVPs. Afterwards they stay visible as they were.

### Picking a Venue and holding the Lunch

- A Member picks one of the unvisited Candidate Venues as the Lunch's Venue, and can change the pick or clear it while the Lunch is planned.
- A Member marks the Planned Lunch as held, on or after its date and only once it has a Venue. Its Venue becomes a Visited Venue. Nothing becomes held by itself.
- Held and Cancelled are final. There's no undo in this feature.

## Design notes

- New package `lunches` in the shared application layer (ADR-0003), behind a `LunchRepository` with an in-memory fake (ADR-0005). Candidate Venues, Votes, RSVPs and Lunch Comments belong to a Lunch.
- **At most one Planned Lunch** is a rule across Lunches. Enforce it in `Lunches` and back it with a partial unique index (`where state = 'planned'`), so two Members planning at once can't both succeed. Turn the violation into the same rejection, as `PanacheMemberRepository` does for display names.
- **Visited is derived, not stored.** A Venue is visited when a Held Lunch has it as its Venue. `lunches` depends on `venues`, never the reverse. The Venue list asks `lunches` which Venues are visited. Add an ArchUnit rule for the direction.
- **"Never twice" needs a lock.** Holding the Planned Lunch and recording a past Lunch at the same Venue at once must not both succeed. Lock the Venue row (`findForUpdate`, as for Invites) when holding and recording. State changes of the Planned Lunch lock its row.
- **Today** comes from the injected `Clock` in `Europe/Zurich`. Unit tests cover planning and moving to today, recording yesterday, and holding on the date, around midnight in Basel.
- **The word "candidate" is taken.** The walking skeleton calls the Venue list "candidates" (`Venues.candidates()`, `ul#candidates`, `li.candidate`, `VenuesDsl.assertCandidate`, `VenueDriver.assertCandidate`). Ticket 03 renames these to "Venues" (`Venues.newestFirst()`, `ul#venues`, `li.venue`, `venues.assertListed`). `ProposeVenueAcceptanceTest` is a reviewed contract: the rename needs the human's approval in ticket 03's red review.
- Dates are shown with the existing English pattern, as on the Venue page.

## API

| Call | Result |
| --- | --- |
| `POST /api/lunches` `{date}` | `201` Lunch summary |
| `POST /api/lunches/past` `{date, venueId}` | `201` Lunch summary, held |
| `GET /api/lunches` | The Planned Lunch, then Held and Cancelled latest first, as summaries |
| `GET /api/lunches/planned` | `200` Lunch summary, or `204` without a Planned Lunch |
| `GET /api/lunches/{id}` | Lunch detail |
| `PUT /api/lunches/{id}/date` `{date}` | `204` |
| `POST /api/lunches/{id}/comments` `{text}` | `201 {id, author, text, writtenAt}` |
| `POST /api/lunches/{id}/candidates` `{venueId}` | `201` |
| `DELETE /api/lunches/{id}/candidates/{venueId}` | `204` |
| `PUT /api/lunches/{id}/candidates/{venueId}/vote` `{direction: up\|down}` | `204` |
| `DELETE /api/lunches/{id}/candidates/{venueId}/vote` | `204` |
| `PUT /api/lunches/{id}/rsvp` `{answer: in\|out}` | `204` |
| `PUT /api/lunches/{id}/venue` `{venueId}` | `204`; `DELETE` clears the pick |
| `POST /api/lunches/{id}/held` | `204` |
| `POST /api/lunches/{id}/cancelled` | `204` |

Lunch summary: `{id, date, state: planned|held|cancelled, venue: {id, name}|null, in: n}`.
Lunch detail: the summary plus `candidates: [{venue, visited, up, down, myVote}]`, `rsvps: {in: [names], out: [names]}` and `comments: [{author, text, writtenAt}]`.

| Error | Meaning |
| --- | --- |
| `404` | Unknown Lunch or Venue |
| `400 {"error":"date-in-past"}` | Planning or moving to before today |
| `400 {"error":"date-not-past"}` | Recording a past Lunch today or later |
| `400 {"error":"text-required"}` | Blank comment |
| `409 {"error":"lunch-already-planned"}` | Planning while a Lunch is planned |
| `409 {"error":"lunch-not-planned"}` | Changing date, Candidates, Votes, RSVPs or pick, or holding or cancelling, a Held or Cancelled Lunch |
| `409 {"error":"venue-visited"}` | Candidate, pick or past Lunch with a Visited Venue |
| `409 {"error":"venue-picked"}` | Recording a past Lunch at the Venue the Planned Lunch has picked |
| `409 {"error":"already-candidate"}` | The Venue is already a Candidate Venue |
| `409 {"error":"not-a-candidate"}` | Vote on, or pick of, a Venue that isn't a Candidate Venue of this Lunch |
| `409 {"error":"candidate-picked"}` | Removing the picked Candidate Venue |
| `409 {"error":"lunch-not-due"}` | Marking held before the date |
| `409 {"error":"no-venue"}` | Marking held without a Venue |

## Acceptance tests

Sketches live in the tickets. The final test code is drafted red and reviewed per ticket (ADR-0004).

- New DSL entry point `lunches`. A Lunch alias maps to the id the driver got back, like Invite links.
- **The Planned Lunch is shared state.** Aliases can't isolate "at most one". Every test that plans a Lunch starts with `lunches.noPlannedLunch()`, which cancels whatever Lunch is planned through the normal cancel, with no backdoor. That works because the acceptance tests run one after another. Lunch tests can't run in parallel; if the suite is ever parallelised, they need a shared `@ResourceLock`.
- Dates are given relative to today in Basel: `date: today`, `date: +7`, `date: -60`. A test running across midnight in Basel could fail; accepted.
- Venue names are aliased in `VenuesDsl` today. `LunchesDsl` must resolve the same aliases, so the two share one `Aliases` instance per test, as `MembersDsl` and `InvitesDsl` share `InviteLinks`.
- Tests that need a second Member create one through an Invite, as in the Invites tests.

## Out of scope

- Notifications (email, push, PWA)
- Editing or deleting Lunch Comments; undoing held or cancelled
- Photos, Venue Comments, Successor Venues, Recovery Links, deactivation
- German translation. New texts go into the English message bundle.

## Tickets

1. [Plan, cancel and talk over a Lunch](issues/01-plan-cancel-and-comments.md)
2. [Record a Lunch from before the app](issues/02-record-past-lunch.md)
3. [Candidate Venues](issues/03-candidate-venues.md)
4. [Votes](issues/04-votes.md)
5. [RSVP and moving the date](issues/05-rsvp-and-moving-the-date.md)
6. [Pick a Venue and hold the Lunch](issues/06-pick-and-hold.md)
