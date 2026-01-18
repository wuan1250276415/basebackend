# Quality and Test Summary

## Sources
- `docs/TESTING_SUMMARY.md`
- `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md`
- `docs/P1_CODE_QUALITY_IMPROVEMENT_REPORT.md`
- `docs/P2_CODE_QUALITY_IMPROVEMENT_REPORT.md`
- `sonar-project.properties`

## Current status (from reports)
- Full project test status summary indicates most core modules are at or near 100% pass rate; scheduler is reported at 88.7% and cache at 98.1%.
- TESTING_SUMMARY documents a focused set of 39 passing tests related to audit logging and associated services.

## Gaps / risks
- Scheduler and cache modules are not at 100% pass rate per `FULL_PROJECT_TEST_STATUS_SUMMARY.md`.
- Sonar coverage threshold is set to 30; this is a low bar and may not detect coverage regressions for new code.
- Frontend assets and non-Java sources are excluded from Sonar via `sonar.exclusions`, limiting cross-stack quality signals.

## Regression matrix
| Area | Evidence / report | Regression focus | Suggested command |
| --- | --- | --- | --- |
| Database | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | CRUD, migration, data integrity | `mvn -pl basebackend-database test` |
| Cache | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | Redis ops, lock utilities, cache TTL | `mvn -pl basebackend-cache test` |
| Security | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | JWT, auth filters, baseline config | `mvn -pl basebackend-security test` |
| Gateway | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | Auth filter, whitelist rules | `mvn -pl basebackend-gateway test` |
| Messaging | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | Producer/consumer, retry, compensation | `mvn -pl basebackend-messaging test` |
| File service | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | Upload/download, storage adapters | `mvn -pl basebackend-file-service test` |
| Scheduler | `docs/FULL_PROJECT_TEST_STATUS_SUMMARY.md` | Workflow scheduling, retries | `mvn -pl basebackend-scheduler-parent test` |
| Audit logging | `docs/TESTING_SUMMARY.md` | OperationLog/AuditLog tests | `mvn -pl basebackend-logging test` |
| Sonar quality gate | `sonar-project.properties` | Code smell/vulnerability/coverage thresholds | `mvn sonar:sonar` |

## Notes
- Adjust regression scope based on modules touched in the current change set.
