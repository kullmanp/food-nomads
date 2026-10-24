# Walking skeleton

Status: ready-for-agent

## Goal

The thinnest slice of real behaviour that goes through every layer and every part of the pipeline: a DSL acceptance test, both protocol drivers, the packaged app, PostgreSQL, the passkey login, and CI. Later features should only add behaviour, not structure.

The behaviour is deliberately small:

1. The first Member joins through the Bootstrap Invite and registers a passkey.
2. A Member logs out and logs back in with their passkey.
3. A Member proposes a Venue, and it appears in the list of candidates.
4. Someone who isn't logged in sees none of this.

## Acceptance tests (proposal, for human review)

The final test code is drafted in the DSL and reviewed before implementation (ADR-0004). These sketches show intent and DSL shape.

```java
class MemberAccessAcceptanceTest extends AcceptanceTest {

    @Test
    void memberCanLogOutAndLogBackInWithTheirPasskey() {
        members.logIn("member: founder");
        members.logOut();
        members.assertLoggedOut();

        members.logIn("member: founder");
        members.assertLoggedIn("member: founder");
    }

    @Test
    void visitorWhoIsNotLoggedInCannotSeeVenues() {
        venues.assertNotAccessibleWithoutLogin();
    }

    @Test
    void bootstrapInviteCannotBeUsedOnceAMemberExists() {
        members.assertBootstrapInviteRejected();
    }
}

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

### Bootstrap Invite vs functional isolation

The Bootstrap Invite exists only while there are no Members, so it's global state that every test shares. It doesn't fit functional isolation. We resolve this in the test infrastructure instead of in the tests:

- Once per suite run, the harness starts a fresh app, reads the Bootstrap Invite link from the app's log, and redeems it through the **active protocol driver**. That creates a Member with the alias `founder`. Every run therefore exercises the bootstrap flow on the chosen driver.
- Individual tests use `founder` and never touch the bootstrap themselves, except to check that it's rejected.
- Once Invites exist (next feature), tests will create their own Members instead of sharing `founder`.

## Scope

### Repo structure

- Multi-module Maven: parent, `app`, `acceptance-tests`. Java 25, current Quarkus LTS, Maven wrapper.
- `app` packages as a container image.

### App

- `Member` with a display name, plus passkey credentials (`quarkus-security-webauthn`), stored in PostgreSQL with Flyway migrations.
- Bootstrap Invite: if there are no Members at startup, generate a single-use token and log the join URL. Registering with it creates the first Member. Afterwards it is rejected.
- Log in / log out with a passkey.
- `Venue` with name (required) and address (optional), the proposing Member, and the time it was proposed. The candidate list shows all Venues, newest first. Voting and visiting come later.
- One shared application layer (ADR-0003) with two thin adapters on top:
  - **UI**: Qute + htmx pages for joining, logging in, the Venue list, and proposing a Venue. Phone-first layout. Texts go through Qute message bundles, English only for now.
  - **API**: JSON under `/api` (e.g. `GET/POST /api/venues`, `GET /api/me`), protected by the same session.
- Everything except joining and logging in requires a logged-in Member.

### Acceptance tests

- The four layers: test cases → DSL → protocol drivers → system under test.
- DSL: Farley-style string parameters with defaults, plus aliasing of names (e.g. Venue names) for functional isolation. A shared `AcceptanceTest` base class exposes `members` and `venues`.
- Harness: Testcontainers starts the built app image + PostgreSQL once per run, and the Bootstrap Invite is read from the container log.
- **UI driver**: Playwright for Java, phone viewport, Chromium virtual WebAuthn authenticator.
- **API driver**: REST client plus a software WebAuthn authenticator (e.g. webauthn4j test authenticator) running the real ceremony against Quarkus's WebAuthn endpoints.
- Credentials per Member alias are kept in a driver-side store, so each test's fresh browser context or HTTP client can import `founder`'s passkey (via CDP `WebAuthn.addCredential` for the UI driver).
- Driver selection: `-Ddriver=ui|api`, default `api`.

### CI (GitHub Actions)

- Build, unit tests, image build, acceptance suite with `driver=api` and `driver=ui` as two jobs.
- On `main`, push the image to GHCR.

## Out of scope

- Invites for new Members, Recovery Links, deactivation
- Votes, Lunches, RSVPs, comments, Photos, Successor Venues
- German translation and the language setting
- PWA manifest and service worker
- Dev-mode seed data and the "log in as …" picker
- Deployment to the Proxmox VM (Caddy, compose, backups)

## Risks to check early

- **Sharing passkeys across test contexts**: importing a stored credential into a fresh Chromium virtual authenticator, and into the software authenticator, needs a spike in the first slice.
- **Bootstrap token in registration**: `quarkus-security-webauthn` must be made to accept registration only with a valid Invite token. Check how much of the default registration flow can be customised.
- **Testcontainers + Playwright in GitHub Actions**: browser install and Docker availability in the runner.

## Proposed slices

1. Repo structure, CI and harness skeleton. The app serves a placeholder page, and one acceptance test proves both drivers reach it.
2. Bootstrap Invite + passkey registration, so the `founder` setup works on both drivers.
3. Log in / log out, plus the visitor-is-blocked check.
4. Propose a Venue and list candidates.
