# Final Security Review

Card: `R.03`
Due: 18 June 2026
Priority: `P0 Critical`
Labels: `Release`, `Security`
Estimate: 2 hours

This document records the final security pass before submission.

## Review Summary

| Check                             | Result                                                                                                                                                  |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Password search                   | No production passwords found. Local PostgreSQL placeholders are documented and overrideable.                                                           |
| Token/API key search              | No committed real tokens or API keys found. NVD API keys remain external through environment variables, GitHub secrets, or ignored `.local/` files.     |
| Private key search                | No production private key files found. Reviewer JWT signing material is local-only and excluded from the Docker image.                                  |
| Account/card/customer data search | No real account, card, or customer data found. Security test fixtures use synthetic marker values.                                                      |
| JWT dev materials                 | Clearly documented as local-only reviewer tooling, not production authentication.                                                                       |
| Production Swagger exposure       | `application-prod.yml` disables both OpenAPI JSON and Swagger UI.                                                                                       |
| Actuator exposure                 | Only `health`, `info`, `metrics`, and `prometheus` are exposed by default; detail endpoints require `actuator:read`.                                    |
| Logging                           | Application logs decision summaries and counts only; raw payloads, authorization headers, tokens, SQL text, and stack traces are not logged to clients. |
| Authorization                     | Live checks confirmed protected endpoints return `401` without/invalid tokens and `403` for wrong scopes.                                               |

## Scan Results

The final scan excluded generated build output, Gradle caches, `.git`, and ignored local secret folders. Findings were reviewed manually.

Accepted local-reviewer placeholders:

- `POSTGRES_PASSWORD=fraud` appears only as local Docker/example configuration and is overrideable.
- `NVD_API_KEY` appears only as an environment variable, GitHub secret reference, or ignored local-file instruction.
- README curl examples use shell variables for bearer tokens, not committed token values.
- Security tests use synthetic sensitive markers to prove sanitization and safe error handling.

No real passwords, access tokens, refresh tokens, cloud keys, customer personal data, account numbers, card numbers, or production private keys were found.

## JWT Development Material

The repository includes local-only token generation scripts so reviewers can exercise the secured API without a full identity provider. This is intentionally not production authentication.

Controls:

- The generated tokens use the local issuer and audience defaults documented in the README.
- Generated token output should be written under `.local/` or to `.jwt` / `.token` files, all of which are ignored.
- The Docker build context excludes `scripts/generate-jwt.ps1` and `scripts/generate-jwt.sh`.
- The runtime image contains only `/app/app.jar`.
- The bootable jar contains the local development public key resource used for validation, not the token generation scripts.
- Production deployments must configure a real issuer, audience, and JWKS endpoint or mounted public-key resource.

## Production Profile

`src/main/resources/application-prod.yml` disables local API documentation surfaces:

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

This makes the production profile safer than the local reviewer profile.

## Live Authorization Checks

The running Docker Compose service was checked without printing token values:

| Endpoint scenario                                     | Expected | Observed |
|-------------------------------------------------------|---------:|---------:|
| `GET /actuator/health/readiness` without token        |      200 |      200 |
| `GET /actuator/info` without token                    |      401 |      401 |
| `GET /actuator/info` with `actuator:read`             |      200 |      200 |
| `POST /api/v1/transactions/evaluate` without token    |      401 |      401 |
| `POST /api/v1/transactions/evaluate` with wrong scope |      403 |      403 |
| `GET /api/v1/fraud-alerts` without token              |      401 |      401 |
| `GET /api/v1/fraud-alerts` with `fraud-alerts:read`   |      200 |      200 |
| `GET /api/v1/fraud-alerts` with invalid token         |      401 |      401 |

## Docker Image Check

The running application image exposes only `/app/app.jar` under `/app`. A jar listing from the host build found only the local public key resource under `BOOT-INF/classes/security/`; no token generation scripts or private-key-looking files were present in the runtime image content.

## Conclusion

No unresolved security blocker was found during R.03. The remaining security limitations are intentional submission-scope limits: local reviewer JWT generation is not production authentication, Docker Compose is not a hardened production deployment, and production identity-provider setup remains external.
