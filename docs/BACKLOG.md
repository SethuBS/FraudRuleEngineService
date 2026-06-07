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
