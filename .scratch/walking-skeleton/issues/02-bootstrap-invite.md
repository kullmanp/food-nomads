# 02 Bootstrap Invite and passkey registration

Status: resolved
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

## Comments

Notes for later tickets:

- **Identity:** the WebAuthn username and security principal name is the Member id (UUID). The display name is only shown, and it's what the authenticator lists as `user.name`. `Membership.current(identity)` turns a `SecurityIdentity` into the `Member`. Quarkus puts a random user handle into each registration and never uses it for lookup. Login goes by credential id via `MemberPasskeys.findByCredentialId`.
- **Registration:** `quarkus-security-webauthn`'s own `/q/webauthn/register` is off (the default). Joining goes through `GET /api/join/{token}/options` and `POST /api/join/{token}` (`{displayName, credential}`), which check the Invite before the ceremony and again when they consume it. A rejected Invite is `403 {"error":"invite-rejected"}`. The options path fits `registerClientSteps` in Quarkus's `webauthn.js`, so the join page (`/join/{token}`) runs the ceremony against these API endpoints, not separate web endpoints. Invites (next feature) can reuse the same flow with a different token source behind `Membership.checkInvite`.
- **Bootstrap Invite:** the token lives only in memory and is logged as `Bootstrap Invite: <foodnomads.base-url>/join/<token>`. Restarting with no Members logs a new one.
- **Session:** `rememberUser` sets the `quarkus-credential` cookie. `quarkus.http.auth.session.encryption-key` isn't set yet, so each start uses a random key, which logs everyone out on restart and is printed at WARN level. Set it from the environment before deploying.
- **Unauthenticated API:** `GET /api/me` returns 401 itself. An `@Authenticated` endpoint would instead get the WebAuthn mechanism's 302 to `/login.html`. Ticket 03 should decide this for `/api/*` as a whole (e.g. 401 for `/api`, redirect for pages).
- **Harness:** `Founder.ensureJoined()` runs at the start of every test's setup. It joins once per JVM on the active driver as display name `founder` and checks they're logged in. If that fails, every test fails with the cause. `founder`'s display name is exactly `founder`, not aliased: `Aliases.aliasOf("founder")` returns `founder`, but `Aliases.realName("founder")` would add a suffix, so the DSL for 03 must not alias Member names that refer to `founder`.
- **Passkeys:** `Passkeys` (driver side, keyed by display name) holds a `Passkey`: credential id, PKCS#8 P-256 private key, rpId, user handle, sign count. The UI driver reads it from the virtual authenticator with CDP `WebAuthn.getCredentials`. The API driver's `SoftwareAuthenticator` creates it. Importing isn't written yet. For ticket 03: UI → `WebAuthn.addCredential` with `isResidentCredential: true` and standard-base64 fields; API → add an assertion method to `SoftwareAuthenticator` (ES256 over `authData || sha256(clientDataJSON)`, DER signature, flags UP|UV). The rpId is `localhost` because the app derives origin and rpId from the request's Host.
- **API driver:** `ApiClient` wraps the `HttpClient` with a `CookieManager` and holds the JSON helpers. Both API drivers use it.
- **CI:** green on the feature branch (run 40096954460): build, `acceptance (api)` and `acceptance (ui)`.
