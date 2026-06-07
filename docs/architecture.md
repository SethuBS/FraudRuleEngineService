# Architecture Notes

Card: `S0.04`
Due: 6 June 2026
Priority: `P0 Critical`
Labels: `Sprint 0`, `Architecture`, `Backend`
Estimate: 2 hours

## Modular Monolith

FraudRuleEngineService is one deployable Spring Boot application with clear internal package boundaries. This avoids distributed-system overhead during the submission while keeping the codebase organized enough to evolve later.

```text
Client
  -> api
  -> application
  -> domain and rules
  -> infrastructure.persistence
  -> PostgreSQL
```

## Package Responsibilities

| Package | Responsibility |
| --- | --- |
| `com.capitec.fraud.api` | REST controllers, request and response DTOs, validation boundaries, and API error mapping. |
| `com.capitec.fraud.application` | Use-case orchestration, transaction boundaries, context loading, risk scoring, privacy controls, and query services. |
| `com.capitec.fraud.domain` | Framework-light fraud concepts, normalized transaction models, rule outcomes, and domain enums. |
| `com.capitec.fraud.rules` | Code-first fraud rule implementations. |
| `com.capitec.fraud.infrastructure.persistence` | JPA entities, repositories, Flyway-backed persistence integration, and database bootstrap support. |
| `com.capitec.fraud.infrastructure.security` | JWT Resource Server configuration and authorization rules. |
| `com.capitec.fraud.infrastructure.observability` | Correlation-id propagation, logging support, metrics, health, and operational filters. |
| `com.capitec.fraud.infrastructure.config` | Configuration properties, OpenAPI setup, clock wiring, and runtime defaults. |

## Guardrails

- Controllers should stay thin and delegate business work to application services.
- Domain and rule classes should not depend on Spring MVC or JPA where practical.
- Persistence entities should not be returned directly as API DTOs.
- Database constraints should protect correctness where concurrent requests can race.
- Rule evaluation should be deterministic and explainable.
- Security and observability concerns should be explicit rather than hidden in controllers.

## Idempotency And Duplicate Race Protection

Transaction evaluation uses a two-layer duplicate strategy. The application service first checks `processed_events.event_id` and `transactions.transaction_id` before running rules so normal repeated submissions can return the stored evaluation without doing more work.

The pre-check is not enough on its own because two identical requests can pass the read check at the same time. PostgreSQL unique constraints on `processed_events.event_id` and `transactions.transaction_id` remain the source of truth for concurrent duplicate races. If a race reaches the database, the duplicate key/data integrity exception is translated back into a lookup of the stored evaluation, producing the same deterministic API response instead of duplicate transaction, rule evaluation, or alert rows.

Processed event insertion, transaction persistence, rule evaluation rows, and alert creation run in one transaction. If any unique constraint rejects the write, the whole attempted duplicate write is rolled back.

## Transactional Evaluation Use Case

`EvaluateTransactionUseCase` is the application entry point for transaction evaluation. The use case delegates the normal evaluation flow to a transactional operation that performs the duplicate pre-check, calls the current rule engine implementation, persists the transaction, stores every rule evaluation result, creates an alert when the decision requires one, and records the processed event in the same transaction.

The outer use case catches database duplicate-key races only after the failed transaction has rolled back. It then performs a read-only lookup of the previously stored evaluation and returns that deterministic result. If no stored evaluation can be found, the database exception is translated into a safe application exception instead of exposing persistence internals.

## Transaction Evaluation API

`TransactionEvaluationController` exposes the primary reviewer path `POST /api/v1/transactions/evaluate`, backed by the configurable `fraud.api.paths.transactions-evaluate` property. The earlier `fraud.api.paths.transaction-evaluations` path remains available for compatibility with the stable DTO contract.

The controller accepts raw JSON so the payload can be sanitized and retained with expiry metadata before mapping into the domain model. Bean validation returns structured `400` responses, while duplicate event or transaction submissions flow through the idempotent use case and return the stored evaluation deterministically when available.

## Fraud Retrieval APIs

