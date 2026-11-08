# 03 Already a Member

Status: ready-for-agent
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
