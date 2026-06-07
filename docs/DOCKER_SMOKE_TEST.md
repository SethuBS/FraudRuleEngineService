# Docker Smoke Test

Card: `S4.07`
Due: 17 June 2026
Priority: `P0 Critical`
Labels: `Sprint 4`, `DevOps`, `Testing`, `Reliability`
Estimate: 4 hours

## Summary

An end-to-end reviewer flow was executed from a clean checkout on 7 June 2026. The checkout used `development` commit `c2cf410`, which contains the completed reviewer README and architecture notes.

The smoke test proved that a reviewer can clone the repository, build the Docker image, start PostgreSQL and the application with Docker Compose, apply Flyway migrations, generate local JWTs, evaluate the high-risk transaction example, retrieve the stored alert and transaction evaluation, open Swagger/OpenAPI locally, restart the containers, and continue using the app.

## Commands Exercised

```powershell
git clone --branch development https://github.com/SethuBS/FraudRuleEngineService.git C:\Development\FraudRuleEngineService-smoke-s4-07-20260607-233420
cd C:\Development\FraudRuleEngineService-smoke-s4-07-20260607-233420
docker compose up --build -d
```

Readiness was checked with:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health/readiness' -Method Get
```

Local JWTs were generated with:

```powershell
$ingestorToken = .\scripts\generate-jwt.ps1 -Profile system-ingestor
$analystToken = .\scripts\generate-jwt.ps1 -Profile fraud-analyst
```

The high-risk transaction example was submitted to:

```text
POST http://localhost:8080/api/v1/transactions/evaluate
```

The stored fraud alert was retrieved from:

```text
GET http://localhost:8080/api/v1/fraud-alerts?customerId=customer-1&riskLevel=CRITICAL&page=0&size=20
```

The stored transaction evaluation was retrieved from:

```text
GET http://localhost:8080/api/v1/transactions/tx-high-risk-1/fraud-evaluation
```

Swagger/OpenAPI was checked at:

```text
GET http://localhost:8080/swagger-ui/index.html
GET http://localhost:8080/v3/api-docs
```

The runtime was restarted with:

```powershell
docker compose stop
docker compose start
```

## Results

| Check | Result |
| --- | --- |
| Clean checkout completed | Passed |
| Docker image built | Passed |
| PostgreSQL container healthy | Passed |
| Application container healthy | Passed |
| Readiness endpoint returned `UP` | Passed |
| Flyway applied migrations | Passed, 4 migrations applied to schema version `v4` |
| Local JWT generation worked | Passed |
| High-risk transaction evaluation worked | Passed, `FLAGGED`, score `100`, risk level `CRITICAL` |
| Fraud alert retrieval worked | Passed, 1 alert returned for `customer-1` and `CRITICAL` |
| Stored transaction evaluation retrieval worked | Passed |
| Swagger UI returned `200` | Passed |
| OpenAPI JSON returned `200` | Passed |
| Stop/start restart worked | Passed |
| Stored evaluation still worked after restart | Passed |

## Required Fix Found

The smoke test found one documentation mismatch. The high-risk example evaluates to `CRITICAL`, while the README alert retrieval sample previously filtered by `riskLevel=HIGH`. The README sample was updated to `riskLevel=CRITICAL` so the documented reviewer flow returns the created alert.

No Java code changes were required.