`FraudAlertController` exposes configured alert retrieval paths for listing alerts and fetching a single alert. The list endpoint supports optional `customerId`, `accountId`, `riskLevel`, `fromDate`, and `toDate` filters plus configured page and size defaults through `fraud.api.pagination.*`.

`TransactionEvaluationQueryController` exposes the configured transaction evaluation lookup path. It delegates to the application query service and reuses `TransactionEvaluationResponse`, so stored evaluation retrieval has the same reviewer-facing decision, score, risk level, matched-rule evidence, and evaluated timestamp as the submit response.

The API layer maps application views into response DTOs and never returns JPA entities directly. Missing alerts or transaction evaluations are translated to structured `404 RESOURCE_NOT_FOUND` responses.

## OpenAPI Documentation

Springdoc publishes the local OpenAPI document and Swagger UI using metadata from `fraud.openapi.*` configuration. The API document includes a JWT bearer security scheme, endpoint annotations, request examples, and response examples loaded from classpath resources so reviewer payloads stay aligned with versioned JSON files.

`application-prod.yml` disables both `/v3/api-docs` and Swagger UI. Local documentation paths are included in the configured public security paths so reviewers can inspect the contract without first configuring an identity provider, while protected business endpoints continue to require authentication.

## JWT Resource Server Security

The service validates bearer tokens as an OAuth2 Resource Server and does not issue production tokens. JWT verification is configured through `fraud.security.jwt.*`: issuer, accepted audiences, and either a JWKS URI or an RS256 public-key resource. Local development uses a classpath public key by default, while production can point at an external identity provider without changing code.

Authorization is enforced at both URL and method level. Public paths are configured through `fraud.security.public-paths`. Transaction evaluation requires the configured `transactions:evaluate` scope, fraud alert and stored-evaluation retrieval require `fraud-alerts:read`, future rule catalog retrieval is prepared for `rules:read`, future rule administration is prepared for `rules:admin`, and actuator detail endpoints require `actuator:read`. Requests without a token or with an invalid token return `401`; authenticated requests without the required scope return `403`.

Only health endpoints and local API documentation paths are public by default. Actuator details such as info, metrics, and Prometheus are protected, and `application-prod.yml` disables OpenAPI and Swagger UI for production deployments.

The reviewer JWT generator is deliberately local-only. `scripts/generate-jwt.ps1` and `scripts/generate-jwt.sh` sign RS256 tokens for the bundled local public key and support system ingestor, fraud analyst, and rule admin profiles. These scripts are excluded from the Docker build context, generated token files are ignored by Git, and production deployments must use an external IdP rather than the local development signer.

## Code-First Rule Engine

Fraud rules are implemented by adding Spring beans that implement `FraudRule`. The HTTP transaction-evaluation contract does not change when a new rule is added. Each rule owns metadata such as code, name, description, severity, and default score, and evaluates a `TransactionContext` that contains the normalized transaction plus slots for historical evaluation and alert data.

`FraudRuleEngine` injects all rule beans, sorts them by rule code for deterministic execution, checks the operational catalog before evaluating a rule, and converts each enabled rule outcome into a `RuleEvaluationResult`. Both matched and unmatched outcomes are returned so persistence can store auditable `matched=true` and `matched=false` rows.

Rule enabled state is read through an application port. The PostgreSQL adapter uses `fraud_rules.enabled` when a catalog row exists and falls back to the configured `fraud.rule-catalog.enabled-by-default` value for rule definitions that have not been seeded yet.

## Risk Scoring And Decision Policy

`FraudDecisionService` aggregates matched rule scores into the final `riskScore`, `riskLevel`, and `FraudDecision`. Unmatched rules remain in the audit trail but contribute `RiskScore.ZERO`, keeping the final score explainable from matched evidence only.

Risk thresholds, decision thresholds, and maximum score are supplied through `fraud.evaluation.*` configuration. `RiskPolicy` caps aggregated scores at the configured maximum before mapping to risk level and decision, so duplicate/idempotent reconstruction and fresh evaluations produce the same deterministic outcome.

## High Value Transaction Rule

