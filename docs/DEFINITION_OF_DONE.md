# Definition Of Done

Card: `DOD.01`
Due: 6 June 2026
Priority: `P0 Critical`
Labels: `Sprint 0`, `Process`, `Architecture`
Estimate: 1 hour

This Definition of Done applies to every project card. A card is not done because the happy path works locally; it is done when the implementation is correct, secure, observable, tested, documented, and runnable in the Docker-based shape expected by reviewers.

## Production-Grade For This Project

Production-grade means the Fraud Rule Engine Service can be reviewed as a maintainable, secure, and operationally understandable Spring Boot service, even where future production capabilities are intentionally kept out of scope for the submission.

Every completed feature must preserve these qualities:

| Quality         | Required evidence                                                                                                                                                 |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Correctness     | Behavior matches the design document, API contract, and card acceptance criteria, including validation and failure paths.                                         |
| Determinism     | Fraud decisions, rule ordering, idempotent duplicate handling, and persisted audit records are predictable and repeatable.                                        |
| Security        | Protected endpoints enforce the intended access rules, sensitive data is not logged, raw payload storage is sanitized, and production-only exposure is minimized. |
| Privacy         | Customer, account, device, and raw payload handling follows data-minimization and retention rules.                                                                |
| Testing         | Unit, integration, API, security, migration, OpenAPI, or smoke tests cover the risk introduced by the card.                                                       |
| Observability   | Relevant logs, correlation ids, metrics, health checks, and operational failure signals remain useful and do not leak sensitive data.                             |
| Documentation   | README, developer guide, design document, Postman collection, or ADRs are updated when behavior, setup, APIs, security, or trade-offs change.                     |
| Docker runtime  | The service still builds, starts, passes health checks, and works through Docker Compose with PostgreSQL.                                                         |
| Maintainability | Code follows clear package boundaries, with business logic outside controllers and persistence concerns outside domain rules.                                     |

## Build Done

A card can move to Build Done only when all of these are true:

- The implementation satisfies the card acceptance criteria and does not leave hidden TODO behavior on the main path.
- Code follows the project architecture: API entry points, application orchestration, domain fraud logic, infrastructure persistence, and configuration stay separated.
- API inputs, outputs, validation, status codes, and error bodies are intentionally handled.
- Database changes are represented by migrations, with indexes and constraints added where correctness or query behavior requires them.
- Security behavior is explicit for new or changed endpoints.
- Sensitive values are not logged or returned accidentally.
- Metrics, correlation-id behavior, health impact, and operational failure modes are considered.
- Tests are added or updated at the smallest useful level for the risk of the change.
- Documentation or reviewer artifacts are updated when the change affects setup, APIs, behavior, security, operations, or known gaps.
- The local build and relevant focused tests pass.

## Cert Test

A card can move to Cert Test only when Build Done is satisfied and all of these are true:

- The full automated test suite passes with Docker available for database-backed tests.
- API changes are covered by integration tests and, where relevant, OpenAPI contract checks.
- Security changes are covered by tests for allowed access, missing credentials, invalid credentials, and insufficient permissions.
- Database changes are covered by migration smoke tests or repository integration tests.
- Privacy-sensitive changes prove masking, retention, or non-logging behavior as appropriate.
- Documentation has been reviewed against the actual command/API behavior.
- Docker Compose has been considered for any configuration, environment variable, port, health check, or startup dependency change.

## Ready For Release

A card can move to Ready for Release only when Cert Test is satisfied and all of these are true:

- The full reviewer path in `README.md` is accurate.
- Clean build, application packaging, Docker image build, and Docker Compose startup are expected to pass in CI or local release verification.
- The end-to-end smoke script covers the changed user-facing behavior, or the card documents why smoke coverage is not applicable.
- OpenAPI, Postman, and README examples remain aligned for reviewer-facing endpoints.
- Security scan status is known, and any new dependency risk is documented or avoided.
- Known gaps are explicit; incomplete future work is listed as future work rather than implied as delivered.
- No `P3 Stretch` work is started or released while any `P0 Critical` or `P1 High` project card remains incomplete.

## Stretch Work Rule

Stretch work is allowed only after all `P0 Critical` and `P1 High` cards are Build Done, with no unresolved release-blocking defects. Stretch work must not destabilize the release path, expand the API contract without tests, weaken security, or consume the emergency buffer unless explicitly promoted into core scope.

## Card Review Checklist

Use this short checklist before moving any card forward:

- Acceptance criteria are satisfied.
- Build passes at the level appropriate to the card.
- Tests cover the changed behavior and important failure paths.
- Security and privacy impact has been checked.
- Observability impact has been checked.
- Docker/runtime impact has been checked.
- Documentation/reviewer artifacts are current.
- No higher-priority incomplete work is being bypassed by stretch work.
