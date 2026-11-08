# 03 Already a Member

Status: resolved
Blocked by: 01
Spec: [../spec.md](../spec.md)

## What

A logged-in Member who opens an Invite link doesn't create a second Member by accident.

- App: the rule lives in `Membership` (ADR-0003), and the join endpoints answer `409 already-member` to a logged-in caller.
- It's checked before the Invite, so a logged-in Member gets this answer even for an invalid link.
- UI: the join page says the visitor is already a Member and offers no form.
- The Invite isn't used up. After logging out, the same link works.

## Acceptance tests

```java
class AlreadyAMemberAcceptanceTest extends AcceptanceTest {
    @Test
    void memberCannotJoinAgainAndTheInviteStaysValid() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");

        members.join("invite: forAnna", "displayName: Anna", "expect: alreadyMember");

        members.logOut();
        members.join("invite: forAnna", "displayName: Anna");
        members.assertLoggedIn("member: Anna");
    }
}
```

## Done when

- Both drivers are green.

## Comments

Notes for later tickets:

- **Application layer:** `Membership.checkCanJoin(caller, token)` is the one check the join adapters call. It throws `AlreadyMemberException` for a logged-in Member, whatever the token, then `InviteRejectedException`. The display-name checks come after it. `isInviteValid` and `checkInvite` are package-private now, so adapters can't skip the first check. `Membership.join` doesn't repeat it: the POST adapter has already called `checkCanJoin`.
- **Who counts as logged in:** `Membership.current` finds a Member. A valid session whose Member no longer exists counts as a visitor and can join. Deactivation will need to decide this again.
- **Identity on public paths:** `/join/*` and `/api/join/*` are `permit`, but an injected `SecurityIdentity` is still the logged-in caller there. The acceptance test proves it through both drivers: without that identity the `409` wouldn't appear.
- **API:** `GET /api/join/{token}/options` and `POST /api/join/{token}` answer `409 {"error":"already-member"}` before anything else, so it wins over `403` and the name errors.
- **UI:** the join page answers `409` with `JoinPage/alreadyMember.html`: `main#join-already-member`, no form, and `#join-go-home` to the home page, where the log-out button is. A Member who logs in on another tab and then submits an open join form gets the general `#join-error`.
- **DSL and drivers:** `members.join(..., "expect: alreadyMember")` calls `assertAlreadyMember`, which checks the given link and an unknown one, and that no passkey was registered. `ApiMemberDriver.assertDisplayNameRefused` became `assertJoinRefused(joinUrl, displayName, status, error)`, for any refused join.
