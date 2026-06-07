# FraudRuleEngineService

[![CI](https://github.com/SethuBS/FraudRuleEngineService/actions/workflows/ci.yml/badge.svg?branch=development)](https://github.com/SethuBS/FraudRuleEngineService/actions/workflows/ci.yml)

FraudRuleEngineService is a Java 17 / Spring Boot service for evaluating categorized transaction events against explainable fraud rules. It persists the transaction decision, the rule evidence behind that decision, and alert records that can be retrieved through secured APIs. The project is intentionally scoped as a modular monolith so the 18 June 2026 submission remains focused, reviewable, and runnable.

## Status

The reviewer path is implemented: Docker Compose starts the service and PostgreSQL, Flyway creates the schema, local-only JWT scripts generate reviewer tokens, the main APIs are secured by scope, and the Gradle test suite includes unit, integration, API, security, idempotency, Flyway, and Docker-facing coverage.

## Project Overview

The service must receive a categorized transaction event, evaluate it against deterministic fraud rules, persist the decision and rule evaluation evidence, and expose retrieval APIs for alerts, event evaluations, and the active rule catalog.

The primary reviewer workflow is:

1. Start the app and PostgreSQL with Docker Compose.
2. Generate a local reviewer JWT.
3. Submit `examples/high-risk-transaction.json` to `POST /api/v1/transactions/evaluate`.
4. Retrieve the created alert with `GET /api/v1/fraud-alerts`.
5. Retrieve the stored decision with `GET /api/v1/transactions/{transactionId}/fraud-evaluation`.

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

## Prerequisites

- Git.
- Java 17 when running Gradle or the app outside Docker.
- Docker Desktop or another Docker engine with Compose support.
- PowerShell on Windows, or Bash on Linux/macOS.
- `curl` for the sample API calls.
- OpenSSL only when using `scripts/generate-jwt.sh`; Windows reviewers can use `scripts/generate-jwt.ps1`.
- No local Gradle installation is required because the Gradle wrapper is committed.

## Quick Start

The fastest reviewer path is Docker Compose:

```bash
docker compose up --build
```

In another terminal, check readiness:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/readiness
```

Generate a transaction-ingestor token and evaluate the high-risk sample:

```powershell
$token = .\scripts\generate-jwt.ps1 -Profile system-ingestor
curl.exe -X POST "http://localhost:8080/api/v1/transactions/evaluate" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@examples/high-risk-transaction.json"
```

```bash
TOKEN="$(./scripts/generate-jwt.sh --profile system-ingestor)"
curl -X POST "http://localhost:8080/api/v1/transactions/evaluate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d @examples/high-risk-transaction.json
```

Open Swagger UI locally at `http://localhost:8080/swagger-ui/index.html`.

Stop the reviewer runtime when finished:

```bash
docker compose down
```

## Local Run Without Docker Compose

Docker Compose is the recommended reviewer runtime because it wires PostgreSQL and the app together. If you prefer to run the Spring Boot process from the host, start PostgreSQL first:

```bash
docker compose up -d postgres
```

Then run the app with the same local database settings:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fraud_rule_engine"
$env:SPRING_DATASOURCE_USERNAME = "fraud"
$env:SPRING_DATASOURCE_PASSWORD = "fraud"
.\gradlew.bat bootRun
```

```bash
SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/fraud_rule_engine" \
SPRING_DATASOURCE_USERNAME="fraud" \
SPRING_DATASOURCE_PASSWORD="fraud" \
./gradlew bootRun
```

Flyway runs automatically at startup in both Docker and local-host modes.

## Code Style

Java formatting is enforced in the repository through `.editorconfig`, Checkstyle, and the CI `check` job. Use four spaces, no tabs, grouped imports, and next-line opening braces for classes, records, constructors, and methods:

```java
public class RuleController
{

    public List<RuleDefinitionResponse> rules()
    {
        return ruleCatalogService.activeRules();
    }
}
```

## Continuous Integration

GitHub Actions CI is defined in [.github/workflows/ci.yml](.github/workflows/ci.yml). The workflow runs on pushes to `development`, `main`, and `feature/**`, on pull requests targeting `development` or `main`, and through manual dispatch. The badge at the top of this README shows the current `development` branch status.

The CI job uses Java 17, validates the Gradle wrapper, enables the Gradle dependency cache, runs `./gradlew clean test`, runs `./gradlew check`, builds the bootable jar with `./gradlew bootJar`, and validates the Docker image build. Test, result, and Checkstyle reports are uploaded as artifacts when a run fails. Dependency-Check remains a separate scheduled/manual report-only workflow in [.github/workflows/security-scan.yml](.github/workflows/security-scan.yml).

Dependency and secret hygiene review notes are maintained in [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md). Local Dependency-Check runs should use `NVD_API_KEY` from an environment variable or ignored `.local/` file, never from committed configuration.

## Configuration

Fraud evaluation thresholds are runtime configuration, not domain constants. The defaults live in `application.yml` and can be overridden by environment variables:

| Setting | Environment variable | Default |
| --- | --- | --- |
| OpenAPI JSON enabled | `SPRINGDOC_API_DOCS_ENABLED` | `true` |
| Swagger UI enabled | `SPRINGDOC_SWAGGER_UI_ENABLED` | `true` |
| OpenAPI title | `FRAUD_OPENAPI_TITLE` | `Fraud Rule Engine Service API` |
| OpenAPI version | `FRAUD_OPENAPI_VERSION` | `0.0.1-SNAPSHOT` |
| OpenAPI bearer security scheme name | `FRAUD_OPENAPI_BEARER_SECURITY_SCHEME_NAME` | `bearer-jwt` |
| Transaction evaluation API path | `FRAUD_API_PATH_TRANSACTION_EVALUATIONS` | `/api/v1/transaction-evaluations` |
| Reviewer transaction evaluation API path | `FRAUD_API_PATH_TRANSACTIONS_EVALUATE` | `/api/v1/transactions/evaluate` |
| Fraud alerts API path | `FRAUD_API_PATH_FRAUD_ALERTS` | `/api/v1/fraud-alerts` |
| Fraud alert detail API path | `FRAUD_API_PATH_FRAUD_ALERT_DETAIL` | `/api/v1/fraud-alerts/{alertId}` |
| Transaction fraud evaluation lookup API path | `FRAUD_API_PATH_TRANSACTION_FRAUD_EVALUATION` | `/api/v1/transactions/{transactionId}/fraud-evaluation` |
| Alert list default page | `FRAUD_API_PAGINATION_DEFAULT_PAGE` | `0` |
| Alert list default size | `FRAUD_API_PAGINATION_DEFAULT_SIZE` | `20` |
| Alert list maximum size | `FRAUD_API_PAGINATION_MAX_SIZE` | `100` |
| Exposed actuator endpoints | `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info,metrics,prometheus` |
| Health probes enabled | `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` | `true` |
| Health components visibility | `MANAGEMENT_ENDPOINT_HEALTH_SHOW_COMPONENTS` | `when_authorized` |
| Health details visibility | `MANAGEMENT_ENDPOINT_HEALTH_SHOW_DETAILS` | `when_authorized` |
| Readiness health contributors | `MANAGEMENT_ENDPOINT_HEALTH_GROUP_READINESS_INCLUDE` | `readinessState,db` |
| Prometheus export enabled | `MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED` | `true` |
| Application info name | `INFO_APP_NAME` | `FraudRuleEngineService` |
| Application info version | `INFO_APP_VERSION` | `0.0.1-SNAPSHOT` |
| Error and logging correlation header | `FRAUD_API_ERRORS_CORRELATION_ID_HEADER` | `X-Correlation-ID` |
| Correlation MDC key | `FRAUD_OBSERVABILITY_CORRELATION_ID_MDC_KEY` | `correlationId` |
| Correlation header maximum length | `FRAUD_OBSERVABILITY_CORRELATION_ID_MAX_LENGTH` | `128` |
| Log level pattern | `LOGGING_PATTERN_LEVEL` | `%5p [correlationId=%X{correlationId}]` |
| Safe internal error message | `FRAUD_API_ERRORS_INTERNAL_SERVER_ERROR_MESSAGE` | `An unexpected error occurred` |
| Generic not-found error message | `FRAUD_API_ERRORS_RESOURCE_NOT_FOUND_MESSAGE` | `Resource was not found` |
| Security public paths | `FRAUD_SECURITY_PUBLIC_PATHS` | `/actuator/health,/actuator/health/**,/v3/api-docs/**,/swagger-ui/**,/swagger-ui.html` |
| Transaction evaluation paths | `FRAUD_SECURITY_TRANSACTION_EVALUATE_PATHS` | `/api/v1/transaction-evaluations,/api/v1/transactions/evaluate` |
| Fraud alert read paths | `FRAUD_SECURITY_FRAUD_ALERTS_READ_PATHS` | `/api/v1/fraud-alerts,/api/v1/fraud-alerts/**,/api/v1/transactions/*/fraud-evaluation` |
| Rule read paths | `FRAUD_SECURITY_RULES_READ_PATHS` | `/api/v1/rules,/api/v1/rules/**` |
| Rule admin paths | `FRAUD_SECURITY_RULES_ADMIN_PATHS` | `/api/v1/rules/admin,/api/v1/rules/admin/**` |
| Actuator read paths | `FRAUD_SECURITY_ACTUATOR_READ_PATHS` | `/actuator,/actuator/info,/actuator/metrics,/actuator/metrics/**,/actuator/prometheus` |
| Transaction evaluation scope | `FRAUD_SECURITY_TRANSACTION_EVALUATE_SCOPE` | `transactions:evaluate` |
| Fraud alert read scope | `FRAUD_SECURITY_FRAUD_ALERTS_READ_SCOPE` | `fraud-alerts:read` |
| Rule read scope | `FRAUD_SECURITY_RULES_READ_SCOPE` | `rules:read` |
| Rule admin scope | `FRAUD_SECURITY_RULES_ADMIN_SCOPE` | `rules:admin` |
| Actuator read scope | `FRAUD_SECURITY_ACTUATOR_READ_SCOPE` | `actuator:read` |
| JWT issuer | `FRAUD_SECURITY_JWT_ISSUER_URI` | `fraud-rule-engine-local` |
| JWT audiences | `FRAUD_SECURITY_JWT_AUDIENCES` | `fraud-rule-engine-service` |
| JWT JWK set URI | `FRAUD_SECURITY_JWT_JWK_SET_URI` | empty |
| JWT public key location | `FRAUD_SECURITY_JWT_PUBLIC_KEY_LOCATION` | `classpath:security/local-dev-public-key.pem` |
| Merchant category maximum length | `FRAUD_API_VALIDATION_MERCHANT_CATEGORY_MAX_LENGTH` | `80` |
| Medium risk score threshold | `FRAUD_EVALUATION_RISK_THRESHOLD_MEDIUM` | `25` |
| High risk score threshold | `FRAUD_EVALUATION_RISK_THRESHOLD_HIGH` | `50` |
| Critical risk score threshold | `FRAUD_EVALUATION_RISK_THRESHOLD_CRITICAL` | `75` |
| Review decision minimum risk level | `FRAUD_EVALUATION_DECISION_THRESHOLD_REVIEW` | `MEDIUM` |
| Flagged decision minimum risk level | `FRAUD_EVALUATION_DECISION_THRESHOLD_FLAGGED` | `HIGH` |
| Application clock zone | `FRAUD_TIME_ZONE_ID` | `UTC` |
| Raw payload retention duration | `FRAUD_RAW_PAYLOAD_RETENTION_DURATION` | `P7D` |
| Raw payload cleanup enabled | `FRAUD_RAW_PAYLOAD_CLEANUP_ENABLED` | `true` |
| Raw payload cleanup cron | `FRAUD_RAW_PAYLOAD_CLEANUP_CRON` | `0 0 * * * *` |
| Docker PostgreSQL image | `POSTGRES_IMAGE` | `postgres:16-alpine` |
| Docker PostgreSQL database | `POSTGRES_DB` | `fraud_rule_engine` |
| Docker PostgreSQL user | `POSTGRES_USER` | `fraud` |
| Docker PostgreSQL password | `POSTGRES_PASSWORD` | `fraud` |
| Docker app host port | `APP_HOST_PORT` | `8080` |
| Docker PostgreSQL host port | `POSTGRES_HOST_PORT` | `5432` |
| Test PostgreSQL image | `TEST_POSTGRES_IMAGE` or `-Dtest.postgres.image` | `postgres:16-alpine` |
| Test PostgreSQL startup timeout | `TEST_POSTGRES_STARTUP_TIMEOUT` or `-Dtest.postgres.startup-timeout` | `PT2M` |

## Docker

Docker runtime is part of the Definition of Done. The Dockerfile builds the Spring Boot jar in a Gradle stage, copies only the bootable jar into a Java 17 runtime image, runs the application as a non-root user, exposes port `8080`, and uses the readiness probe for container health.

Start the local reviewer runtime:

```bash
docker compose up --build
```

Docker Compose starts PostgreSQL first, waits for `pg_isready`, then starts the service with database and local JWT validation environment variables. Flyway migrations run automatically on application startup. Optional Compose defaults are shown in [.env.example](.env.example); real `.env` files are ignored by Git.

Health checks:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/readiness
```

Generate a local reviewer token on the host, then call the API:

```powershell
$token = .\scripts\generate-jwt.ps1 -Profile system-ingestor
curl.exe -X POST "http://localhost:8080/api/v1/transactions/evaluate" `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer $token" `
  -d "@examples/high-risk-transaction.json"
```

```bash
TOKEN="$(./scripts/generate-jwt.sh --profile system-ingestor)"
curl -X POST "http://localhost:8080/api/v1/transactions/evaluate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d @examples/high-risk-transaction.json
```

Stop the runtime:

```bash
docker compose down
```

## Database

Flyway migrations live in `src/main/resources/db/migration`. `V1__create_schema.sql` creates the core PostgreSQL schema for idempotent event processing, transaction storage, fraud alert retrieval, rule catalog metadata, and auditable rule evaluation rows.

Raw payload storage is intentionally sanitized and retention-ready: tables that may hold payload snapshots include `sanitized_raw_payload` and `raw_payload_expires_at`, while the retention duration itself is not fixed in the schema.

Expired payload snapshots are cleaned by a scheduled job using `FRAUD_RAW_PAYLOAD_CLEANUP_CRON`. The job nulls `sanitized_raw_payload` and `raw_payload_expires_at` in `transactions` and `processed_events` when the expiry timestamp is in the past. It does not delete transaction, processed-event, rule-evaluation, or alert records, and logs cleanup counts only.

## Testing

Run the normal reviewer verification from a clean checkout:

```powershell
.\gradlew.bat clean test
.\gradlew.bat check
.\gradlew.bat bootJar
```

```bash
./gradlew clean test
./gradlew check
./gradlew bootJar
```

`clean test` starts PostgreSQL Testcontainers for database integration coverage, so Docker must be running. The test PostgreSQL image and startup timeout are configurable through `TEST_POSTGRES_IMAGE` / `-Dtest.postgres.image` and `TEST_POSTGRES_STARTUP_TIMEOUT` / `-Dtest.postgres.startup-timeout`.

The current verification baseline includes:

- Rule unit tests.
- Application service tests.
- PostgreSQL integration tests with Testcontainers.
- API integration tests.
- Security tests.
- Idempotency and concurrent duplicate race tests.
- OpenAPI contract tests.
- Flyway migration smoke tests.
- Docker Compose smoke tests.

Security/dependency review can be run separately:

```powershell
.\gradlew.bat dependencyCheckAnalyze
```

Use `NVD_API_KEY` from an environment variable or an ignored `.local/` file for faster NVD updates.

## Security Model

The service is an OAuth2 Resource Server. Protected API requests must include a JWT bearer token. Missing tokens and invalid tokens return `401`. Valid JWTs are authorized by scope at both URL and method level. Transaction evaluation requires `transactions:evaluate`; fraud alert and stored evaluation retrieval require `fraud-alerts:read`; rule catalog paths are prepared for `rules:read`; rule admin paths are prepared for `rules:admin`; actuator details require `actuator:read`. Authenticated requests without the required scope return `403`.

JWT verification is configured with issuer, audience, and either `FRAUD_SECURITY_JWT_JWK_SET_URI` or `FRAUD_SECURITY_JWT_PUBLIC_KEY_LOCATION`. Local development defaults to the bundled RS256 public key resource so the service can validate signed reviewer tokens without contacting an identity provider. Production deployments should configure the real issuer, accepted audience, and JWKS endpoint or mounted public-key resource. The service does not issue production tokens; token issuing and identity-provider setup are outside the target submission scope.

API errors use a consistent safe response shape: `code`, `message`, `correlationId`, and `fieldErrors`. Validation errors include field-level details, while duplicate races, unauthorized requests, forbidden requests, missing resources, and unexpected failures use controlled messages that do not expose stack traces, SQL errors, secrets, or implementation details. The correlation id is also returned in the configured response header for log lookup.

## Actuator Endpoints

Health probes are public and intentionally shallow:

- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

The readiness group includes `db`, so database availability affects readiness. Detailed health components are hidden from unauthenticated responses and are only shown when Spring considers the caller authorized for health details.

Operational detail endpoints require a JWT with the configured `actuator:read` scope:

- `GET /actuator/info`
- `GET /actuator/metrics`
- `GET /actuator/metrics/{name}`
- `GET /actuator/prometheus`

Only `health`, `info`, `metrics`, and `prometheus` are exposed by default. Dangerous actuator endpoints such as environment, beans, configprops, heapdump, threaddump, loggers, and shutdown are not exposed.

## Logging And Correlation

Every request receives a correlation id. The service accepts the configured `X-Correlation-ID` header when present, generates a new id when it is missing or unsafe, stores it in MDC under the configured key, and returns it on the response. This keeps support logs and client responses joinable without exposing request bodies.

Evaluation logs use stable key/value fields such as event name, event id, transaction id, decision, risk score, risk level, and rule counts. Logs must not include JWTs, authorization headers, card data, raw account numbers, customer personal identifiers, merchant details, full request payloads, SQL error text, or stack traces in client-facing responses.

## Local JWT Tokens

Reviewer token generation is local-only. The scripts use the bundled development signing key that matches `classpath:security/local-dev-public-key.pem`; this is not production authentication and must not be reused outside local review. Production must use an external identity provider, configured issuer, accepted audience, and JWKS or mounted public-key resource.

Generate a token in one command:

```powershell
$token = .\scripts\generate-jwt.ps1 -Profile system-ingestor
```

```bash
TOKEN="$(./scripts/generate-jwt.sh --profile system-ingestor)"
```

Supported profiles:

| Profile | Scopes |
| --- | --- |
| `system-ingestor` | `transactions:evaluate` |
| `fraud-analyst` | `fraud-alerts:read actuator:read` |
| `rule-admin` | `rules:read rules:admin actuator:read` |

The default issuer is `fraud-rule-engine-local`, the default audience is `fraud-rule-engine-service`, and token lifetime can be overridden with `FRAUD_LOCAL_JWT_TTL_SECONDS` or the script argument. Generated token files should be written under `.local/` or with a `.jwt` / `.token` extension so they stay ignored by Git. The dev token scripts are excluded from the Docker build context; the production image only receives the bootable application JAR.

## API Examples

Swagger UI is available locally at `http://localhost:8080/swagger-ui/index.html`, and the OpenAPI JSON is available at `http://localhost:8080/v3/api-docs`. The `prod` profile disables both endpoints through `application-prod.yml`, so Swagger remains a local-reviewer surface rather than a production API surface.

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

Example request payloads:

- [examples/high-risk-transaction.json](examples/high-risk-transaction.json)
- [examples/low-risk-transaction.json](examples/low-risk-transaction.json)
- [examples/duplicate-event.json](examples/duplicate-event.json)
- [examples/transaction-evaluation-request.json](examples/transaction-evaluation-request.json)

```bash
TOKEN="$(./scripts/generate-jwt.sh --profile system-ingestor)"

curl -X POST "http://localhost:8080/api/v1/transactions/evaluate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${TOKEN}" \
  -d @examples/high-risk-transaction.json
```

The response includes `transactionId`, `decision`, `riskScore`, `riskLevel`, `matchedRules`, and `evaluatedAt`. Duplicate event or transaction submissions are handled deterministically by the idempotency layer and return the stored evaluation when available.

Stored fraud alerts can be retrieved with optional filters and pagination:

```bash
TOKEN="$(./scripts/generate-jwt.sh --profile fraud-analyst)"

curl "http://localhost:8080/api/v1/fraud-alerts?customerId=customer-1&riskLevel=HIGH&page=0&size=20" \
  -H "Authorization: Bearer ${TOKEN}"
```

Retrieve one alert by alert id:

```bash
curl "http://localhost:8080/api/v1/fraud-alerts/b3ed20ca-bac6-45dc-a37e-d61001ee37ab" \
  -H "Authorization: Bearer ${TOKEN}"
```

Retrieve the stored evaluation for a transaction id:

```bash
curl "http://localhost:8080/api/v1/transactions/tx-1/fraud-evaluation" \
  -H "Authorization: Bearer ${TOKEN}"
```

Missing alerts or transaction evaluations return `404 RESOURCE_NOT_FOUND`. Alert list pagination defaults and limits are configurable through `fraud.api.pagination.*`.

## Design Trade-Offs

- A modular monolith keeps the write path simple and strongly consistent while still demonstrating clean boundaries.
- Code-first rules keep fraud logic deterministic, testable, and easy to explain.
- PostgreSQL and Flyway provide durable audit records with explicit schema evolution.
- The service stores matched and unmatched rule evaluation rows so decisions can be audited, at the cost of more write volume per transaction.
- Idempotency uses an application pre-check for the normal duplicate path and PostgreSQL unique constraints for concurrent races. That keeps the response deterministic without pretending the pre-check alone is enough.
- Sanitized raw payload retention helps local debugging and audit review, but payloads expire and sensitive fields are redacted before storage.
- Local JWT generation is deliberately reviewer-friendly and local-only. Production token issuing remains the responsibility of an external identity provider.
- REST is the v1 ingestion adapter; Kafka can be added later around the same application service.

## Known Limitations

- The implemented rules are deterministic assessment examples, not a complete banking fraud strategy.
- The service does not include a full analyst case-management workflow for assigning, commenting on, or closing alerts.
- Kafka ingestion is intentionally out of scope; transaction events are submitted through REST.
- Production identity-provider setup is out of scope. The app validates JWTs, but production token issuance must come from an external IdP.
- There is no tenant model or fine-grained customer/account authorization beyond configured JWT scopes.
- Raw payload retention cleanup removes expired payload snapshots, but database archiving and long-term cold storage policies are not implemented.
- Alert status transitions are persisted as a simple operational field; no workflow engine is included.
- The Docker Compose setup is for local review, not a hardened production deployment.

## Future Improvements

- Add Kafka or another event-stream adapter around the existing evaluation use case.
- Add analyst workflow APIs for alert assignment, status transitions, comments, and audit history.
- Integrate with a real identity provider and JWKS endpoint in a deployment environment.
- Add richer customer profile context, such as home country and historical behaviour windows sourced from dedicated profile data.
- Add rule administration APIs with approval workflow for enabling or disabling rules.
- Add database archival jobs for older transaction and evaluation records.
- Publish dashboard-ready Grafana/Prometheus examples for operational review.
- Add contract tests for generated OpenAPI clients or a Postman collection for manual reviewers.

## Project Governance

- Definition of Done: [docs/DEFINITION_OF_DONE.md](docs/DEFINITION_OF_DONE.md)
- Project board workflow and labels: [docs/PROJECT_BOARD.md](docs/PROJECT_BOARD.md)
- Sprint backlog snapshot: [docs/BACKLOG.md](docs/BACKLOG.md)
- Security review: [docs/SECURITY_REVIEW.md](docs/SECURITY_REVIEW.md)
