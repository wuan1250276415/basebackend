# Action Backlog (Review Conclusions)

## Scope
Backlog items derived from review artifacts in `docs/review/`.

## Prioritized backlog
| ID | Priority | Module | Action | Acceptance criteria | Verification plan |
| --- | --- | --- | --- | --- | --- |
| ACT-001 | P0 | security/config | Enforce `JWT_SECRET` via env/secret manager and rotate defaults | `JWT_SECRET` has no default fallback in runtime config; production secrets stored outside repo | Verify config templates and runtime env usage; check effective config via `/actuator/configprops` |
| ACT-002 | P0 | gateway/config | Remove plaintext Redis password from gateway config | `spring.data.redis.password` no longer hard-coded in `config/nacos-configs/gateway-config.yml` | Confirm env/secret reference in config and redeploy |
| ACT-003 | P1 | gateway/api | Align gateway response wrapper with common `Result` | Gateway responses use common `Result` or documented mapping layer | Verify API response schema across gateway and services |
| ACT-004 | P1 | gateway/security | Align CORS with baseline allowlist | Gateway CORS config matches `security.baseline.allowed-origins` for non-dev environments | Validate allowed origins in Nacos config and smoke-test browser requests |
| ACT-005 | P1 | messaging | Define topic naming and payload schema/versioning | Messaging doc includes topic conventions and schema versioning rules | Review doc and sample publisher/consumer contracts |
| ACT-006 | P1 | database | Document schema ownership and migration governance | Database doc includes ownership and approval workflow for migrations | Review doc and confirm with release checklist |
| ACT-007 | P2 | file-service | Document storage adapter timeouts/retry defaults and API error contract | File-service docs list adapter timeout/retry defaults and error codes | Verify docs align with implementation and configs |
| ACT-008 | P2 | config/infra | Standardize Nacos config directory usage | One authoritative Nacos config directory and upload flow documented | Verify scripts and README reference the same source |
| ACT-009 | P2 | quality | Raise Sonar coverage threshold and track deltas | `sonar.coverageThreshold` updated and tracked in CI | Run `mvn sonar:sonar` and confirm quality gate |

## Sources
- `docs/review/core-module-consistency.md`
- `docs/review/config-infra-review.md`
- `docs/review/security-review.md`
- `docs/review/data-integration-review.md`
- `docs/review/quality-test-summary.md`
