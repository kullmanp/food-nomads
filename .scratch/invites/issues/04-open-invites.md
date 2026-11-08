# 04 List, re-share and revoke Open Invites

Status: resolved
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

## Comments

Notes for later tickets:

- **Application layer:** `Invites.open()` is the Open Invites, newest first, with `createdBy` loaded. `Invites.revoke(id)` sets `revokedAt` if the Invite is still open and otherwise changes nothing, also for an unknown id. `Invite.isOpenAt` stays the one place that decides, so the list, redeeming and revoking all agree. Column `revoked_at` comes from `V6__revoke_invites.sql`. Who revoked isn't recorded: the spec doesn't ask for it, and it would mean nothing for the Deactivation case below.
- **Deactivation:** deactivating a Member should revoke their Open Invites. Add a repository query for a creator's Invites, and loop over `Invites.revoke`, or give `Invites` a `revokeAllBy(member)` in the same transaction. The `Membership.current` check stays the gate for writes on `/invites` and `/api/invites`, so a deactivated Member's old session can't revoke either, as long as `current` stops finding them.
- **Concurrency:** `InviteRepository.findForUpdate(UUID)` takes the same row lock as redeeming. Hibernate writes all columns of an entity, so an unlocked revoke could overwrite a redemption that committed in between.
- **Filtering in memory:** `InviteRepository.newestFirst()` returns every Invite ever made, because expiry is decided by `Invite.isOpenAt` and the `Clock`, not in a query. Fine for a small group.
- **API:** `GET /api/invites` is `200` with the shape of `POST`, and `DELETE /api/invites/{id}` answers `204`, also for an unknown or no longer open id.
- **UI:** `/invites` now shows the list after the new-link section: `ul#open-invites > li.open-invite` with `.invite-link`, `.invite-created-by`, `.invite-expires` (`time`), `.share-invite`, `.copy-invite`, `.invite-copied` and `.revoke-invite`. Copy and Share work on the closest `.invite`. `#new-invite` keeps its ids for the existing driver.
- **Without JavaScript:** `POST /invites` and `POST /invites/{id}/revoke` answer `303 /invites`, as the Venue page does, so a reload doesn't create another Invite. The new link is in the list. With htmx, `POST /invites` still swaps in `#new-invite`. The redirects aren't exercised by an acceptance test, as with Venues.
- **DSL and drivers:** `invites.assertOpen("invite: x", "createdBy: member")`, `assertNotOpen` and `revoke` find an Invite by the path of its link, because the app builds links on its own base URL. `InvitesDsl` asks `MembersDsl.realName` for `createdBy`. Newest first in the database is only covered by the fake and was checked once on the API driver (no permanent test), as was the per-row Copy on the UI driver.
