# Submission Snapshot

Card: `R.04`
Due: 18 June 2026
Priority: `P0 Critical`
Labels: `Release`, `DevOps`
Estimate: 1 hour

This document records the stable GitHub submission point for the Fraud Rule Engine Service.

## Repository

| Field | Value |
|-------|-------|
| Repository | `SethuBS/FraudRuleEngineService` |
| GitHub URL | `https://github.com/SethuBS/FraudRuleEngineService` |
| Submission branch | `development` |
| Submission tag | `v0.1.0-submission` |
| Repository visibility | Public |

The GitHub repository URL was checked from the web UI and opened without an authenticated repository session. The page shows the repository as public and renders the README with build, run, Docker, test, API, security, and documentation instructions.

## Release Readiness

The release tag is intended to point at the final submission commit on `development`. Reviewers can use either:

- the `development` branch, or
- the `v0.1.0-submission` tag.

R.01, R.02, and R.03 are complete and merged into `development`:

- Final build, tests, and dependency check: [RELEASE_VERIFICATION.md](RELEASE_VERIFICATION.md)
- Final documentation review: [FINAL_DOCUMENTATION_REVIEW.md](FINAL_DOCUMENTATION_REVIEW.md)
- Final security review: [FINAL_SECURITY_REVIEW.md](FINAL_SECURITY_REVIEW.md)

## Final Notes

- Docker Compose is the recommended reviewer runtime.
- The README contains the build, run, Docker, test, JWT, API, and curl collection instructions.
- The GitHub Wiki mirrors the deeper architecture, security, testing, release, and governance notes.
- Local reviewer JWT generation is documented as local-only and not production authentication.
- Production profile disables Swagger/OpenAPI exposure.
