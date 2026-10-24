# Food Nomads

Six former colleagues meet every two weeks for lunch in and around Basel. There is one rule: **never the same place twice**, unless something substantial changed (a new chef, a new owner).

This app organises and documents those lunches. It runs as a PWA on the phone and is closed to the group.

## Features

- **Venues**: propose places, comment on them, vote for the ones you want to try.
- **Lunches**: plan the next Lunch, RSVP, then add comments and photos afterwards.
- **The rule, enforced**: a Venue becomes *visited* once a Lunch is held there and can't be chosen again. A substantial change creates a linked *Successor Venue*.
- **Invite-only**: any Member can invite someone new with a single-use link.
- **No passwords**: login uses passkeys (Face ID / Touch ID).
- **English and German** UI.

Domain terms are defined in [GLOSSARY.md](GLOSSARY.md).

## Tech stack

- Java 25, Quarkus (LTS), Maven
- Qute + htmx server-rendered UI, plus a JSON API under `/api`
- PostgreSQL, photos on a mounted volume
- WebAuthn via `quarkus-security-webauthn`
- Acceptance tests: Java DSL, Playwright and REST protocol drivers

## Running locally

```sh
./mvnw quarkus:dev
```

Dev Services starts PostgreSQL automatically (Docker required). Dev mode loads made-up seed data and offers a "log in as …" picker, so you don't need a passkey. That picker exists only in dev mode.

## Tests

```sh
./mvnw verify                                        # everything, API driver
./mvnw verify -pl acceptance-tests -Ddriver=ui       # acceptance tests through the browser
```

## Deployment

The app runs as `docker compose` (Caddy + app + PostgreSQL) in a Debian VM on a home Proxmox server, reached at `nomads.kup.ch` through dynamic DNS and a port-forward to Caddy, which handles TLS. GitHub Actions builds the image to GHCR, and a deploy script pulls it. On an empty database, the app logs a one-time bootstrap Invite for the first Member. Configuration comes from environment variables; see `.env.example`.

## How this repo is built

This project is developed with AI coding agents, using **acceptance-test-driven development** in the style of Dave Farley:

1. A story is written as a ticket under `.scratch/<feature>/issues/`.
2. An acceptance test is written in the DSL, **reviewed by a human**, and committed failing.
3. Agents implement it until it passes, on both protocol drivers.

The same acceptance suite runs unchanged through the browser (Playwright) and through the REST API. Only the protocol driver changes.

- [AGENTS.md](AGENTS.md): instructions for agents
- [GLOSSARY.md](GLOSSARY.md): the domain language
- [docs/adr/](docs/adr/): architecture decisions
- `.scratch/`: specs and tickets
