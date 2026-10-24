# 04 Propose a Venue

Status: resolved
Blocked by: 03
Spec: [../spec.md](../spec.md)

## What

A Member proposes a Venue, and it appears among the candidates (see `GLOSSARY.md`: Venue).

- A Venue has a name (required), an address (optional), the proposing Member, and the time it was proposed.
- The candidate list shows all Venues, newest first. Votes and Visited Venues come later.
- UI: a phone-first list page with an htmx "propose" form. API: `GET/POST /api/venues`.
- Business rules live in the shared application layer, not in the adapters (ADR-0003).
- DSL: Venue names are aliased for functional isolation.

## Acceptance tests

```java
class ProposeVenueAcceptanceTest extends AcceptanceTest {
    @Test
    void proposedVenueAppearsAmongTheCandidates() {
        members.logIn("member: founder");

        venues.propose("name: Sternen", "address: Spalenberg 1, Basel");

        venues.assertCandidate("name: Sternen", "address: Spalenberg 1, Basel", "proposedBy: founder");
    }

    @Test
    void venueNeedsAName() {
        members.logIn("member: founder");

        venues.propose("name: ", "expect: rejected");
    }
}
```

## Done when

- Both drivers are green.
- The walking skeleton spec's goal is met.

## Comments

Notes for later tickets:

- **Application layer:** `venues.Venues` holds the rules: `propose(member, name, address)` strips both, rejects a blank name with `IllegalArgumentException` and stores a blank address as `null`. `candidates()` returns all Venues newest first, with the proposing Member fetched. Table `venue` comes from `V3__venues.sql`.
- **API:** `GET /api/venues` returns `[{id, name, address, proposedBy, proposedAt}]`, where `proposedBy` is the display name and `address` may be `null`. `POST /api/venues` with `{name, address}` answers `201` with the Venue, or `400 {"error":"name-required"}`.
- **UI:** `/venues` has `form#propose-form` (`#venue-name`, `#venue-address`, `#propose-button`) and `ul#candidates` of `li.candidate` (`.venue-name`, `.venue-address`, `.venue-proposed-by`, `<time>`). The form uses htmx to post to `POST /venues`, which re-renders the page; htmx swaps in `main#venues`. A rejected proposal answers `422` and shows `#propose-error`. The page's `htmx-config` meta tag makes htmx swap 422 responses. Without htmx, success answers `303` to `/venues`. The name field has no `required` attribute, so the server rule is what rejects an empty name.
- **htmx:** comes from the `org.webjars.npm:htmx.org` webjar (version `htmx.version` in `app/pom.xml`, 2.x) through `quarkus-web-dependency-locator`, so pages load `/webjars/htmx.org/dist/htmx.min.js` without a version. `/webjars/*` isn't public, so a page that loads htmx before login needs it added to the `public` paths.
- **Dates:** `VenuesPage` shows dates in `Europe/Zurich` with an English pattern. This will move to the message bundles and a locale setting with the German translation.
- **UI driver:** don't use `Pattern.quote` in Playwright text or regex options. Java's `\Q…\E` isn't valid JavaScript regex, so the locator silently matches nothing. Use `getByText(…, setExact(true))`.
