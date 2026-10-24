# 03 Log in and log out with a passkey

Status: claimed
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
