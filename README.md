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
.\gradlew.bat clean check
.\gradlew.bat bootJar
```

On Bash or PowerShell Core:

```bash
./gradlew clean check
./gradlew bootJar
```

## Code Style

Java formatting is enforced in the repository through `.editorconfig`, Checkstyle, and the CI `clean check` job. Use four spaces, no tabs, grouped imports, and next-line opening braces for classes, records, constructors, and methods:

```java
public class RuleController
{

    public List<RuleDefinitionResponse> rules()
    {
        return ruleCatalogService.activeRules();
    }
}
```

## Configuration

Fraud evaluation thresholds are runtime configuration, not domain constants. The defaults live in `application.yml` and can be overridden by environment variables:

| Setting | Environment variable | Default |
| --- | --- | --- |
| Transaction evaluation API path | `FRAUD_API_PATH_TRANSACTION_EVALUATIONS` | `/api/v1/transaction-evaluations` |
| Reviewer transaction evaluation API path | `FRAUD_API_PATH_TRANSACTIONS_EVALUATE` | `/api/v1/transactions/evaluate` |
| Fraud alerts API path | `FRAUD_API_PATH_FRAUD_ALERTS` | `/api/v1/fraud-alerts` |
| Fraud alert detail API path | `FRAUD_API_PATH_FRAUD_ALERT_DETAIL` | `/api/v1/fraud-alerts/{alertId}` |
| Transaction fraud evaluation lookup API path | `FRAUD_API_PATH_TRANSACTION_FRAUD_EVALUATION` | `/api/v1/transactions/{transactionId}/fraud-evaluation` |
| Alert list default page | `FRAUD_API_PAGINATION_DEFAULT_PAGE` | `0` |
| Alert list default size | `FRAUD_API_PAGINATION_DEFAULT_SIZE` | `20` |
| Alert list maximum size | `FRAUD_API_PAGINATION_MAX_SIZE` | `100` |
| Merchant category maximum length | `FRAUD_API_VALIDATION_MERCHANT_CATEGORY_MAX_LENGTH` | `80` |
| Medium risk score threshold | `FRAUD_EVALUATION_RISK_THRESHOLD_MEDIUM` | `25` |
| High risk score threshold | `FRAUD_EVALUATION_RISK_THRESHOLD_HIGH` | `50` |
| Critical risk score threshold | `FRAUD_EVALUATION_RISK_THRESHOLD_CRITICAL` | `75` |
| Review decision minimum risk level | `FRAUD_EVALUATION_DECISION_THRESHOLD_REVIEW` | `MEDIUM` |
| Flagged decision minimum risk level | `FRAUD_EVALUATION_DECISION_THRESHOLD_FLAGGED` | `HIGH` |
| Application clock zone | `FRAUD_TIME_ZONE_ID` | `UTC` |
| Test PostgreSQL image | `TEST_POSTGRES_IMAGE` or `-Dtest.postgres.image` | `postgres:16-alpine` |

## Docker

Docker runtime is part of the Definition of Done. The baseline Docker assets are present, and the full smoke-test path will be completed as the API and persistence cards land.

```bash
docker compose up --build
```

## Database

Flyway migrations live in `src/main/resources/db/migration`. `V1__create_schema.sql` creates the core PostgreSQL schema for idempotent event processing, transaction storage, fraud alert retrieval, rule catalog metadata, and auditable rule evaluation rows.

Raw payload storage is intentionally sanitized and retention-ready: tables that may hold payload snapshots include `sanitized_raw_payload` and `raw_payload_expires_at`, while the retention duration itself is not fixed in the schema.

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

Transaction evaluation accepts a stable categorized transaction contract. Fraud rules can change internally without changing the request shape or the generic matched-rule response list.

```http
POST /api/v1/transactions/evaluate
Content-Type: application/json
```

```json
{
  "eventId": "event-1",
  "transactionId": "tx-1",
  "customerId": "customer-1",
  "accountId": "account-1",
  "amount": 100.50,
  "currency": "ZAR",
  "transactionTimestamp": "2026-06-07T08:00:00Z",
  "merchantCategory": "GROCERY",
  "country": "ZA",
  "channel": "MOBILE",
  "merchantId": "merchant-1",
  "merchantName": "Corner Shop",
  "deviceId": "device-1"
}
```

The same payload is available at [examples/transaction-evaluation-request.json](examples/transaction-evaluation-request.json).

```bash
curl -X POST "http://localhost:8080/api/v1/transactions/evaluate" \
  -H "Content-Type: application/json" \
  -d @examples/transaction-evaluation-request.json
```

The response includes `transactionId`, `decision`, `riskScore`, `riskLevel`, `matchedRules`, and `evaluatedAt`. Duplicate event or transaction submissions are handled deterministically by the idempotency layer and return the stored evaluation when available.

Stored fraud alerts can be retrieved with optional filters and pagination:

```bash
curl "http://localhost:8080/api/v1/fraud-alerts?customerId=customer-1&riskLevel=HIGH&page=0&size=20"
```

Retrieve one alert by alert id:

```bash
curl "http://localhost:8080/api/v1/fraud-alerts/b3ed20ca-bac6-45dc-a37e-d61001ee37ab"
```

Retrieve the stored evaluation for a transaction id:

```bash
curl "http://localhost:8080/api/v1/transactions/tx-1/fraud-evaluation"
```

Missing alerts or transaction evaluations return `404 RESOURCE_NOT_FOUND`. Alert list pagination defaults and limits are configurable through `fraud.api.pagination.*`.

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
