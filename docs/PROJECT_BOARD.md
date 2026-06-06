# Project Board Workflow

Card: `DOD.02`
Due: 6 June 2026
Priority: `P0 Critical`
Labels: `Sprint 0`, `Process`
Estimate: 1 hour

This project uses labels for priority, sprint, and work type. Trello columns remain workflow states; do not create separate sprint columns.

## Workflow Rules

- All planned work starts in `Backlog`.
- Cards move right only as work progresses.
- Sprint labels show when work is planned.
- Priority labels show release importance.
- Work-type labels show the kind of effort.
- A card can carry one priority label, one sprint label, and one or more work-type labels.
- `P3 Stretch` cards must not start while any `P0 Critical` or `P1 High` card is incomplete.

## Priority Labels

| Label | Meaning |
| --- | --- |
| `P0 Critical` | Required for a credible submission or release gate. |
| `P1 High` | Important for reviewer confidence and target release quality. |
| `P2 Medium` | Valuable, but can move if the core path is at risk. |
| `P3 Stretch` | Optional improvement after core work is stable. |

## Sprint Labels

| Label | Window |
| --- | --- |
| `Sprint 0` | 6 Jun, planning and baseline |
| `Sprint 1` | 7-9 Jun, core domain and persistence |
| `Sprint 2` | 10-12 Jun, rule engine and APIs |
| `Sprint 3` | 13-15 Jun, security and production hardening |
| `Sprint 4` | 16-17 Jun, testing, documentation, polish |
| `Release` | 18 Jun, release and submit |
| `Stretch` | Optional work after required scope is complete |

## Work-Type Labels

- `Architecture`
- `Backend`
- `Database`
- `API`
- `Security`
- `Testing`
- `DevOps`
- `Documentation`
- `Observability`
- `Reliability`
- `Interview`
- `Process`

## Current Trello Action

No Trello connector is available in this Codex session, so this document is the source checklist for manual board setup. The planned Sprint 0 cards are captured in [BACKLOG.md](BACKLOG.md).
