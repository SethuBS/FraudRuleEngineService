# Architecture And Interview Notes

Card: `S4.06`
Due: 17 June 2026
Priority: `P1 High`
Labels: `Sprint 4`, `Documentation`, `Architecture`, `Interview`
Estimate: 4 hours

These notes are written for a two-hour technical interview. They explain the decisions behind the implementation, the trade-offs, and the first changes that would be made if the system evolved beyond the assessment scope.

## Short Architecture Pitch

FraudRuleEngineService is a modular monolith. It has one deployable Spring Boot application, one PostgreSQL database, and clear internal package boundaries for API, application use cases, domain objects, rules, persistence, security, observability, and configuration.

The most important workflow is deliberately simple:

1. A categorized transaction is submitted through a secured REST API.
2. The application use case checks idempotency and loads historical context.
3. The code-first rule engine evaluates every enabled rule.
4. The decision service turns matched rule scores into a risk score, risk level, and final decision.
5. The service stores the transaction, processed event, rule evaluations, and fraud alert in one transaction.
6. Retrieval APIs expose alerts and stored evaluations without returning persistence entities directly.

## Why A Modular Monolith

The modular monolith was chosen because the assessment needs a runnable, reviewable, production-minded service rather than a distributed demo. The core transaction evaluation path benefits from a single database transaction: either the transaction, processed-event row, rule evidence, and alert all commit together, or none of them do.

This design also keeps local review straightforward. A reviewer can run the app and PostgreSQL with Docker Compose, generate a local token, submit a transaction, and inspect the stored result without coordinating multiple services, brokers, deployments, or identity-provider components.

The monolith is still modular because the packages are intentionally separated:

- `api` owns HTTP contracts and validation.
- `application` owns use-case orchestration and transaction boundaries.
- `domain` owns framework-light fraud concepts.
- `rules` owns the code-first fraud rules.
- `infrastructure.persistence` owns JPA and database adapters.
- `infrastructure.security` owns JWT and authorization configuration.
- `infrastructure.observability` owns correlation ids, logging, metrics, and health.
- `infrastructure.config` owns runtime properties and wiring.

That gives the codebase a migration path without paying distributed-system costs on day one.

## Why Not Multiple Services

Multiple services would be premature for this scope. Splitting ingestion, rule execution, alerts, and rule catalog into separate deployables would introduce service-to-service authentication, network failure handling, distributed tracing, eventual consistency, cross-service test setup, schema ownership questions, deployment sequencing, and more operational work.

The current problem is mostly a strongly consistent write path. For that shape, service extraction would make correctness harder before it creates enough value. If separate scaling or independent team ownership becomes real, the existing application boundaries show where extraction could happen later.

## Code-First Rule Engine

Fraud rules are code-first because fraud logic should be type-safe, version-controlled, reviewable, and unit-testable. A rule implements `FraudRule`, provides metadata such as code, name, description, severity, and default score, and evaluates a `TransactionContext`.

The engine injects all rule beans, sorts them deterministically by rule code, checks whether each rule is operationally enabled, and returns one auditable `RuleEvaluationResult` per enabled rule. This keeps rule execution repeatable and easy to explain in logs, persistence rows, tests, and API responses.

## How New Rules Are Added

To add a new rule:

1. Create a new class that implements `FraudRule`.
2. Add configuration properties for thresholds, score, severity, and any rule-specific lists.
3. Use `TransactionContext` for transaction data and historical lookup data.
4. Return a clear matched or unmatched result with a human-readable reason.
5. Add unit tests for match, non-match, boundary, and missing optional data cases.
6. Let `RuleDefinitionSeeder` upsert the rule metadata into `fraud_rules` on startup.

No controller change is required for a new rule. If the rule needs new data that is not currently present, the preferred first step is to enrich `TransactionContext` or a backend lookup adapter rather than changing the HTTP contract immediately.

## Why The HTTP Contract Stays Stable

The transaction evaluation API accepts categorized transaction facts, not rule-specific inputs. The response returns a generic list of matched rules with rule code, name, score, severity, matched flag, and reason. Because the contract talks about rule evidence generically, adding a new rule changes the internal rule set and persisted evidence, not the request or response shape.

This is important for clients. Producers can keep sending the same transaction event contract while the service evolves fraud logic behind the boundary.

## Idempotency And Duplicate Race Handling

Transaction evaluation uses a two-layer duplicate strategy:

- The application pre-check looks for an existing processed event or transaction before evaluating rules.
- PostgreSQL unique constraints on `processed_events.event_id` and `transactions.transaction_id` protect the race condition where two identical requests pass the pre-check at the same time.

The pre-check improves the normal path because a repeated request can return the stored evaluation without doing unnecessary work. The database constraint is still necessary because only the database can enforce uniqueness atomically under concurrency.

If a duplicate insert race reaches the database, the attempted write rolls back and the service performs a read-only lookup of the stored evaluation. The API then returns a deterministic response instead of creating duplicate transaction, processed-event, rule-evaluation, or alert rows.

## Why Store Matched False Rows

The service stores both matched and unmatched rule evaluation rows because auditability is stronger when the system can show what was evaluated, not only what fired. A future reviewer, analyst, or support engineer can see that a rule was present, enabled, evaluated, and did not match for a specific reason.

The trade-off is more write volume per transaction. For this assessment, the audit value is worth it. In a high-volume production system, the same choice would be reviewed against storage cost, retention policy, and regulatory audit requirements.

## RuleDefinitionSeeder

`RuleDefinitionSeeder` keeps the database rule catalog aligned with live code. At startup, it reads all `FraudRule` beans and upserts their code-owned metadata into `fraud_rules`.

The seeder updates fields owned by code, such as name, description, severity, and score. It preserves operational fields such as `enabled`, so an operator can disable a rule in the database without a deployment accidentally re-enabling it.

