## How we work

Every story is built acceptance-test-first, in Dave Farley's style (ADR-0004). One ticket per session, on a feature branch named after the ticket:

1. **Draft red.** Write the ticket's acceptance tests in the DSL in `acceptance-tests`. Add any new DSL and driver methods as stubs only. Stop here and hand over for review. Done when the tests compile and fail for the stated reason: the behaviour is missing, not the harness broken.
2. **Human review.** The human commits the reviewed red tests. From then on they are the contract: changing them needs the human's approval.
3. **Go green.** Implement the DSL, both drivers, and `app` until the suite passes with `-Ddriver=api` and with `-Ddriver=ui`. Use unit-level TDD inside `app` where it helps. Business rules belong in the shared application layer, and the Qute pages and `/api` stay thin (ADR-0003).
4. **Close.** Set the ticket's `Status:` to `resolved`, and add a short note under `## Comments` covering anything a later ticket needs to know. The human merges to `main`.

`main` is always green. Work that isn't a story (docs, tooling) is committed directly to `main`.

Code, comments and docs are in English. User-facing text goes through message bundles.

## Agent skills

### Issue tracker

Issues are tracked as local markdown files under `.scratch/<feature>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

Uses the five default triage labels: `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `GLOSSARY.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
