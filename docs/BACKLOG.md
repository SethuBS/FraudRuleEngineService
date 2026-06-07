# Backlog Snapshot

This backlog snapshot captures the Sprint 0 cards supplied on 6 June 2026. Planned work starts in `Backlog`; cards move right only as work progresses.

## Sprint 0 Cards

| Card | Title | Priority | Labels | Due | Estimate | Local status |
| --- | --- | --- | --- | --- | ---: | --- |
| `DOD.01` | Define production-grade Definition of Done | `P0 Critical` | `Sprint 0`, `Process`, `Architecture` | 6 June 2026 | 1h | Implemented in docs |
| `DOD.02` | Configure project board labels and workflow | `P0 Critical` | `Sprint 0`, `Process` | 6 June 2026 | 1h | Documented for Trello setup |
| `S0.01` | Finalise project scope and non-goals | `P0 Critical` | `Sprint 0`, `Architecture`, `Documentation` | 6 June 2026 | 1.5h | Implemented in docs |
| `S0.02` | Prepare GitHub repository and project hygiene | `P0 Critical` | `Sprint 0`, `DevOps`, `Documentation` | 6 June 2026 | 1.5h | Local repo scaffolded and initial commit created |
| `S0.03` | Verify Gradle and Spring Boot baseline | `P0 Critical` | `Sprint 0`, `Backend`, `DevOps` | 6 June 2026 | 2h | Verified with `clean test`, `bootJar`, and local health startup |
| `S0.04` | Create package structure and architecture guardrails | `P0 Critical` | `Sprint 0`, `Architecture`, `Backend` | 6 June 2026 | 2h | Package guardrails added |
| `S0.05` | Create reviewer-first README skeleton | `P1 High` | `Sprint 0`, `Documentation` | 6 June 2026 | 1.5h | README skeleton added |

## Sprint 0 Acceptance Notes

- The Definition of Done exists in [DEFINITION_OF_DONE.md](DEFINITION_OF_DONE.md).
- Board labels and workflow are documented in [PROJECT_BOARD.md](PROJECT_BOARD.md).
- Scope and non-goals are documented in [PROJECT_SCOPE.md](PROJECT_SCOPE.md).
- Package responsibilities are documented in [architecture.md](architecture.md).
- Repository hygiene is represented by `.gitignore`, `.dockerignore`, `docs/`, `examples/`, `scripts/`, and `config/dependency-check/`.
- Gradle, Spring Boot, Java, Dependency-Check, Testcontainers, PostgreSQL, Flyway, and OpenAPI dependency choices are represented in `build.gradle`.

## Sprint 1 Cards

| Card | Title | Priority | Labels | Due | Estimate | Local status |
| --- | --- | --- | --- | --- | ---: | --- |
| `S1.01` | Model transaction, fraud decision, alert, and rule evaluation domain | `P0 Critical` | `Sprint 1`, `Backend`, `Architecture` | 7 June 2026 | 3h | Implemented on `feature/s1-01-domain-model` |
| `S1.02` | Define transaction evaluation API contract and validation rules | `P0 Critical` | `Sprint 1`, `API`, `Backend`, `Security` | 7 June 2026 | 3h | Implemented on `feature/s1-02-evaluation-api-contract` |
| `S1.03` | Create PostgreSQL schema with Flyway migrations | `P0 Critical` | `Sprint 1`, `Database`, `Reliability` | 8 June 2026 | 4h | Implemented on `feature/s1-03-postgresql-schema` |
| `S1.04` | Implement JPA entities, repositories, and mapping layer | `P0 Critical` | `Sprint 1`, `Backend`, `Database` | 8 June 2026 | 5h | Implemented on `feature/s1-04-jpa-persistence` |
| `S1.05` | Implement idempotency and duplicate race protection | `P0 Critical` | `Sprint 1`, `Backend`, `Database`, `Reliability` | 8 June 2026 | 5h | Implemented on `feature/s1-05-idempotency` |
| `S1.06` | Implement RuleDefinitionSeeder for code/database sync | `P0 Critical` | `Sprint 1`, `Backend`, `Database`, `Reliability` | 9 June 2026 | 4h | Implemented on `feature/s1-06-rule-definition-seeder` |
| `S1.07` | Implement sanitized raw payload storage and retention metadata | `P1 High` | `Sprint 1`, `Security`, `Database`, `Reliability` | 9 June 2026 | 4h | Implemented on `feature/s1-07-sanitized-raw-payload` |
| `S1.08` | Implement transactional evaluation use case shell | `P0 Critical` | `Sprint 1`, `Backend`, `Architecture` | 9 June 2026 | 4h | Implemented on `feature/s1-08-evaluate-transaction-use-case` |

## Sprint 2 Cards

