# Security Review

This review records the Sprint 3 dependency and secret hygiene checks for `S3.10`, plus the release security updates completed before submission. The final release security pass is recorded in [FINAL_SECURITY_REVIEW.md](FINAL_SECURITY_REVIEW.md).

## Dependency Vulnerability Review

Dependency-Check is configured in `build.gradle` through the OWASP Dependency-Check Gradle plugin. It generates HTML and JSON reports under `build/reports/`:

```powershell
.\gradlew.bat dependencyCheckAnalyze --no-daemon
```

For reliable local runs, use an NVD API key without committing it:

```powershell
$env:NVD_API_KEY = (Get-Content .local\nvd-api-key.txt -Raw).Trim()
.\gradlew.bat dependencyCheckAnalyze --no-daemon
```

The same key is expected in GitHub as the `NVD_API_KEY` repository secret for the scheduled/manual `Security Scan` workflow. The first local unauthenticated attempts on 7 June 2026 timed out during NVD data update and did not produce a report. After supplying `NVD_API_KEY` through the ignored `.local/nvd-api-key.txt` file, the authenticated local run completed successfully on 7 June 2026 and generated:

- `build/reports/dependency-check-report.html`
- `build/reports/dependency-check-report.json`

Report summary:

| Severity | Count |
|----------|------:|
| Critical |     6 |
| High     |     8 |
| Medium   |    13 |
| Low      |     2 |

Findings reviewed:

| Dependency or asset                                      | Scope observed                                       | Notes                                                                                                                      |
|----------------------------------------------------------|------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `tomcat-embed-core` / `tomcat-embed-websocket` `10.1.54` | Application runtime via Spring Boot web starter      | Real runtime finding. Track with Spring Boot/Tomcat managed dependency updates before release.                             |
| `log4j-api` / `log4j-to-slf4j` `2.24.3`                  | Application runtime via Spring Boot logging starter  | Real runtime finding. Track with Spring Boot managed dependency updates before release.                                    |
| `swagger-ui` `5.32.2` bundled `DOMPurify` assets         | Local OpenAPI UI runtime; disabled in `prod` profile | Real local-reviewer surface. Track springdoc/swagger-ui update before release and keep Swagger disabled in production.     |
| `commons-beanutils` `1.10.1`                             | Build tooling through Checkstyle                     | Not on the application runtime classpath. Do not suppress without a narrow justification; track Checkstyle/tooling update. |
| `plexus-utils` `3.3.0`                                   | Build tooling through Checkstyle/Doxia               | Not on the application runtime classpath. Do not suppress without a narrow justification; track Checkstyle/tooling update. |
| `commons-lang3` `3.17.0`                                 | Build tooling through Checkstyle/Doxia               | Application runtime resolves `commons-lang3` `3.20.0`; this finding is from the Checkstyle configuration.                  |

No suppressions were added during this review. Findings are retained in the report so they remain visible until upgraded or individually justified.

## Release Verification Update - 8 June 2026

R.01 resolved the previously reviewed release-blocking dependency findings without adding Dependency-Check suppressions.

Runtime dependency updates:

- Tomcat is managed to `10.1.55` through configurable Gradle dependency management.
- Log4j is managed to `2.25.4` through configurable Gradle dependency management.
- Swagger UI is overridden to `5.32.5`, with `SPRINGDOC_SWAGGER_UI_VERSION` set to the same version so the local Swagger resource mapping works in Docker.

Build-tooling updates:

- Checkstyle remains on Gradle-compatible `10.21.4`.
- Checkstyle-only `commons-beanutils` is overridden to `1.11.0`.
- Checkstyle-only `commons-lang3` is overridden to `3.20.0`.
- Checkstyle-only `plexus-utils` is overridden to `3.6.1`.

Dependency-Check hardening:

- `DEPENDENCY_CHECK_AUTO_UPDATE` controls online update behavior.
- `DEPENDENCY_CHECK_DATA_DIRECTORY` or `-Pdependency-check.data-directory` can point to an ignored local NVD cache for repeatable release checks. The default local cache is `.gradle/dependency-check-data`, which is ignored and survives `clean`.
- `DEPENDENCY_CHECK_FAIL_CVSS` defaults to `9.0`, so critical findings fail the build.

The final R.01 Dependency-Check run generated HTML and JSON reports and found 0 vulnerabilities. The detailed release run is recorded in [RELEASE_VERIFICATION.md](RELEASE_VERIFICATION.md).

## Suppressions

Suppressions live in `config/dependency-check/suppressions.xml`. The file is intentionally minimal and currently has no active suppressions. Any future suppression must include a clear justification, scope the affected dependency or CVE narrowly, and be revisited before release.

## Secret Hygiene Review

The review checks tracked files for private keys, API tokens, bearer tokens, cloud access keys, and accidental passwords. Scans should avoid printing suspected secret values; prefer file-name or marker-only output.

Findings:

- No production secrets are intentionally committed.
- Targeted scans for private-key headers, GitHub tokens, AWS access keys, Slack tokens, bearer tokens, and committed NVD API key assignments returned no matches outside ignored/build files.
- `.env`, `.env.*`, `.local/`, `dev-secrets/`, `secrets/`, private key files, generated JWTs, and token files are ignored by Git.
- `POSTGRES_PASSWORD=fraud` in `.env.example`, `application.yml`, and `docker-compose.yml` is a local development placeholder and is overrideable by environment variable.
- `src/main/resources/security/local-dev-public-key.pem` contains only the local development public key used for JWT validation.
- `scripts/generate-jwt.ps1` and `scripts/generate-jwt.sh` contain local-only reviewer token signing material. They are for development review only and must not be used for production authentication.
- Generated reviewer tokens should be written under `.local/` or to `.jwt` / `.token` files so they stay ignored.
- NVD API keys must be supplied through `NVD_API_KEY`, GitHub repository secrets, or ignored local files only.

## Final Security Review Update - 8 June 2026

R.03 repeated the final release security pass from a reviewer perspective:

- Password and token searches found no committed real production secrets.
- Private-key searches found no production private key files. The local reviewer JWT signing material remains explicitly development-only and excluded from the Docker image.
- Account/card/customer-data searches found no real financial or customer data. Sanitizer tests now use synthetic marker values rather than card- or account-shaped numbers.
- `application-prod.yml` disables OpenAPI JSON and Swagger UI.
- Actuator exposure remains limited to health, info, metrics, and Prometheus; detail endpoints require the configured `actuator:read` scope.
- Live authorization checks confirmed `401` for missing/invalid tokens, `403` for wrong scope, and success for correct scope.
- Docker image checks found only `/app/app.jar` under `/app`; the jar contains the local public key resource but no token generation scripts.

The detailed R.03 evidence is recorded in [FINAL_SECURITY_REVIEW.md](FINAL_SECURITY_REVIEW.md).

## Docker Image Hygiene

The Docker build context excludes the local JWT generation scripts and generated secret material through `.dockerignore`. The production image is built from the bootable Spring Boot jar only. A local image filesystem check found no `generate-jwt` scripts, private-key-looking files, `.key`, `.jwt`, or `.token` files under `/app`. A jar listing check found the bundled public key resource only; the private local signing material is not copied into the image.

## Verification Commands

Commands run for this review:

```powershell
.\gradlew.bat dependencyCheckAnalyze --no-daemon --stacktrace
.\gradlew.bat test --no-daemon --stacktrace
.\gradlew.bat bootJar --no-daemon --stacktrace
git diff --check
```
