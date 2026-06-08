# Release Verification

This document records the local R.01 verification run for the release submission path.

## R.01 - Final Build, Tests, and Dependency Check

| Field | Value |
|-------|-------|
| Due date | 18 June 2026 |
| Priority | P0 Critical |
| Labels | Release, Testing, DevOps, Security |
| Branch | `release/r-01-final-verification` |
| Verification date | 8 June 2026 |

## Release Hardening Changes

- Upgraded managed runtime patch versions through configurable Gradle properties and environment variables:
  - Tomcat: `10.1.55`
  - Log4j: `2.25.4`
  - Swagger UI webjar: `5.32.5`
- Kept Checkstyle on Gradle-compatible `10.21.4` and added Checkstyle-only classpath overrides for:
  - `commons-beanutils` `1.11.0`
  - `commons-lang3` `3.20.0`
  - `plexus-utils` `3.6.1`
- Set the default Dependency Check build threshold to fail on critical CVSS findings through `DEPENDENCY_CHECK_FAIL_CVSS`, with a default of `9.0`.
- Added `SPRINGDOC_SWAGGER_UI_VERSION` so Docker/local Swagger UI resolves the patched webjar directory.

## Commands And Results

| Command | Result |
|---------|--------|
| `.\gradlew.bat clean test --no-daemon` | Passed |
| `.\gradlew.bat bootJar --no-daemon` | Passed |
| `.\gradlew.bat check --no-daemon` | Passed |
| `.\gradlew.bat dependencyCheckAnalyze --no-daemon --rerun-tasks` | Passed with 0 vulnerabilities |
| `docker compose up --build -d` | Passed; PostgreSQL and application containers healthy |
| README curl collection suite | Passed all 13 checks |

Dependency Check was run with the ignored local NVD data cache to avoid release verification depending on transient NVD API update availability:

```powershell
$env:DEPENDENCY_CHECK_DATA_DIRECTORY = (Resolve-Path .local\dependency-check-data).Path
$env:DEPENDENCY_CHECK_AUTO_UPDATE = 'false'
.\gradlew.bat dependencyCheckAnalyze --no-daemon --rerun-tasks
```

The NVD API key remains local-only in `.local\nvd-api-key.txt` or `NVD_API_KEY`; it is not committed.

## Test Report

Gradle test report:

- `build/reports/tests/test/index.html`

JUnit XML summary:

| Metric | Count |
|--------|------:|
| Test classes | 38 |
| Tests | 152 |
| Failures | 0 |
| Errors | 0 |
| Skipped | 0 |

## Dependency Check Report

Generated reports:

- `build/reports/dependency-check-report.html`
- `build/reports/dependency-check-report.json`

Report summary:

| Severity | Count |
|----------|------:|
| Critical | 0 |
| High | 0 |
| Medium | 0 |
| Low | 0 |

## Docker Smoke Result

Docker Compose built the image and started:

- `fraudruleengineservice-postgres-1`
- `fraudruleengineservice-fraud-rule-engine-1`

Observed runtime checks:

- PostgreSQL health check passed.
- Application readiness returned `{"status":"UP"}`.
- Flyway validated 4 migrations and reported the schema up to date.
- Application started with embedded Apache Tomcat `10.1.55`.
- Swagger UI returned `200` at `/swagger-ui/index.html`.
- OpenAPI JSON returned `200` at `/v3/api-docs`.

## Curl Collection Result

The README curl collection passed:

- Readiness endpoint returned `200`.
- Swagger UI returned `200`.
- OpenAPI JSON returned `200`.
- High-risk transaction evaluation returned `FLAGGED`, score `100`, risk level `CRITICAL`.
- Duplicate event returned the deterministic stored evaluation.
- Fraud alert list returned results for `customer-1` and `riskLevel=CRITICAL`.
- Fraud alert detail retrieval returned `200`.
- Stored transaction fraud evaluation retrieval returned `200`.
- Low-risk transaction evaluation returned `200`.
- Missing token returned `401`.
- Invalid token returned `401`.
- Wrong scope returned `403`.
- Invalid request returned `400` with field errors.

## Remaining Release Check

After this branch is pushed, GitHub Actions CI must be checked on the pull request before merging to `development`.
