# Final Documentation Review

Card: `R.02`
Due: 18 June 2026
Priority: `P0 Critical`
Labels: `Release`, `Documentation`
Estimate: 2 hours

This document records the final reviewer-facing documentation pass before submission.

## Review Scope

The review was performed from the perspective of a reviewer opening the GitHub repository for the first time.

| Area                    | Result                                                                                                                         |
|-------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| GitHub README web view  | Reviewed on the `development` branch                                                                                           |
| Quick Start             | Clear Docker-first path with separate PowerShell and Bash examples                                                             |
| Architecture docs       | Linked from README and available in `docs/architecture.md` and `docs/INTERVIEW_NOTES.md`                                       |
| API examples            | README includes Swagger/OpenAPI links, sample JSON, simple curl examples, and the executable curl suite                        |
| Docker instructions     | README documents `docker compose up --build`, health checks, ports, environment variables, and shutdown                        |
| Test instructions       | README documents Gradle test commands and Dependency-Check usage                                                               |
| Security model          | README and `docs/SECURITY_REVIEW.md` explain JWT scopes, local-only token generation, actuator access, and safe errors         |
| Known limitations       | README honestly lists out-of-scope analyst workflow, Kafka ingestion, tenant model, production IdP setup, and Docker hardening |
| GitHub Wiki             | Wiki is published and linked from the README                                                                                   |
| Spelling and formatting | Markdown formatting and obvious stale references reviewed                                                                      |

## Documentation Changes Made

- Added a `Reviewer Navigation` section near the top of the README.
- Split Quick Start token examples into `PowerShell` and `Bash or Git Bash`.
- Added direct README links to release verification, final documentation review, and the GitHub Wiki.
- Added `R.02` to the release backlog snapshot.
- Updated release verification notes so the curl suite is described as the full expanded suite rather than the older 13-check version.
- Updated Dependency-Check release notes to reflect the successful heap-safe command used during final verification.

## Reviewer Five-Minute Path

1. Read the README `Status`, `Reviewer Navigation`, `Project Overview`, and `Quick Start` sections.
2. Run `docker compose up --build`.
3. Generate a local `system-ingestor` token.
4. Submit `examples/high-risk-transaction.json`.
5. Read `docs/architecture.md` or the GitHub Wiki for deeper design context.

## Remaining Notes

- The README intentionally keeps the complete curl collection because it is directly executable and proves the reviewer flow without requiring Postman.
- The GitHub Wiki mirrors and organizes deeper documentation, but the repository docs remain the versioned source of truth.
- No outdated local machine paths or stale alert-risk filter examples were found during the documentation scan.
