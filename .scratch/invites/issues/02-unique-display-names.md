# 02 Unique display names

Status: resolved
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

## Comments

Notes for later tickets:

- **Application layer:** `Membership.checkDisplayName(input)` strips the name and returns it, or throws `DisplayNameRequiredException` (blank) or `DisplayNameTakenException` (another Member has it, ignoring case). `Membership.join` calls it before redeeming, so a refused name leaves the Invite open.
- **Ignoring case:** the application layer lowercases with `Locale.ROOT` and asks `MemberRepository.existsWithLowercaseDisplayName`. The database compares with `lower(display_name)`. The two can differ for rare characters (e.g. `İ`); then the unique index in `V5__unique_display_names.sql` still decides.
- **Race:** the index `member_display_name_lower_key` stops two people picking one name at once. `PanacheMemberRepository.add` uses `persistAndFlush` and turns that violation into `DisplayNameTakenException`, so the loser gets `409`, and the rollback leaves their Invite open. Checked once by hand with the application check switched off (25 rounds of 2 joiners with different Invites and the same name: one `201`, one `409`, loser's Invite still open). There is no permanent test for it.
- **API:** `GET /api/join/{token}/options` and `POST /api/join/{token}` answer `409 {"error":"display-name-taken"}` and `400 {"error":"display-name-required"}`. The Invite is checked first, so `403` wins over a bad name. Ticket 03's `already-member` check belongs before those. The fallback to "Member" is gone.
- **UI:** the join page shows `#join-name-taken`, `#join-name-required` or the general `#join-error`, and picks one from the `error` field of the options or POST response. `registerClientSteps` puts the failed `Response` in `error.cause`. The form has `novalidate`, because the browser's own `required` bubble would stop the submit and the app's message would never appear.
- **DSL and drivers:** `members.join(..., "expect: nameTaken|nameRequired")` calls `assertDisplayNameTaken` or `assertDisplayNameRequired`. The UI driver checks `VirtualAuthenticator.passkeyCount()` is 0. The API driver checks the refused options carry no challenge, in the body or as a `_quarkus_webauthn_challenge` cookie, then POSTs with a passkey from Quarkus's own challenge and checks it's refused and can't log in.