| Card | Title | Priority | Labels | Due | Estimate | Local status |
| --- | --- | --- | --- | --- | ---: | --- |
| `S2.01` | Implement code-first fraud rule contract and rule engine | `P0 Critical` | `Sprint 2`, `Backend`, `Architecture` | 10 June 2026 | 4h | Implemented on `feature/s2-01-rule-engine` |
| `S2.02` | Implement High Value Transaction rule | `P0 Critical` | `Sprint 2`, `Backend`, `Testing` | 10 June 2026 | 2h | Implemented on `feature/s2-02-high-value-transaction-rule` |
| `S2.03` | Implement Velocity Transaction rule | `P0 Critical` | `Sprint 2`, `Backend`, `Database`, `Testing` | 10 June 2026 | 5h | Implemented on `feature/s2-03-velocity-transaction-rule` |
| `S2.04` | Implement Foreign Country Transaction rule | `P1 High` | `Sprint 2`, `Backend`, `Testing` | 11 June 2026 | 3h | Implemented on `feature/s2-04-foreign-country-transaction-rule` |
| `S2.05` | Implement Risky Merchant Category rule | `P1 High` | `Sprint 2`, `Backend`, `Testing` | 11 June 2026 | 3h | Implemented on `feature/s2-05-risky-merchant-category-rule` |
| `S2.06` | Implement Suspicious Merchant rule | `P1 High` | `Sprint 2`, `Backend`, `Testing` | 11 June 2026 | 3h | Implemented on `feature/s2-06-suspicious-merchant-rule` |
| `S2.07` | Implement Unusual Amount rule | `P1 High` | `Sprint 2`, `Backend`, `Database`, `Testing` | 11 June 2026 | 5h | Implemented on `feature/s2-07-unusual-amount-rule` |
| `S2.08` | Implement risk scoring and final decision policy | `P0 Critical` | `Sprint 2`, `Backend`, `Architecture`, `Testing` | 12 June 2026 | 4h | Implemented on `feature/s2-08-risk-scoring-decision-policy` |
| `S2.09` | Implement POST /api/v1/transactions/evaluate | `P0 Critical` | `Sprint 2`, `API`, `Backend`, `Security` | 12 June 2026 | 4h | Implemented on `feature/s2-09-post-transaction-evaluate` |
| `S2.10` | Implement fraud alert and evaluation retrieval APIs | `P0 Critical` | `Sprint 2`, `API`, `Backend`, `Database` | 12 June 2026 | 6h | Implemented on `feature/s2-10-alert-evaluation-retrieval-apis` |
| `S2.11` | Add OpenAPI documentation and API examples | `P1 High` | `Sprint 2`, `API`, `Documentation` | 12 June 2026 | 3h | Implemented on `feature/s2-11-openapi-docs-examples` |

## Sprint 3 Cards

| Card | Title | Priority | Labels | Due | Estimate | Local status |
| --- | --- | --- | --- | --- | ---: | --- |
| `S3.01` | Configure JWT resource server and scope authorization | `P0 Critical` | `Sprint 3`, `Security`, `Backend` | 13 June 2026 | 4h | Implemented on `feature/s3-01-jwt-resource-server-security` |
| `S3.02` | Enforce scope-based authorization and method security | `P0 Critical` | `Sprint 3`, `Security`, `API` | 13 June 2026 | 4h | Implemented on `feature/s3-02-scope-method-security` |
| `S3.03` | Add dev-only JWT generation workflow for reviewers | `P1 High` | `Sprint 3`, `Security`, `Documentation`, `DevOps` | 13 June 2026 | 3h | Implemented on `feature/s3-03-dev-jwt-generation` |
| `S3.04` | Implement global error handling and safe API errors | `P0 Critical` | `Sprint 3`, `Backend`, `API`, `Security` | 14 June 2026 | 4h | Implemented on `feature/s3-04-safe-global-errors` |
| `S3.05` | Add structured logging and correlation IDs | `P1 High` | `Sprint 3`, `Observability`, `Reliability`, `Security` | 14 June 2026 | 4h | Implemented on `feature/s3-05-structured-logging-correlation` |
| `S3.06` | Configure Actuator health, readiness, metrics, and Prometheus | `P1 High` | `Sprint 3`, `Observability`, `DevOps` | 14 June 2026 | 3h | Implemented on `feature/s3-06-actuator-observability` |
| `S3.07` | Implement raw payload retention cleanup job | `P2 Medium` | `Sprint 3`, `Security`, `Database`, `Reliability` | 14 June 2026 | 3h | Implemented on `feature/s3-07-raw-payload-cleanup` |
| `S3.08` | Finalise Dockerfile and Docker Compose runtime | `P0 Critical` | `Sprint 3`, `DevOps`, `Reliability` | 15 June 2026 | 5h | Implemented on `feature/s3-08-docker-runtime` |
| `S3.09` | Add GitHub Actions CI pipeline | `P1 High` | `Sprint 3`, `DevOps`, `Testing` | 15 June 2026 | 4h | Implemented on `feature/s3-09-github-actions-ci` |
| `S3.10` | Run OWASP Dependency Check and secret hygiene review | `P1 High` | `Sprint 3`, `Security`, `DevOps` | 15 June 2026 | 3h | Implemented on `feature/s3-10-dependency-secret-review` |

## Sprint 4 Cards

| Card | Title | Priority | Labels | Due | Estimate | Local status |
| --- | --- | --- | --- | --- | ---: | --- |
| `S4.01` | Complete unit tests for all fraud rules and scoring | `P0 Critical` | `Sprint 4`, `Testing`, `Backend` | 16 June 2026 | 5h | Implemented on `feature/s4-01-rule-scoring-unit-tests` |
| `S4.02` | Complete integration tests with PostgreSQL Testcontainers | `P0 Critical` | `Sprint 4`, `Testing`, `Database` | 16 June 2026 | 6h | Implemented on `feature/s4-02-postgresql-testcontainers-integration` |
| `S4.03` | Complete API and security integration tests | `P0 Critical` | `Sprint 4`, `Testing`, `API`, `Security` | 16 June 2026 | 6h | Implemented on `feature/s4-03-api-security-integration-tests` |
