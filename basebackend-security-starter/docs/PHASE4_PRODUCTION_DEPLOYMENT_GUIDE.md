# BaseBackend Phase 4 生产环境部署指南

## 概述

本指南详细说明如何在生产环境中部署和配置 BaseBackend Phase 4 安全现代化组件，包括：
- OAuth2 资源服务器
- mTLS 服务间通信
- 零信任安全策略

## 目录

1. [部署前准备](#部署前准备)
2. [OAuth2 资源服务器部署](#oauth2-资源服务器部署)
3. [mTLS 证书管理](#mtls-证书管理)
4. [零信任策略配置](#零信任策略配置)
5. [监控与告警](#监控与告警)
6. [性能优化](#性能优化)
7. [故障排除](#故障排除)
8. [维护操作](#维护操作)

---

## 部署前准备

### 1.1 环境要求

**JDK 版本**
- OpenJDK 17 或更高版本
- 推荐使用 Eclipse Temurin 17

**依赖服务**
- PostgreSQL 14+ (或 MySQL 8+)
- Redis 6.2+ (用于缓存)
- Nacos 2.0+ (配置中心)
- OAuth2 授权服务器 (Keycloak, Auth0, 或自建)

**网络要求**
- 服务间通信端口开放 (8080-9090)
- mTLS 端口配置 (默认 8443)
- 健康检查端口 (8080/actuator)

### 1.2 安全前提

**证书准备**
- 准备 CA 根证书
- 生成服务端证书
- 生成客户端证书
- 配置证书信任链

**密钥管理**
- 使用 HashiCorp Vault 或类似工具管理敏感信息
- 定期轮换 API 密钥和证书
- 启用审计日志

### 1.3 配置检查清单

```bash
# 检查必要配置
✅ application.yml 基本配置
✅ OAuth2 授权服务器连接信息
✅ mTLS 证书文件路径
✅ 数据库连接信息
✅ Redis 连接信息
✅ 日志级别配置
✅ 监控端点配置
```

---

## OAuth2 资源服务器部署

### 2.1 配置 OAuth2 授权服务器

**Keycloak 配置示例**

```yaml
# keycloak realm 配置
realm: basebackend
clients:
  - client-id: basebackend-api
    client-secret: ${OAUTH2_CLIENT_SECRET}
    standard-flow-enabled: true
    implicit-flow-enabled: false
    direct-access-grants-enabled: false
    service-accounts-enabled: true
    public-client: false
    protocol: openid-connect
    attributes:
      post.logout.redirect.uris: "https://api.example.com/*"
      oauth2.device.authorization.grant.enabled: "true"
```

### 2.2 配置 JWT 验证

**application.yml**

```yaml
basebackend:
  security:
    oauth2:
      enabled: true
      jwt:
        jwk-set-uri: ${JWT_JWK_SET_URI}
        issuer-uri: ${JWT_ISSUER_URI}
        audience: ${JWT_AUDIENCE:basebackend-api}
        clock-skew: 300
        cache-jwk-set: true
        jwk-set-cache-ttl: 3600
        jwk-set-cache-size: 5
```

### 2.3 权限策略配置

**生产环境权限配置**

```yaml
basebackend:
  security:
    oauth2:
      resource-server:
        permission-strategy:
          fields-priority:
            - "permissions"
            - "roles"
            - "scopes"
          enable-wildcard-match: true
          wildcard-separator: ":"
          enable-role-inheritance: true
          super-admin-roles:
            - "admin"
            - "super_admin"
        token-validation:
          enable-expiration-check: true
          enable-issuer-check: true
          enable-audience-check: true
          enable-scope-check: true
          enable-revocation-check: true  # 生产环境建议启用
          clock-skew: 300
        cache:
          enabled: true
          expire-time: 1800
          max-size: 10000
          key-prefix: "oauth2:permission:"
```

### 2.4 部署步骤

**1. 配置验证**

```bash
# 验证 JWT 连接
curl -X GET "${JWT_JWK_SET_URI}" \
  -H "Accept: application/json"

# 验证令牌格式
echo $JWT_TOKEN | cut -d. -f2 | base64 -d | jq
```

**2. 启动验证**

```bash
# 启动服务
java -jar basebackend-scheduler-4.0.0.jar \
  --spring.profiles.active=prod \
  --basebackend.security.oauth2.enabled=true

# 检查 OAuth2 配置
curl -X GET "http://localhost:8080/actuator/health" \
  -H "Accept: application/json"
```

**3. 功能测试**

```bash
# 测试受保护端点
curl -X GET "http://localhost:8080/api/secure-endpoint" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Accept: application/json"
```

---

## mTLS 证书管理

### 3.1 证书生成

**1. 生成 CA 根证书**

```bash
# 生成 CA 私钥
openssl genrsa -out ca-key.pem 4096

# 生成 CA 证书
openssl req -new -x509 -days 3650 \
  -key ca-key.pem \
  -out ca-cert.pem \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=CA/CN=BaseBackend Root CA"

# 转换为 JKS 格式
keytool -importkeystore \
  -srckeystore ca-cert.p12 \
  -srcstoretype PKCS12 \
  -destkeystore ca-trust.jks \
  -deststoretype JKS \
  -srcstorepass changeit \
  -deststorepass changeit
```

**2. 生成服务端证书**

```bash
# 生成私钥
openssl genrsa -out server-key.pem 2048

# 生成证书签名请求
openssl req -new \
  -key server-key.pem \
  -out server.csr \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=Server/CN=basebackend-server"

# 使用 CA 签名
openssl x509 -req -in server.csr \
  -CA ca-cert.pem \
  -CAkey ca-key.pem \
  -CAcreateserial \
  -out server-cert.pem \
  -days 365

# 转换为 PKCS12
openssl pkcs12 -export \
  -in server-cert.pem \
  -inkey server-key.pem \
  -out server.p12 \
  -name basebackend-server \
  -passout pass:changeit

# 转换为 JKS
keytool -importkeystore \
  -srckeystore server.p12 \
  -srcstoretype PKCS12 \
  -destkeystore server.jks \
  -deststoretype JKS \
  -srcstorepass changeit \
  -deststorepass changeit
```

**3. 生成客户端证书**

```bash
# 为每个微服务生成独立证书
for service in scheduler user-api system-api; do
  # 生成私钥
  openssl genrsa -out ${service}-key.pem 2048

  # 生成证书签名请求
  openssl req -new \
    -key ${service}-key.pem \
    -out ${service}.csr \
    -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=Client/CN=basebackend-${service}"

  # 使用 CA 签名
  openssl x509 -req -in ${service}.csr \
    -CA ca-cert.pem \
    -CAkey ca-key.pem \
    -CAcreateserial \
    -out ${service}-cert.pem \
    -days 365

  # 转换为 PKCS12
  openssl pkcs12 -export \
    -in ${service}-cert.pem \
    -inkey ${service}-key.pem \
    -out ${service}.p12 \
    -name basebackend-${service} \
    -passout pass:changeit
done
```

### 3.2 配置 mTLS

**application.yml**

```yaml
basebackend:
  security:
    mtls:
      enabled: true
      client:
        key-store-path: /etc/ssl/mtls/client/${SERVICE_NAME}.jks
        key-store-password: ${MTLS_CLIENT_KEYSTORE_PASSWORD}
        key-password: ${MTLS_CLIENT_KEY_PASSWORD}
        key-store-type: JKS
        trust-store-path: /etc/ssl/mtls/ca-trust.jks
        trust-store-password: ${MTLS_CLIENT_TRUSTSTORE_PASSWORD}
        connect-timeout: 10000
        read-timeout: 30000
        generate-self-signed: false  # 生产环境必须为 false
        enable-revocation-check: true
        enable-validity-check: true
        expiry-warning-days: 30
      server:
        enabled: true
        key-store-path: /etc/ssl/mtls/server/server.jks
        key-store-password: ${MTLS_SERVER_KEYSTORE_PASSWORD}
        key-password: ${MTLS_SERVER_KEY_PASSWORD}
        key-store-type: JKS
        common-name: "BaseBackend Server"
        client-auth: REQUIRED
        require-full-chain: true
        allowed-subject-pattern: ".*CN=basebackend-.*"
        allowed-issuer-pattern: ".*CN=BaseBackend Root CA.*"
        enabled-protocols:
          - TLSv1.2
          - TLSv1.3
        enabled-cipher-suites:
          - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
          - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
          - TLS_RSA_WITH_AES_256_GCM_SHA384
          - TLS_RSA_WITH_AES_128_GCM_SHA256
        enable-client-cert-cache: true
        client-cert-cache-size: 1000
        client-cert-cache-timeout: 3600
```

### 3.3 证书分发

**使用 HashiCorp Vault 管理证书**

```bash
# 上传证书到 Vault
vault kv put secret/mtls/certs \
  server_keystore=@/etc/ssl/mtls/server/server.jks \
  client_keystore=@/etc/ssl/mtls/client/scheduler.jks \
  truststore=@/etc/ssl/mtls/ca-trust.jks

# 服务启动时动态加载
java -jar basebackend-scheduler-4.0.0.jar \
  --spring.cloud.vault.enabled=true \
  --spring.cloud.vault.kv.enabled=true \
  --spring.cloud.vault.kv.default-context=secret/mtls/certs
```

### 3.4 证书验证

**自动证书健康检查**

```bash
#!/bin/bash
# check-certificate-health.sh

CERT_FILE="/etc/ssl/mtls/server/server.jks"
TRUST_FILE="/etc/ssl/mtls/ca-trust.jks"
WARNING_DAYS=30

# 检查证书有效期
EXPIRY_DATE=$(keytool -list -v -keystore $CERT_FILE -storepass changeit | grep "Valid from:" | tail -1 | awk '{print $9}')
EXPIRY_TIMESTAMP=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_TIMESTAMP=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( ($EXPIRY_TIMESTAMP - $CURRENT_TIMESTAMP) / 86400 ))

if [ $DAYS_UNTIL_EXPIRY -lt $WARNING_DAYS ]; then
    echo "WARNING: Certificate expires in $DAYS_UNTIL_EXPIRY days"
    # 发送告警
    curl -X POST "https://alerts.example.com/api/cert-expiry" \
      -H "Content-Type: application/json" \
      -d "{\"service\": \"scheduler\", \"days_remaining\": $DAYS_UNTIL_EXPIRY}"
else
    echo "Certificate is valid for $DAYS_UNTIL_EXPIRY more days"
fi
```

---

## 零信任策略配置

### 4.1 设备指纹配置

**生产环境设备指纹配置**

```yaml
basebackend:
  security:
    zerotrust:
      enabled: true
      device:
        enabled: true
        timeout: 30000
        cache-expire-minutes: 60
        enable-hash-verification: true
        enable-persistence: true  # 生产环境建议持久化
        table-name: "device_fingerprints"
      risk:
        threshold: 60
        high-threshold: 80
        max-login-attempts: 5
        account-lock-duration: 30
        real-time-analysis-enabled: true
        behavior-analysis-enabled: true
        network-analysis-enabled: true
        analysis-update-interval: 60
        history-retention-days: 90
        enable-ml-prediction: true  # 生产环境可启用 ML 预测
```

### 4.2 风险评估策略

**行为分析配置**

```yaml
# 风险评分规则
behavior-analysis:
  # 登录时间异常 (工作时间外登录)
  login-time-anomaly:
    risk-score: 30
    threshold-hours: [0-6, 22-24]

  # 登录频率异常
  login-frequency-anomaly:
    risk-score: 40
    threshold: "5 attempts per hour"

  # 地理位置异常
  geo-location-anomaly:
    risk-score: 50
    distance-threshold: "100km"

  # 设备异常
  device-anomaly:
    risk-score: 60
    factors:
      - user-agent-change
      - screen-resolution-change
      - language-change

  # IP 地址异常
  ip-anomaly:
    risk-score: 45
    factors:
      - proxy-detection
      - tor-exit-node
      - datacenter-ip
```

### 4.3 策略执行配置

**策略规则配置**

```yaml
basebackend:
  security:
    zerotrust:
      policy:
        trust-score-threshold: 70
        max-concurrent-sessions: 3
        session-timeout: 30
        real-time-monitoring-enabled: true
        cache-enabled: true
        cache-ttl: 300
        enforce-mode: true  # 生产环境强制执行
        audit-enabled: true
        policy-update-interval: 300
        minimum-access-time: 1
      monitoring:
        enabled: true
        interval-minutes: 15
        real-time-threat-detection: true
        behavior-baseline-monitoring: true
        data-retention-days: 90
        enable-alerts: true
        alert-interval-minutes: 60
        metrics-collection-interval: 30
```

### 4.4 部署步骤

**1. 数据库准备**

```sql
-- 创建设备指纹表
CREATE TABLE IF NOT EXISTS device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    fingerprint_hash VARCHAR(256) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    browser VARCHAR(64),
    os VARCHAR(64),
    device_type VARCHAR(32),
    screen_resolution VARCHAR(32),
    language VARCHAR(16),
    timezone VARCHAR(64),
    trust_score INTEGER DEFAULT 100,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_device (user_id, device_id),
    INDEX idx_user_last_seen (user_id, last_seen),
    INDEX idx_device_hash (fingerprint_hash)
);

-- 创建风险事件表
CREATE TABLE IF NOT EXISTS risk_events (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(128),
    risk_type VARCHAR(64) NOT NULL,
    risk_category VARCHAR(32) NOT NULL,
    risk_score INTEGER NOT NULL,
    severity VARCHAR(16) NOT NULL,
    event_details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_risk (user_id, created_at),
    INDEX idx_risk_type (risk_type),
    INDEX idx_severity (severity)
);

-- 创建策略审计表
CREATE TABLE IF NOT EXISTS policy_audit (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    policy_name VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    trust_score INTEGER,
    risk_score INTEGER,
    session_id VARCHAR(128),
    request_details JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_audit (user_id, created_at),
    INDEX idx_policy (policy_name)
);
```

**2. 启动验证**

```bash
# 启动服务
java -jar basebackend-scheduler-4.0.0.jar \
  --spring.profiles.active=prod \
  --basebackend.security.zerotrust.enabled=true

# 验证设备指纹收集
curl -X GET "http://localhost:8080/actuator/health" \
  -H "Accept: application/json" | jq '.components.zerotrust'

# 检查风险评估状态
curl -X GET "http://localhost:8080/actuator/metrics/zerotrust.risk.score" \
  -H "Accept: application/json"
```

**3. 功能测试**

```bash
# 测试设备指纹收集
curl -X POST "http://localhost:8080/api/test-device-fingerprint" \
  -H "Content-Type: application/json" \
  -d '{
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
    "screenResolution": "1920x1080",
    "language": "zh-CN",
    "timezone": "Asia/Shanghai"
  }'

# 测试风险评估
curl -X GET "http://localhost:8080/actuator/zerotrust/risk/user/{userId}" \
  -H "Accept: application/json"
```

---

## 监控与告警

### 5.1 指标收集

**Prometheus 指标配置**

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'basebackend-security'
    static_configs:
      - targets: ['scheduler:8080', 'user-api:8081', 'system-api:8082']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s
    scrape_timeout: 10s
```

**Grafana 仪表板配置**

```json
{
  "dashboard": {
    "title": "BaseBackend Security Dashboard",
    "panels": [
      {
        "title": "OAuth2 Token 验证成功率",
        "targets": [
          {
            "expr": "rate(oauth2_token_validation_total[5m]) * 100",
            "legendFormat": "成功率"
          }
        ]
      },
      {
        "title": "mTLS 连接状态",
        "targets": [
          {
            "expr": "mtls_connections_active",
            "legendFormat": "活跃连接"
          },
          {
            "expr": "rate(mtls_connections_total[5m])",
            "legendFormat": "新建连接率"
          }
        ]
      },
      {
        "title": "零信任风险评分分布",
        "targets": [
          {
            "expr": "zerotrust_risk_score_bucket",
            "legendFormat": "风险评分 {{le}}"
          }
        ]
      }
    ]
  }
}
```

### 5.2 告警规则

**alertmanager.yml**

```yaml
# alertmanager.yml
route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
  - name: 'web.hook'
    webhook_configs:
      - url: 'http://alerts.example.com/api/alerts'

rules:
  - alert: 'OAuth2TokenValidationHighFailure'
    expr: 'rate(oauth2_token_validation_failed_total[5m]) / rate(oauth2_token_validation_total[5m]) > 0.1'
    for: '2m'
    labels:
      severity: 'warning'
    annotations:
      summary: 'OAuth2 Token 验证失败率过高'
      description: '当前失败率为 {{ $value | humanizePercentage }}'

  - alert: 'MTLSCertificateExpiringSoon'
    expr: 'mtls_certificate_expiry_days < 30'
    for: '0m'
    labels:
      severity: 'critical'
    annotations:
      summary: 'mTLS 证书即将过期'
      description: '证书将在 {{ $value }} 天后过期'

  - alert: 'ZeroTrustHighRiskScore'
    expr: 'zerotrust_risk_score_max > 80'
    for: '1m'
    labels:
      severity: 'warning'
    annotations:
      summary: '检测到高风险用户活动'
      description: '最大风险评分为 {{ $value }}'
```

### 5.3 健康检查

**自定义健康检查端点**

```java
@Component
public class SecurityHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        SecurityHealth health = SecurityHealth.builder()
            .oauth2(checkOAuth2Status())
            .mtls(checkMTlsStatus())
            .zerotrust(checkZeroTrustStatus())
            .build();

        if (health.allComponentsHealthy()) {
            return Health.up()
                .withDetail("oauth2", health.getOAuth2().toString())
                .withDetail("mtls", health.getMtls().toString())
                .withDetail("zerotrust", health.getZerotrust().toString())
                .build();
        } else {
            return Health.down()
                .withDetail("oauth2", health.getOAuth2().toString())
                .withDetail("mtls", health.getMtls().toString())
                .withDetail("zerotrust", health.getZerotrust().toString())
                .build();
        }
    }

    private ComponentStatus checkOAuth2Status() {
        // 检查 JWK Set 加载状态
        // 检查令牌验证成功率
        // 返回组件状态
    }

    private ComponentStatus checkMTlsStatus() {
        // 检查证书有效性
        // 检查连接池状态
        // 返回组件状态
    }

    private ComponentStatus checkZeroTrustStatus() {
        // 检查风险评估引擎状态
        // 检查设备指纹数据库连接
        // 返回组件状态
    }
}
```

---

## 性能优化

### 6.1 OAuth2 优化

**JWK Set 缓存优化**

```yaml
basebackend:
  security:
    oauth2:
      jwt:
        cache-jwk-set: true
        jwk-set-cache-ttl: 3600  # 1小时缓存
        jwk-set-cache-size: 10   # 增加缓存大小
```

**令牌验证优化**

```java
@Configuration
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
public class OAuth2PerformanceConfig {

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return ReactiveJwtDecoders.fromIssuerLocation(issuerUri)
            .cache(Duration.ofMinutes(60))
            .refreshTokenRefreshPolicy(RefreshTokenRefreshPolicy.ONE_SHOT);
    }

    @Bean
    public MeterRegistry meterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
}
```

### 6.2 mTLS 优化

**连接池优化**

```yaml
basebackend:
  security:
    mtls:
      client:
        # 连接池配置
        max-pool-size: 100
        min-idle-connections: 10
        connection-ttl: 60000
        idle-timeout: 300000

        # SSL Session 缓存
        enable-session-cache: true
        session-cache-size: 1000
        session-timeout: 300

        # 证书缓存
        enable-client-cert-cache: true
        client-cert-cache-size: 1000
        client-cert-cache-timeout: 3600
```

### 6.3 零信任优化

**风险评估缓存**

```yaml
basebackend:
  security:
    zerotrust:
      policy:
        cache-enabled: true
        cache-ttl: 300  # 5分钟缓存

      async:
        core-pool-size: 10   # 增加核心线程数
        max-pool-size: 50
        queue-capacity: 500
        thread-name-prefix: "zerotrust-"
```

**Redis 缓存配置**

```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5分钟
      cache-null-values: false
```

---

## 故障排除

### 7.1 OAuth2 问题

**问题 1: JWK Set 加载失败**

```bash
# 检查授权服务器连接
curl -v "${JWT_JWK_SET_URI}"

# 检查网络连通性
telnet auth.example.com 443

# 检查 DNS 解析
nslookup auth.example.com

# 查看日志
grep "JWK Set" /var/log/basebackend/scheduler.log
```

**解决方案**
- 验证授权服务器 URL 配置
- 检查防火墙规则
- 更新证书信任链
- 启用调试日志：`logging.level.com.basebackend.security.oauth2=DEBUG`

**问题 2: 令牌验证失败**

```bash
# 解码 JWT 令牌
echo $JWT_TOKEN | cut -d. -f2 | base64 -d | jq

# 检查令牌过期时间
echo $JWT_TOKEN | cut -d. -f2 | base64 -d | jq -r '.exp'

# 检查当前时间
date +%s
```

**解决方案**
- 检查时钟同步：确保所有服务器时间一致
- 调整 clock-skew 配置
- 检查令牌受众（audience）配置
- 验证授权服务器状态

### 7.2 mTLS 问题

**问题 1: 证书验证失败**

```bash
# 检查证书链
openssl verify -CAfile ca-cert.pem server-cert.pem

# 检查证书有效期
openssl x509 -in server-cert.pem -noout -dates

# 检查证书主题
openssl x509 -in server-cert.pem -noout -subject

# 测试 SSL 连接
openssl s_client -connect scheduler:8443 -cert client-cert.pem -key client-key.pem -CAfile ca-cert.pem
```

**解决方案**
- 验证证书链完整性
- 更新过期证书
- 检查证书主题匹配规则
- 重新生成并安装证书

**问题 2: 客户端认证失败**

```bash
# 检查服务端日志
grep "mTLS" /var/log/basebackend/scheduler.log | grep ERROR

# 验证客户端证书
keytool -list -v -keystore client.jks -storepass changeit

# 测试相互认证
openssl s_server -cert server-cert.pem -key server-key.pem -CAfile ca-cert.pem -verify 5
```

### 7.3 零信任问题

**问题 1: 设备指纹收集失败**

```bash
# 检查数据库连接
psql -h db.example.com -U basebackend -d security_db -c "\dt device_fingerprints"

# 检查设备指纹表数据
SELECT COUNT(*) FROM device_fingerprints WHERE last_seen > NOW() - INTERVAL '1 hour';

# 查看应用日志
grep "device.fingerprint" /var/log/basebackend/scheduler.log
```

**解决方案**
- 验证数据库连接配置
- 检查设备指纹表结构
- 清理过期数据
- 调整缓存配置

**问题 2: 风险评分异常**

```bash
# 查看风险事件
SELECT * FROM risk_events ORDER BY created_at DESC LIMIT 10;

# 查看高风险用户
SELECT user_id, AVG(risk_score) as avg_risk_score
FROM risk_events
WHERE created_at > NOW() - INTERVAL '1 hour'
GROUP BY user_id
HAVING AVG(risk_score) > 70
ORDER BY avg_risk_score DESC;

# 检查风险评估配置
curl -X GET "http://localhost:8080/actuator/configprops" | jq '.basebackend.security.zerotrust'
```

**解决方案**
- 调整风险评分阈值
- 检查风险因子权重
- 清理历史数据
- 更新机器学习模型

---

## 维护操作

### 8.1 定期维护任务

**证书轮换 (每月)**

```bash
#!/bin/bash
# rotate-certificates.sh

BACKUP_DIR="/backup/certificates/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# 备份现有证书
cp /etc/ssl/mtls/server/*.jks $BACKUP_DIR/
cp /etc/ssl/mtls/client/*.jks $BACKUP_DIR/

# 生成新证书
openssl genrsa -out new-server-key.pem 2048
openssl req -new -key new-server-key.pem -out new-server.csr \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=Server/CN=basebackend-server"
openssl x509 -req -in new-server.csr \
  -CA ca-cert.pem -CAkey ca-key.pem \
  -out new-server-cert.pem -days 365

# 验证新证书
openssl verify -CAfile ca-cert.pem new-server-cert.pem

# 如果验证成功，替换旧证书
if [ $? -eq 0 ]; then
    openssl pkcs12 -export -in new-server-cert.pem -inkey new-server-key.pem \
      -out new-server.p12 -name basebackend-server -passout pass:changeit
    keytool -importkeystore -srckeystore new-server.p12 \
      -srcstoretype PKCS12 -destkeystore server.jks \
      -deststoretype JKS -srcstorepass changeit -deststorepass changeit
    echo "证书轮换成功"
else
    echo "证书验证失败，保留原证书"
fi
```

**数据清理 (每周)**

```bash
#!/bin/bash
# cleanup-data.sh

# 清理旧风险事件 (保留90天)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
DELETE FROM risk_events
WHERE created_at < NOW() - INTERVAL '90 days';"

# 清理过期设备指纹 (保留30天)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
DELETE FROM device_fingerprints
WHERE last_seen < NOW() - INTERVAL '30 days';"

# 清理策略审计日志 (保留180天)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
DELETE FROM policy_audit
WHERE created_at < NOW() - INTERVAL '180 days';"

# 重建表索引
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -c "
REINDEX TABLE device_fingerprints;
REINDEX TABLE risk_events;
REINDEX TABLE policy_audit;"
```

**性能监控 (每日)**

```bash
#!/bin/bash
# performance-check.sh

# 检查 OAuth2 令牌验证延迟
LATENCY=$(curl -s -o /dev/null -w "%{time_total}" \
  "http://localhost:8080/actuator/metrics/oauth2.token.validation.latency")

echo "OAuth2 令牌验证延迟: ${LATENCY}s"

# 检查 mTLS 连接数
MTLS_CONNECTIONS=$(curl -s "http://localhost:8080/actuator/metrics/mtls.connections.active" \
  | jq '.measurements[0].value')

echo "mTLS 活跃连接数: ${MTLS_CONNECTIONS}"

# 检查零信任风险评分
MAX_RISK_SCORE=$(curl -s "http://localhost:8080/actuator/metrics/zerotrust.risk.score.max" \
  | jq '.measurements[0].value')

echo "最大风险评分: ${MAX_RISK_SCORE}"

# 检查磁盘空间
DISK_USAGE=$(df -h /var/log/basebackend | tail -1 | awk '{print $5}' | cut -d'%' -f1)
if [ $DISK_USAGE -gt 80 ]; then
    echo "WARNING: 磁盘使用率超过80%: ${DISK_USAGE}%"
fi
```

### 8.2 应急响应

**证书过期应急处理**

```bash
#!/bin/bash
# emergency-cert-renewal.sh

echo "开始紧急证书更新流程..."

# 1. 生成临时自签名证书
openssl genrsa -out temp-key.pem 2048
openssl req -new -x509 -key temp-key.pem -out temp-cert.pem -days 7 \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=Emergency/CN=temp-cert"

# 2. 更新临时证书
openssl pkcs12 -export -in temp-cert.pem -inkey temp-key.pem \
  -out temp-cert.p12 -name temp-cert -passout pass:temp123
keytool -importkeystore -srckeystore temp-cert.p12 \
  -srcstoretype PKCS12 -destkeystore server.jks \
  -deststoretype JKS -srcstorepass temp123 -deststorepass changeit

# 3. 重启服务
systemctl restart basebackend-scheduler

# 4. 发送告警
curl -X POST "https://alerts.example.com/api/emergency" \
  -H "Content-Type: application/json" \
  -d '{"type": "cert_emergency", "service": "scheduler", "timestamp": "'$(date -Iseconds)'"}'

echo "紧急证书更新完成，请尽快安装正式证书"
```

**OAuth2 授权服务器故障**

```bash
#!/bin/bash
# oauth2-fallback.sh

echo "OAuth2 授权服务器故障，切换到降级模式..."

# 1. 启用本地令牌验证
java -jar basebackend-scheduler-4.0.0.jar \
  --basebackend.security.oauth2.fallback-mode=true \
  --basebackend.security.oauth2.cache-only=true

# 2. 启用告警
curl -X POST "https://alerts.example.com/api/emergency" \
  -H "Content-Type: application/json" \
  -d '{"type": "oauth2_downgrade", "service": "scheduler", "timestamp": "'$(date -Iseconds)'"}'

echo "已切换到降级模式，仅使用缓存令牌验证"
```

---

## 总结

本指南提供了 Phase 4 安全现代化组件的完整生产部署流程。关键要点：

1. **安全优先**: 生产环境必须使用正式的 CA 证书，禁用自签名证书
2. **监控完善**: 配置全面的指标收集和告警机制
3. **定期维护**: 建立证书轮换、数据清理、性能监控的定期任务
4. **应急准备**: 制定应急响应预案，确保业务连续性

遵循本指南可以确保 BaseBackend Phase 4 安全组件在生产环境中稳定、安全、高效运行。

---

**文档版本**: 1.0.0
**最后更新**: 2025-11-26
**维护人员**: Claude Code (浮浮酱)
