# 04 List, re-share and revoke Open Invites

Status: ready-for-agent
Blocked by: 01
Spec: [../spec.md](../spec.md)

## What

All Members see the Open Invites and any Member can revoke one (see `GLOSSARY.md`: Open Invite).

- App: Open Invites are those not redeemed, revoked or expired, newest first. Revoking keeps the Invite stored. Revoking one that's no longer open changes nothing.
- UI: `/invites` lists them with who created each one, when it expires, Share/Copy and Revoke, with no confirmation step.
- API: `GET /api/invites` and `DELETE /api/invites/{id}` (`204`).
- Other tests' Invites will be in the list, so the DSL finds an Invite by its link.

## Acceptance tests

```java
class OpenInvitesAcceptanceTest extends AcceptanceTest {
    @Test
    void createdInviteIsListedWithItsLink() {
        members.logIn("member: founder");

        invites.create("invite: forAnna");

        invites.assertOpen("invite: forAnna", "createdBy: founder");
    }

    @Test
    void revokedInviteIsNoLongerOpenAndCannotBeRedeemed() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");

        invites.revoke("invite: forAnna");

        invites.assertNotOpen("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna", "expect: rejected");
    }

    @Test
    void anyMemberCanRevokeAnInvite() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        invites.create("invite: forBen");
        members.logOut();
        members.join("invite: forBen", "displayName: Ben");

        invites.revoke("invite: forAnna");

        invites.assertNotOpen("invite: forAnna");
    }

    @Test
    void redeemedInviteIsNoLongerOpen() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");

        invites.assertNotOpen("invite: forAnna");
    }
}
```

## Done when

- Both drivers are green.
- Expired Invites leaving the list is covered by a unit test with a `Clock`.
