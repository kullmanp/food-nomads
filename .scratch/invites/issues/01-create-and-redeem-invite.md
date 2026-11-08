# 01 Create and redeem an Invite

Status: ready-for-agent
Blocked by: none
Spec: [../spec.md](../spec.md)

## What

A Member creates an Invite, and a new person joins through its link (see `GLOSSARY.md`: Invite).

- App: an Invite is stored with who created it and when, and which Member redeemed it and when. It's single-use and expires 7 days after it was created. `Membership` accepts stored Invites next to the Bootstrap Invite and consumes them atomically. Only a successful join uses one up.
- UI: `/invites` (logged in, linked from the home page) creates an Invite and shows the link with Share and Copy. Phone-first, htmx like the Venue page.
- API: `POST /api/invites`. The existing join endpoints accept the new Invites.
- Expiry goes through an injected `Clock`, with unit tests for just before, exactly at and after 7 days.
- DSL: a new `invites` entry point. Invite aliases map to the link the driver got back. Member names are aliased, except `founder`.

## Acceptance tests

```java
class InviteAcceptanceTest extends AcceptanceTest {
    @Test
    void invitedPersonJoinsAndIsLoggedIn() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();

        members.join("invite: forAnna", "displayName: Anna");

        members.assertLoggedIn("member: Anna");
    }

    @Test
    void newMemberCanLogBackInWithTheirPasskey() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");
        members.logOut();

        members.logIn("member: Anna");

        members.assertLoggedIn("member: Anna");
    }

    @Test
    void inviteCanBeRedeemedOnlyOnce() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");
        members.logOut();

        members.join("invite: forAnna", "displayName: Ben", "expect: rejected");
    }

    @Test
    void visitorWhoIsNotLoggedInCannotCreateInvites() {
        invites.assertNotAccessibleWithoutLogin();
    }
}
```

## Done when

- Both drivers are green.
- Expiry is covered by unit tests and no mutant survives.
