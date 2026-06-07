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

## Rule Catalog Synchronization

Fraud rules are code-first. At startup, `RuleDefinitionSeeder` reads every live `FraudRule` bean and upserts its metadata into `fraud_rules`. Code-owned fields such as name, description, severity, and score are refreshed when a rule class changes. Operational fields such as `enabled` are preserved on existing rows so disabling a rule in the database is not undone by a deployment.

The seeder never deletes rows for rules that are no longer present in code. Keeping historical catalog rows protects audit trails and existing rule evaluation references.

## Raw Payload Sanitization And Retention

The service stores sanitized request payloads only for short-term debugging and audit support. Raw financial events can accidentally include card numbers, account numbers, authorization tokens, or personal identifiers that are not required for fraud evaluation. `RawPayloadSanitizer` redacts configured sensitive field names before payloads are written to `processed_events` or `transactions`.

Payload records also include `raw_payload_expires_at`. This keeps retention explicit so cleanup can remove temporary payload context without deleting the durable transaction decision, rule evaluation, or alert history.

## Future Extraction Path

If Kafka ingestion, analyst workflow, or multi-tenant security becomes necessary later, those capabilities should be added as adapters or bounded modules around the same core use cases before considering service extraction.
