# 02 Unique display names

Status: claimed
Blocked by: 01
Spec: [../spec.md](../spec.md)

## What

No two Members share a display name, ignoring case (see `GLOSSARY.md`: Member).

- App: `GET /api/join/{token}/options` rejects taken and blank names before the passkey prompt, with `409 display-name-taken` and `400 display-name-required`. It no longer falls back to "Member". `POST /api/join/{token}` checks the name again.
- Database: a unique index on the lowercased display name, for two people picking the same name at once.
- UI: the join form shows the problem right there, and no passkey prompt appears.
- A rejected name leaves the Invite valid.
- Ignoring case is covered by unit tests, because `Aliases` gives each spelling its own suffix.

## Acceptance tests

```java
class UniqueDisplayNameAcceptanceTest extends AcceptanceTest {
    @Test
    void displayNameMustBeUniqueAndARejectedNameLeavesTheInviteValid() {
        members.logIn("member: founder");
        invites.create("invite: first");
        invites.create("invite: second");
        members.logOut();
        members.join("invite: first", "displayName: Anna");
        members.logOut();

        members.join("invite: second", "displayName: Anna", "expect: nameTaken");

        members.join("invite: second", "displayName: Ben");
        members.assertLoggedIn("member: Ben");
    }

    @Test
    void displayNameIsRequired() {
        members.logIn("member: founder");
        invites.create("invite: forAnna");
        members.logOut();

        members.join("invite: forAnna", "displayName: ", "expect: nameRequired");
    }
}
```

## Done when

- Both drivers are green.
- A rejected name causes no passkey registration, which both drivers check.
