# Server-rendered Qute + htmx, no SPA

The UI is rendered on the server with Quarkus Qute templates, with htmx for partial updates and a small hand-written service worker and manifest for the PWA. There is no JavaScript build. Everyone in the group knows Java, the app is small, and a single toolchain keeps the code easy for humans and agents to follow.

Alongside the UI there is a JSON API under `/api` (see ADR-0004). The Qute pages and the API are both thin adapters over one shared application layer. Neither one holds business rules.
