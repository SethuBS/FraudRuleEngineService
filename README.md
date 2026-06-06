# FraudRuleEngineService

[![CI](https://github.com/SethuBS/FraudRuleEngineService/actions/workflows/ci.yml/badge.svg?branch=development)](https://github.com/SethuBS/FraudRuleEngineService/actions/workflows/ci.yml)

FraudRuleEngineService is a Java 17 / Spring Boot service for evaluating categorized transaction events against explainable fraud rules. The project is intentionally scoped as a modular monolith so the 18 June 2026 submission remains focused, reviewable, and runnable.

## Status

Sprint 0 baseline is being established. Core fraud rules, persistence models, APIs, security hardening, and full verification will be completed in later sprint cards.

## Problem Statement

The service must receive a categorized transaction event, evaluate it against deterministic fraud rules, persist the decision and rule evaluation evidence, and expose retrieval APIs for alerts, event evaluations, and the active rule catalog.

## Scope

Included:

- REST ingestion for categorized transaction events.
- Code-first fraud rules with explainable outcomes.
- PostgreSQL persistence for transactions, evaluations, alerts, and rule metadata.
- JWT OAuth2 Resource Server security.
- Docker-based local runtime.
- Tests, OpenAPI, observability, and reviewer documentation.

Out of scope for the target submission:

- Full analyst workflow.
- Full Kafka ingestion implementation.
- Multi-tenant RBAC.
- Notifications.
- Machine learning models.

See [docs/PROJECT_SCOPE.md](docs/PROJECT_SCOPE.md) for the locked Sprint 0 scope and non-goals.

## Architecture Summary

The service is a modular monolith with clear package boundaries:

```text
com.capitec.fraud
  api                         REST controllers and DTOs
  application                 use cases and orchestration
  domain                      domain objects, enums, and business concepts
  rules                       code-first fraud rules
  infrastructure.persistence  JPA entities and repositories
  infrastructure.security     JWT and authorization configuration
  infrastructure.observability logging, metrics, and filters
  infrastructure.config       application configuration
```

See [docs/architecture.md](docs/architecture.md) for package responsibilities and guardrails.

## Technology Stack

- Java 17
- Spring Boot 3.5.14
- Gradle
- PostgreSQL
- Flyway
- Spring Web, Validation, Data JPA, Actuator, Security, OAuth2 Resource Server
- springdoc OpenAPI
- JUnit 5, AssertJ, Testcontainers
- OWASP Dependency-Check
- Docker and Docker Compose

## Quick Start

This section will be completed as the implementation moves through Sprint 1 and Sprint 2.

Current baseline checks:

```powershell
.\gradlew.bat clean test
.\gradlew.bat bootJar
```

On Bash or PowerShell Core:

```bash
./gradlew clean test
./gradlew bootJar
```

## Docker

Docker runtime is part of the Definition of Done. The baseline Docker assets are present, and the full smoke-test path will be completed as the API and persistence cards land.

```bash
docker compose up --build
```

## Testing

The target verification baseline includes:

- Rule unit tests.
- Application service tests.
- PostgreSQL integration tests with Testcontainers.
- API integration tests.
- Security tests.
- OpenAPI contract tests.
- Flyway migration smoke tests.
- Docker Compose smoke tests.

## Security Model

The target service is an OAuth2 Resource Server. It validates JWT bearer tokens and scope-based authorization. Token issuing, identity-provider setup, multi-tenant RBAC, and full analyst workflow security are outside the target submission scope.

## API Examples

Reviewer-facing API examples will be added when the Sprint 2 API cards are implemented.

## Design Trade-Offs

- A modular monolith keeps the write path simple and strongly consistent while still demonstrating clean boundaries.
- Code-first rules keep fraud logic deterministic, testable, and easy to explain.
- PostgreSQL and Flyway provide durable audit records with explicit schema evolution.
- REST is the v1 ingestion adapter; Kafka can be added later around the same application service.

## Known Limitations

- No fraud business rules are implemented in Sprint 0 baseline.
- No analyst alert workflow is implemented yet.
- No Kafka adapter is implemented yet.
- No production identity-provider integration is included yet.

## Project Governance

- Definition of Done: [docs/DEFINITION_OF_DONE.md](docs/DEFINITION_OF_DONE.md)
- Project board workflow and labels: [docs/PROJECT_BOARD.md](docs/PROJECT_BOARD.md)
- Sprint backlog snapshot: [docs/BACKLOG.md](docs/BACKLOG.md)
