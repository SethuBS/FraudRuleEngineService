# Final Cleanup And Release Review

Card: `R.05.1`
Due: 18 June 2026
Priority: `P0 Critical`
Labels: `Release`, `DevOps`, `Security`, `Testing`, `Documentation`, `Backend`

This document records the final cleanup and submission-snapshot review for `FraudRuleEngineService`.

## Cleanup Results

| Area                       | Result                                                                                                                                                                                                                                               |
|----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Package-info files         | Removed all `package-info.java` files from `src/main/java`.                                                                                                                                                                                          |
| Generated artifacts        | Removed an accidental JVM heap dump from the workspace and added `*.hprof` to `.gitignore`.                                                                                                                                                          |
| Java warnings              | `.\gradlew.bat clean build --warning-mode all` passed without avoidable project warnings.                                                                                                                                                            |
| Test output noise          | Test JVM class-data-sharing warning was removed by configurable `TEST_JVM_ARGS` / `-Ptest.jvm-args`, defaulting to `-Xshare:off`. Test root logging defaults to configurable `WARN`.                                                                 |
| Dependency Check runtime   | Dependency Check data defaults to ignored `.gradle/dependency-check-data` so `clean` does not force a full NVD refresh.                                                                                                                              |
| Dependency Check analyzers | RetireJS and OSS Index are configurable and disabled by default for this Java/Spring service. NVD-backed dependency analysis remains enabled.                                                                                                        |
| Secrets scan               | Reviewed password, token, API key, private-key, authorization-header, local-path, and localhost matches. Findings are documented local reviewer material, test fixtures, configurable defaults, or local examples; no production secrets were found. |
| API boundary               | Controllers return API DTOs, not JPA entities. Persistence remains isolated under infrastructure packages.                                                                                                                                           |
| Money handling             | Main code uses `BigDecimal` for monetary amounts; no `double` or `float` money handling was found.                                                                                                                                                   |
| Rule design                | Fraud rules continue to implement the shared `FraudRule` strategy contract. New rules do not require HTTP contract changes.                                                                                                                          |

## Warning Notes

Dependency Check `12.2.2` emits a Gradle deprecation warning when forced with `--warning-mode all`:

```text
Invocation of Task.project at execution time has been deprecated.
```

This warning is emitted from the third-party Dependency Check Gradle task during `dependencyCheckAnalyze`; it is not caused by project build script logic. The task still completes and generates HTML/JSON reports.

## Verification Commands

These commands were run during this cleanup pass:

```powershell
.\gradlew.bat clean build --warning-mode all
.\gradlew.bat clean test --warning-mode all
.\gradlew.bat bootJar
.\gradlew.bat dependencyCheckAnalyze
docker compose down -v
docker compose up --build -d
docker compose down
```

## Verification Results

| Command                                        | Result                    |
|------------------------------------------------|---------------------------|
| `.\gradlew.bat clean build --warning-mode all` | Passed                    |
| `.\gradlew.bat clean test --warning-mode all`  | Passed                    |
| `.\gradlew.bat bootJar`                        | Passed                    |
| `.\gradlew.bat dependencyCheckAnalyze`         | Passed, 0 vulnerabilities |
| `docker compose down -v`                       | Passed                    |
| `docker compose up --build -d`                 | Passed; PostgreSQL and application containers healthy |
| `GET /actuator/health/readiness`               | Passed; returned `UP`     |
| README curl collection suite                    | Passed                    |
| Prod profile Swagger check                     | Passed; `/swagger-ui/index.html` and `/v3/api-docs` returned `404` |
| `docker compose down`                          | Passed                    |

## Release Snapshot Rule

The `v0.1.0-submission` tag should be recreated only after the final cleanup commit is pushed and the target branch is verified. The tag must point at the reviewed submission commit.
