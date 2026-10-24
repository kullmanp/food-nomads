# 03 Log in and log out with a passkey

Status: resolved
Blocked by: 02
Spec: [../spec.md](../spec.md)

## What

- A Member logs out, and logs back in with their passkey (UI and `/api`).
- `GET /api/me` returns the logged-in Member.
- Everything except joining and logging in requires a logged-in Member: the UI redirects to login, and the API answers 401.

## Acceptance tests

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
}
```

`venues.assertNotAccessibleWithoutLogin()` needs a protected Venue list page and `GET /api/venues` that exist but are empty. Ticket 04 fills them.

## Done when

Both drivers are green.

## Comments

Notes for later tickets:

- **Login:** `GET /api/login/options` and `POST /api/login` (the credential JSON as the body) run the WebAuthn login ceremony with discoverable credentials, so no username is needed. Success is `200` with the Member (`{displayName}`). A rejected passkey is `401 {"error":"passkey-rejected"}`. The `/login` page runs `loginClientSteps` from `webauthn.js` against these endpoints and then goes to `/`. It doesn't return to the page that sent you to login yet.
- **Logout:** `POST /api/logout` answers `204`. The home page's logout button posts the form to `POST /logout`, which answers `303` to `/`.
- **Access:** deny by default through `quarkus.http.auth.permission.*` in `application.properties`. The `public` list holds `/`, joining, logging in, `/api/status` and `/q/*`. A new public path has to be added there. Anything else needs a logged-in Member. `ApiAuthenticationChallenge` makes `/api/*` answer 401, while pages get the WebAuthn mechanism's redirect to `/login`. The home page stays public: it's the landing page with the "Log in" link, and the UI's system-availability check loads it without logging in.
- **Venues:** `/venues` (`main#venues`, `#no-venues`) and `GET /api/venues` (`[]`) exist but are empty, ready for ticket 04. Once logged in, the home page links to the venues page (`#venues-link`).
- **Passkeys in the drivers:** the UI driver imports a passkey into each fresh browser context with `VirtualAuthenticator.holdPasskey`. Chromium raises the sign count on every login, so after each login the driver stores the passkey back into `Passkeys`. The API driver's `SoftwareAuthenticator.logIn` always signs with count 0.
- **DSL:** `members.logIn("member: …")` and `assertLoggedIn` pass the Member name through unaliased (see ticket 02's note about `founder`).
