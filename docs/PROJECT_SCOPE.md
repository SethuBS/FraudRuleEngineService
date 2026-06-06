# Project Scope And Non-Goals

Card: `S0.01`
Due: 6 June 2026
Priority: `P0 Critical`
Labels: `Sprint 0`, `Architecture`, `Documentation`
Estimate: 1.5 hours

## Confirmed Scope

| Decision | Scope |
| --- | --- |
| Project name | `FraudRuleEngineService` |
| Architecture style | Modular monolith |
| Input | Categorized transaction event |
| Output | Fraud decision, risk score, rule evaluation results, and fraud alert where applicable |
| Storage | PostgreSQL |
| API style | REST |
| Security model | JWT OAuth2 Resource Server |
| Target submission date | 18 June 2026 |
| Official due date | 24 June 2026 |

## Included Capabilities

- Ingest categorized transaction events through REST.
- Normalize and validate transaction event fields.
- Evaluate transactions against deterministic code-first fraud rules.
- Persist normalized events, rule outcomes, and alerts.
- Return clear explanations for matched fraud rules.
- Expose retrieval APIs for alerts, event evaluations, and rule metadata.
- Run locally with Docker and PostgreSQL.
- Provide OpenAPI and reviewer-friendly API examples.
- Include automated test coverage appropriate to the risk of each feature.
- Provide production-oriented security, privacy, observability, and documentation controls.

## Non-Goals For Target Submission

- Full analyst workflow with assignment, comments, status history, and resolution actions.
- Full Kafka ingestion implementation.
- Multi-tenant RBAC and tenant-isolated queries.
- Notifications through email, SMS, webhook, or case-management systems.
- Machine learning models or adaptive scoring.
- Runtime business-user rule authoring.
- Production identity-provider provisioning.
- Distributed microservice decomposition.

## Scope Control Rule

The 18 June submission should be a complete, reviewable fraud rule engine service, not a broad platform. Any work outside this document must be treated as `P3 Stretch` unless it is explicitly promoted because it protects a `P0 Critical` or `P1 High` requirement.
