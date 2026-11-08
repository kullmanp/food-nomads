# 01 Create and redeem an Invite

Status: resolved
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

## Comments

Notes for later tickets:

- **Application layer:** `members.Invites` holds the rules: `create(member)`, `joinUrl(invite)`, `isOpen(token)` and package-private `redeem(token, member)`. `Invite.isOpenAt(now)` is the one place that decides whether an Invite is still open, and `Invite.expiresAt()` is `createdAt` plus 7 days. `Membership.isInviteValid` and `checkInvite` accept the Bootstrap Invite or an open stored Invite. `Membership.join` picks one and redeems it, and the stored Invite then records `redeemedBy` and `redeemedAt`. Table `invite` comes from `V4__invites.sql`. There is no revocation column yet: ticket 04 adds it in a new migration and extends `isOpenAt`.
- **Atomic redeem:** `InviteRepository.findForUpdate` takes a row lock (`PESSIMISTIC_WRITE`) for the transaction of `Membership.join`, so the check-then-set stays in `Invites`. A join that fails later rolls back and leaves the Invite open. Checked once by hand with concurrent joins on the API (15 rounds of 4 joiners, exactly one winner each). There is no permanent test for it.
- **Clock:** `ClockProducer` produces `Clock.systemUTC()` as a `@Singleton`, so any application service can take a `Clock` in its constructor. Unit tests use `members.MutableClock` (`advance(duration)`).
- **API:** `POST /api/invites` answers `201 {id, url, createdBy, expiresAt}`, where `createdBy` is the display name. `InvitesResource` has no `GET` or `DELETE` yet (ticket 04). The join endpoints are unchanged and accept stored Invites through `Membership.checkInvite`. Their name checks are still as before (ticket 02), and they don't look at who is logged in (ticket 03).
- **UI:** the home page links to `/invites` (`#invites-link`). `main#invites` has `form#create-invite-form` with `#create-invite-button`. After creating, `section#new-invite` shows `input#invite-link`, `#copy-invite`, `#invite-copied` (status text) and `#share-invite` (hidden unless `navigator.share` exists). The page script listens on `body` because the section arrives through an htmx swap. The page only shows the Invite just created, so without JavaScript the post answers the page itself instead of a `303`.
- **DSL:** `invites.create("invite: alias")` and `invites.assertNotAccessibleWithoutLogin()`. `members.join("invite: alias", "displayName: Name", "expect: success|rejected")`: tickets 02 and 03 add their `expect` values to the switch in `MembersDsl.join`. `InviteLinks` maps an Invite alias to its link, one instance per test, shared by `InvitesDsl` and `MembersDsl`. `Member` names are aliased in `MembersDsl.realName`, except `founder`, so `members.logIn`, `assertLoggedIn` and `join` all see the same real name. Tests can't use the alias of another test's Member.
- **Links in tests:** the app builds links on `foodnomads.base-url`, which is `http://localhost:8080` in the container, so `InvitesDsl.create` points them at the mapped port with `SystemUnderTest.pointedAtApp`. Ticket 04 can compare links by path or use the same method.
- **Drivers:** `InviteDriver` has `create()` (returns the link as the app gave it) and `assertNotAccessibleWithoutLogin()`. `MemberDriver.assertBootstrapInviteRejected` became `assertInviteRejected(joinUrl, displayName)`, used for both kinds of Invite.
- **UI driver passkeys:** one browser context now acts as several people in a test. `VirtualAuthenticator.holdPasskey` replaces whatever is on the authenticator, and `UiMemberDriver.join` calls `removePasskeys()` first, so `onlyPasskey()` and discoverable login each see one credential. The context grants `clipboard-write` so the driver can press Copy.
