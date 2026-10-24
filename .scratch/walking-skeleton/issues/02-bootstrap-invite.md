# 02 Bootstrap Invite and passkey registration

Status: claimed
Blocked by: 01
Spec: [../spec.md](../spec.md)

## What

The first Member joins through the Bootstrap Invite and registers a passkey (see `GLOSSARY.md`: Bootstrap Invite, Member).

- App: if there are no Members at startup, generate a single-use token and log the join URL. Registering a passkey with that token creates the first Member, with a display name, and logs them in. Once a Member exists, the token is rejected. Works through the UI (join page) and through `/api` + the WebAuthn endpoints.
- Harness: once per run, read the join URL from the app container's log and redeem it through the active driver, creating the Member alias `founder`. Fail the run loudly if `founder` isn't logged in afterwards.
- Drivers: a credential store keyed by Member alias, so later tests can import `founder`'s passkey into a fresh browser context (Chromium virtual authenticator, CDP `WebAuthn.addCredential`) or a fresh HTTP client (software authenticator, e.g. webauthn4j test authenticator).

## Acceptance test

```java
class BootstrapInviteAcceptanceTest extends AcceptanceTest {
    @Test
    void bootstrapInviteCannotBeUsedOnceAMemberExists() {
        members.assertBootstrapInviteRejected();
    }
}
```

The happy path is covered by the harness's `founder` setup, which runs on every suite run.

## Risks: spike these first

- Restricting `quarkus-security-webauthn` registration to requests with a valid Invite token.
- Exporting a credential from one authenticator and importing it into another, for both drivers.

## Done when

- Both drivers are green, and the `founder` setup succeeds on each.
- The new tables come from a Flyway migration.
