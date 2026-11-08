# Invites

Status: ready-for-agent

## Goal

Any Member can bring a new person into the group with a link (see `GLOSSARY.md`: Invite, Open Invite). Until now only the first Member can get in, through the Bootstrap Invite.

There is no email (ADR-0001), so the app only produces the link. The Member passes it on themselves, typically through the group chat.

## Behaviour

### Creating an Invite

- Any Member can create an Invite. There is no limit.
- An Invite is anonymous: it doesn't name its recipient. The new person picks their own display name when joining.
- The app shows the link with "Share" (the phone's native share sheet, where available) and "Copy".

### Redeeming an Invite

- An Invite is single-use. Only a successful join uses it up. A failed attempt (name taken, passkey cancelled) leaves it valid.
- An Invite expires 7 days after it was created, and it is expired from exactly that moment on. The period is fixed, not configurable per Invite.
- A new Member lands on the home page, logged in, the same way the first Member does. There is no onboarding and no notification to the inviter.
- An unknown, redeemed, revoked or expired Invite all get the same message, with no reason given.

### Display names

- A display name must be unique among Members, ignoring case.
- Taken and blank names are rejected before the passkey prompt, so that a rejection doesn't leave an orphaned passkey in the person's password manager. The check runs again when joining, to cover two people picking the same name at once.

### Someone who is already a Member

- A logged-in Member who opens an Invite link is told they're already a Member, and the Invite isn't used up. To let someone else join on the same device, they log out first.

### Open Invites

- All Members can see all Open Invites, newest first, each with who created it, when it expires, Share/Copy and Revoke.
- Any Member can revoke any Open Invite, without a confirmation step.
- The link of an Open Invite stays available, so a Member who closed the page before sharing can still get it.
- Redeemed, revoked and expired Invites disappear from the list, but they stay stored.

### What is recorded

An Invite records who created it and when, and which Member redeemed it and when. It isn't shown anywhere beyond the list above, and there's no activity log yet.

## Design notes

- The rules live in the shared application layer (ADR-0003), behind an `InviteRepository` with an in-memory fake (ADR-0005).
- The Bootstrap Invite stays separate: in memory, with no creator, valid only while there are no Members. `Membership.checkInvite` additionally accepts stored Invites. A shared model would force a creator that doesn't exist for a one-time special case.
- Tokens are generated like the Bootstrap Invite's (32 random bytes) and stored in plain text, because the link has to stay available. An Invite is short-lived and single-use, and anyone who can read the database has everything else already.
- The link is built from `foodnomads.base-url`.
- Redeeming must be atomic, so that two people using the same link at once can't both join.
- Expiry needs an injected `Clock`. There is none in the code yet.

## API

| Call | Result |
| --- | --- |
| `POST /api/invites` | `201 {id, url, createdBy, expiresAt}` |
| `GET /api/invites` | Open Invites, newest first, same shape |
| `DELETE /api/invites/{id}` | `204` |
| `GET /api/join/{token}/options`, `POST /api/join/{token}` | Existing endpoints, with the errors below |

| Error | Meaning |
| --- | --- |
| `403 {"error":"invite-rejected"}` | Unknown, redeemed, revoked or expired |
| `409 {"error":"display-name-taken"}` | The name belongs to another Member, ignoring case |
| `400 {"error":"display-name-required"}` | Blank name. The options endpoint currently replaces this with "Member" |
| `409 {"error":"already-member"}` | The caller is logged in |

## Acceptance tests

Sketches live in the tickets. The final test code is drafted red and reviewed per ticket (ADR-0004).

- New tests create their own aliased Members. Existing tests stay on `founder`: they're already isolated through Venue aliases, and they're a reviewed contract.
- Expiry has no acceptance test, because that would need a time-travel backdoor in the production image. It's covered by unit tests with a `Clock`, including the boundary at exactly 7 days, and by mutation testing.
- Ignoring case in display names is also covered by unit tests, since `Aliases` gives each spelling its own suffix.
- `Aliases.realName("founder")` would add a suffix, so the DSL must keep passing `founder` through unaliased.

## Out of scope

- Deactivation. When it's built, deactivating a Member should revoke their Open Invites.
- Recovery Links and the activity log
- Notifying the inviter
- German translation. New texts go into the English message bundle.

## Tickets

1. [Create and redeem an Invite](issues/01-create-and-redeem-invite.md)
2. [Unique display names](issues/02-unique-display-names.md)
3. [Already a Member](issues/03-already-a-member.md)
4. [List, re-share and revoke Open Invites](issues/04-open-invites.md)