`HighValueTransactionRule` flags transactions whose amount exceeds the configured threshold. The threshold, risk score, and severity are supplied through `fraud.rules.high-value-transaction.*` properties so deployment environments can tune the rule without changing Java code. Equal-to-threshold transactions are treated as within threshold; only values above the threshold match.

## Velocity Transaction Rule

`VelocityTransactionRule` flags customers or accounts that exceed the configured transaction count within the configured minute window. The rule reads recent transaction history from `TransactionContext`, which keeps the rule unit-testable while allowing production to supply database-backed history through `DatabaseRecentTransactionLookup`.

The lookup queries transactions by customer or account using transaction timestamps, excludes the current transaction id, and is supported by customer/account plus transaction timestamp indexes. The rule reason includes the observed count, configured time window, and configured threshold for audit readability.

## Foreign Country Transaction Rule

`ForeignCountryTransactionRule` flags transactions whose supplied country differs from the configured expected home country. The expected country, risk score, and severity are supplied through `fraud.rules.foreign-country-transaction.*` properties so deployments can tune the assessment baseline without changing Java code.

The current home-country source is configuration-backed for the assessment slice. Missing optional transaction country data is treated as a non-match with a clear explanation instead of failing the evaluation. A future customer profile lookup can replace the configured default through the transaction context without changing the HTTP contract.

## Risky Merchant Category Rule

`RiskyMerchantCategoryRule` flags transactions whose normalized merchant category appears in the configured risky category list. The `fraud.rules.risky-merchant-category.risk-categories` list, risk score, and severity are supplied through configuration so operations can tune risky merchant categories without a code change.

Transaction categories are normalized to uppercase domain codes before comparison, so configured values and request values match case-insensitively while still using the same domain validation as the API contract.

## Suspicious Merchant Rule

`SuspiciousMerchantRule` flags transactions whose merchant ID or merchant name matches configured suspicious merchant indicators. The `fraud.rules.suspicious-merchant.merchant-ids` and `fraud.rules.suspicious-merchant.merchant-name-fragments` lists, risk score, and severity are supplied through configuration so merchant watchlists can change without Java code changes.

Merchant IDs and merchant names are trimmed, whitespace-normalized, and uppercased before comparison. Missing optional merchant names produce a safe non-match unless the configured merchant ID list matches the transaction merchant ID.

## Unusual Amount Rule

`UnusualAmountRule` flags transactions whose amount exceeds the configured multiplier of the historical average for the same customer or account. The multiplier, risk score, and severity are supplied through `fraud.rules.unusual-amount.*` properties so the threshold can change without Java code changes.

Historical average lookup is exposed to rules through `HistoricalAverageAmountLookup`. The PostgreSQL adapter calculates the average from stored transactions with the same currency, matching the current customer or account and excluding the current transaction id. If no baseline exists, the rule returns an explicit non-match instead of failing or guessing.

## Rule Catalog Synchronization

Fraud rules are code-first. At startup, `RuleDefinitionSeeder` reads every live `FraudRule` bean and upserts its metadata into `fraud_rules`. Code-owned fields such as name, description, severity, and score are refreshed when a rule class changes. Operational fields such as `enabled` are preserved on existing rows so disabling a rule in the database is not undone by a deployment.

The seeder never deletes rows for rules that are no longer present in code. Keeping historical catalog rows protects audit trails and existing rule evaluation references.

## Raw Payload Sanitization And Retention

The service stores sanitized request payloads only for short-term debugging and audit support. Raw financial events can accidentally include card numbers, account numbers, authorization tokens, or personal identifiers that are not required for fraud evaluation. `RawPayloadSanitizer` redacts configured sensitive field names before payloads are written to `processed_events` or `transactions`.

Payload records also include `raw_payload_expires_at`. This keeps retention explicit so cleanup can remove temporary payload context without deleting the durable transaction decision, rule evaluation, or alert history.

## Future Extraction Path

If Kafka ingestion, analyst workflow, or multi-tenant security becomes necessary later, those capabilities should be added as adapters or bounded modules around the same core use cases before considering service extraction.
