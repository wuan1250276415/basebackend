# Security Review (JWT, Gateway, Sensitive Config)

## Sources
- `config/nacos-configs/dev/security-config.yml`
- `config/security-config.yml`
- `basebackend-gateway/src/main/resources/application.yml`
- `basebackend-gateway/src/main/java/com/basebackend/gateway/config/GatewaySecurityProperties.java`
- `basebackend-gateway/src/main/java/com/basebackend/gateway/filter/AuthenticationFilter.java`
- `basebackend-gateway/src/main/java/com/basebackend/gateway/filter/SignatureVerifyFilter.java`
- `config/nacos-configs/gateway-config.yml`

## Findings
1. JWT secret defaults are present in config
   - `config/nacos-configs/dev/security-config.yml` defines `JWT_SECRET` with a default value.
   - Risk: default secrets can leak into non-dev environments if env overrides are missing.

2. Token expiry values are explicitly set
   - Access token: 24h; refresh token: 7d in `security-config.yml`.
   - Ensure values match security requirements and are consistent across services.

3. Gateway whitelist and actuator whitelist are configurable
   - `application.yml` defines `gateway.security.whitelist` and `actuator-whitelist` with explicit notes to avoid `/actuator/**`.
   - Authentication filter enforces JWT + Redis session check and supports whitelist paths.

4. Gateway signature verification has default secret values
   - `SignatureVerifyFilter` uses `gateway.security.signature.secret-key` with a default fallback.
   - Risk: default secret should be replaced by a managed secret in non-dev environments.

5. CORS vs baseline origin allowlist mismatch
   - `config/security-config.yml` defines `security.baseline.allowed-origins` (restricted list).
   - `config/nacos-configs/gateway-config.yml` sets CORS `allowed-origins: "*"`.
   - Potential mismatch between gateway CORS policy and baseline security expectations.

6. Plaintext Redis password in gateway config
   - `config/nacos-configs/gateway-config.yml` contains `spring.data.redis.password` with a concrete value.
   - Risk: plaintext secrets in config; should be moved to env/secret manager.

## Recommendations
- Enforce `JWT_SECRET` via environment/secret manager and rotate non-dev values.
- Align gateway CORS settings with `security.baseline.allowed-origins`.
- Replace signature default secret with a managed secret and verify enablement in production.
- Remove plaintext Redis password from config; use env vars or secret store.
