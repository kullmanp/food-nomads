# Passkey-only login, no email

Members log in with passkeys only (Face ID / Touch ID via WebAuthn). There are no passwords and no email addresses. A Member who loses access gets a single-use Recovery Link from any other Member, delivered out of band (e.g. the group chat). We chose this so we don't have to run or pay for outgoing mail in a group of six friends who all share a chat anyway. Passkeys sync through iCloud Keychain or Google Password Manager, so lost access should be rare.

## Consequences

- The app cannot contact Members by itself. Notifications, if ever needed, need a new channel.
- Any Member can take over another Member's access by issuing a Recovery Link. We accept this because the group trusts each other, and every Recovery Link shows up in the activity log.