The seeder does not delete old rule rows. That protects historical rule evaluation records and avoids breaking audit trails when a rule is renamed, replaced, or removed from code.

## Sanitized Raw Payload Retention

The service stores sanitized payload snapshots for short-term debugging and audit support only. Payloads are sanitized before persistence so obvious sensitive values, such as authorization tokens, card-like values, account numbers, and personal identifiers beyond required IDs, are not stored.

Each stored payload has `raw_payload_expires_at`. The cleanup job nulls expired payload snapshots while keeping transaction identity, decisions, rule evaluations, alerts, and processed-event records intact. This is the privacy trade-off: retain enough context for support, but avoid permanent raw payload storage.

## Swagger Local Only

Swagger and OpenAPI are enabled for local review so a reviewer can inspect the API quickly and compare examples with the real request contract. In the production profile, Swagger UI and OpenAPI JSON are disabled.

The trade-off is convenience versus exposure. Local documentation helps assessment and developer productivity. Production should avoid exposing unnecessary discovery surfaces unless there is an explicit authenticated internal API documentation strategy.

## Security Model

The service is an OAuth2 Resource Server. It validates JWT bearer tokens using configured issuer, audience, and either a JWKS URI or a public key resource. The service does not issue production tokens.

Authorization uses scopes:

- `transactions:evaluate` for transaction evaluation.
- `fraud-alerts:read` for alerts and stored evaluation retrieval.
- `rules:read` for rule catalog reads.
- `rules:admin` for future rule administration.
- `actuator:read` for operational detail endpoints.

Health probes remain public and shallow so runtime platforms can check liveness and readiness. Details, metrics, and Prometheus require scope.

## Observability And Safe Operations

Every request gets a correlation id. The service accepts a configured correlation header, generates one when missing or unsafe, stores it in MDC, and returns it to the caller. API errors include the correlation id so client reports can be matched with server logs.

Logging is intentionally limited to support-safe facts: event identifiers, transaction identifiers, decision, risk score, risk level, rule counts, and error categories. Logs should not contain JWTs, authorization headers, raw payloads, card data, raw account numbers, SQL text, or customer personal details.

Actuator exposure is narrow. Health, liveness, and readiness are exposed for platform checks. Readiness includes the database. Metrics and Prometheus are available only when configured and authorized.

## What Would Change First For Kafka

The first change would be adding a Kafka adapter around the existing `EvaluateTransactionUseCase`, not rewriting the domain or rule engine. A consumer would deserialize transaction events, validate them, pass them to the same use case, and publish optional outcome events after successful persistence.

The next design points would be:

- Message key choice for ordering and partitioning, likely event id, account id, or customer id depending on the use case.
- Consumer retry and dead-letter handling.
- Idempotency reuse through `processed_events.event_id`.
- Outbox pattern if downstream decision or alert events must be published reliably after the database commit.
- Operational metrics for lag, retries, duplicate events, and processing failures.

## What Would Change First For Analyst Workflow

The first change would be expanding the alert aggregate and APIs, not the evaluation endpoint. Alerts would gain assignment, status transitions, comments, resolution reasons, and an audit trail.

The likely additions are:

- Alert status transition rules.
- Analyst assignment and ownership fields.
- Comment and investigation-note tables.
- Search and filtering for queues.
- Method-level authorization for analyst actions.
- Optimistic locking to prevent overwriting concurrent analyst updates.

The existing alert retrieval APIs and persisted alert rows provide the base to grow from.

## What Would Change First For Multi-Tenant Security

The first change would be introducing a tenant identifier into the domain model, persistence schema, JWT claims, and repository filters. Every stored business row would need tenant ownership, and every query would need tenant isolation by default.

The security changes would include:

- Validating tenant claims from the JWT.
- Mapping scopes and roles within a tenant context.
- Enforcing tenant filters in application services and repositories.
- Adding database indexes that include tenant id.
- Adding tests proving cross-tenant reads and writes are denied.

For higher assurance, later production versions could consider PostgreSQL row-level security, separate schemas, or separate databases depending on regulatory and operational requirements.

## Interview Trade-Off Summary

| Decision                          | Benefit                                    | Cost Or Risk                                       | Why It Is Acceptable Now                                                       |
|-----------------------------------|--------------------------------------------|----------------------------------------------------|--------------------------------------------------------------------------------|
| Modular monolith                  | Simple deployment and atomic writes        | Less independent scaling                           | Assessment scope values correctness and reviewability first                    |
| Code-first rules                  | Type-safe, testable, versioned fraud logic | Rule updates require deployment unless config-only | Clear audit trail and deterministic behaviour matter more than runtime editing |
| Matched and unmatched rows        | Stronger explainability                    | More storage per evaluation                        | Auditability is a core requirement                                             |
| Pre-check plus unique constraints | Fast duplicate path and concurrency safety | More duplicate-handling code                       | Correctness under race conditions requires database enforcement                |
| Sanitized payload retention       | Debug support without permanent payloads   | Sanitizer must stay maintained                     | Retention and redaction reduce privacy risk                                    |
| Local JWT scripts                 | Reviewer-friendly security testing         | Not production auth                                | Clearly marked local-only; production uses external IdP                        |
| Swagger local-only                | Easy review and API discovery              | Production docs surface disabled                   | Local convenience without production exposure                                  |

## Interview Closing Answer

The project is intentionally small in deployment shape but production-minded in boundaries. It demonstrates a secure, auditable fraud evaluation flow with deterministic rules, idempotent persistence, safe errors, correlation ids, Testcontainers coverage, Docker runtime, and reviewer documentation. The main future evolution would be adding adapters and bounded modules around the existing use cases before extracting separate services.
