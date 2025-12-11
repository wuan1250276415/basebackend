# BaseBackend Phase 4 故障排除快速参考

## 概述

本指南提供 Phase 4 安全组件的常见问题快速诊断和解决方案。

## 目录

1. [OAuth2 问题](#oauth2-问题)
2. [mTLS 问题](#mtls-问题)
3. [零信任问题](#零信任问题)
4. [性能问题](#性能问题)
5. [网络问题](#网络问题)
6. [日志分析](#日志分析)

---

## OAuth2 问题

### 问题 1: JWK Set 加载失败

**症状**
- 日志: `Failed to load JWK Set from`
- HTTP 状态码: 401 Unauthorized
- 令牌验证失败

**诊断命令**

```bash
# 检查授权服务器连接
curl -v "${JWT_JWK_SET_URI}" 2>&1 | grep -E "(HTTP|SSL)"

# 检查 DNS 解析
nslookup $(echo ${JWT_JWK_SET_URI} | awk -F'/' '{print $3}')

# 检查证书
openssl s_client -connect $(echo ${JWT_JWK_SET_URI} | awk -F'/' '{print $3}') -showcerts

# 验证 JWT 令牌格式
echo ${JWT_TOKEN} | cut -d. -f2 | base64 -d | jq .
```

**解决方案**

```bash
# 1. 更新授权服务器 URL
export JWT_JWK_SET_URI="https://auth.example.com/oauth2/jwks"
export JWT_ISSUER_URI="https://auth.example.com"

# 2. 检查网络连通性
ping auth.example.com

# 3. 检查防火墙
iptables -L | grep 443

# 4. 重新加载配置
curl -X POST "http://localhost:8080/actuator/refresh"
```

### 问题 2: 令牌受众验证失败

**症状**
- 日志: `The aud claim is not valid`
- 错误: `Invalid audience`

**诊断命令**

```bash
# 解码 JWT 并检查受众
echo ${JWT_TOKEN} | cut -d. -f2 | base64 -d | jq -r '.aud'

# 验证令牌过期时间
echo ${JWT_TOKEN} | cut -d. -f2 | base64 -d | jq -r '.exp'

# 检查当前时间戳
date +%s
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    oauth2:
      jwt:
        audience: "basebackend-api"  # 确保与令牌受众一致
        clock-skew: 300  # 增加时钟偏差容忍
```

### 问题 3: 权限解析失败

**症状**
- 日志: `No permissions found in token`
- 用户无法访问受保护资源

**诊断命令**

```bash
# 检查令牌权限字段
echo ${JWT_TOKEN} | cut -d. -f2 | base64 -d | jq -r '.permissions // .roles // .scopes'

# 查看权限策略配置
curl -X GET "http://localhost:8080/actuator/configprops" | jq '.basebackend.security.oauth2'
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    oauth2:
      resource-server:
        permission-strategy:
          fields-priority: ["permissions", "roles", "scopes", "authorities"]
          enable-wildcard-match: true
          wildcard-separator: ":"
```

---

## mTLS 问题

### 问题 1: 证书验证失败

**症状**
- 日志: `Certificate verification failed`
- 错误: `javax.net.ssl.SSLHandshakeException: PKIX path validation failed`

**诊断命令**

```bash
# 检查证书链完整性
openssl verify -CAfile ca-cert.pem server-cert.pem

# 检查证书有效期
openssl x509 -in server-cert.pem -noout -dates

# 检查证书主题和颁发者
openssl x509 -in server-cert.pem -noout -subject -issuer

# 测试 SSL 连接
openssl s_client -connect scheduler:8443 -cert client-cert.pem -key client-key.pem -CAfile ca-cert.pem

# 检查证书密码
keytool -list -keystore server.jks -storepass ${KEYSTORE_PASSWORD} -v
```

**解决方案**

```bash
# 1. 更新 CA 证书
openssl x509 -in ca-cert.pem -outform PEM -out ca-cert-new.pem
keytool -importcert -alias ca -file ca-cert-new.pem -keystore ca-trust.jks -storepass changeit -noprompt

# 2. 重新生成过期证书
openssl genrsa -out new-key.pem 2048
openssl req -new -key new-key.pem -out new.csr \
  -subj "/C=CN/ST=Beijing/L=Beijing/O=BaseBackend/OU=Server/CN=basebackend-server"
openssl x509 -req -in new.csr -CA ca-cert.pem -CAkey ca-key.pem \
  -out new-cert.pem -days 365

# 3. 转换为 JKS
openssl pkcs12 -export -in new-cert.pem -inkey new-key.pem \
  -out new.p12 -name basebackend-server -passout pass:changeit
keytool -importkeystore -srckeystore new.p12 -srcstoretype PKCS12 \
  -destkeystore server.jks -deststoretype JKS -srcstorepass changeit -deststorepass changeit

# 4. 重启服务
systemctl restart basebackend-scheduler
```

### 问题 2: 客户端认证被拒绝

**症状**
- 日志: `Client certificate required`
- 错误: `403 Forbidden`

**诊断命令**

```bash
# 检查客户端证书
keytool -list -keystore client.jks -storepass changeit -v

# 检查证书主题模式匹配
openssl x509 -in client-cert.pem -noout -subject

# 测试相互认证
openssl s_server -cert server-cert.pem -key server-key.pem \
  -CAfile ca-cert.pem -verify 5 -verify_return_error

# 启用调试模式
export javax.net.debug=ssl:handshake:verbose
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    mtls:
      server:
        allowed-subject-pattern: ".*CN=basebackend-.*"  # 更新模式匹配
        allowed-issuer-pattern: ".*CN=BaseBackend Root CA.*"
        client-auth: REQUIRED
        require-full-chain: true
```

### 问题 3: SSL 会话问题

**症状**
- 连接建立缓慢
- 日志: `New session created`
- 错误: `SSL session timeout`

**诊断命令**

```bash
# 检查 SSL 会话缓存
keytool -list -keystore server.jks -storepass changeit | grep -i session

# 测试 SSL 握手时间
time openssl s_client -connect scheduler:8443 -CAfile ca-cert.pem < /dev/null

# 检查 SSL 配置
curl -v -k https://scheduler:8443/health 2>&1 | grep -E "(SSL|cipher)"
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    mtls:
      global:
        enable-session-cache: true
        session-cache-size: 1000
        session-timeout: 300
      client:
        connect-timeout: 10000
        read-timeout: 30000
```

---

## 零信任问题

### 问题 1: 设备指纹收集失败

**症状**
- 日志: `Failed to collect device fingerprint`
- 设备指纹表为空
- 风险评估不准确

**诊断命令**

```bash
# 检查数据库连接
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "SELECT version();"

# 检查设备指纹表
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "\dt device_fingerprints"

# 查看最近指纹收集
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
SELECT COUNT(*) as total_fingerprints,
       COUNT(DISTINCT user_id) as unique_users,
       MAX(last_seen) as last_collection
FROM device_fingerprints;"

# 检查 Redis 连接
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    zerotrust:
      device:
        enabled: true
        timeout: 30000
        enable-persistence: true  # 启用持久化
        table-name: "device_fingerprints"
      async:
        core-pool-size: 10
        max-pool-size: 50
```

```bash
# 重新初始化表
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
DROP TABLE IF EXISTS device_fingerprints;
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    fingerprint_hash VARCHAR(256) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);"
```

### 问题 2: 风险评分异常

**症状**
- 风险评分一直为 0 或 100
- 误报率高
- 高风险用户未被识别

**诊断命令**

```bash
# 查看风险事件分布
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
SELECT risk_type, COUNT(*), AVG(risk_score)
FROM risk_events
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY risk_type
ORDER BY AVG(risk_score) DESC;"

# 查看高风险用户
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
SELECT user_id, COUNT(*) as event_count, MAX(risk_score) as max_score
FROM risk_events
WHERE created_at > NOW() - INTERVAL '24 hours'
GROUP BY user_id
HAVING MAX(risk_score) > 70
ORDER BY max_score DESC;"

# 检查风险评估配置
curl -X GET "http://localhost:8080/actuator/configprops" | \
  jq '.basebackend.security.zerotrust.risk'
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    zerotrust:
      risk:
        threshold: 60          # 调整风险阈值
        high-threshold: 80     # 调整高风险阈值
        max-login-attempts: 5
        account-lock-duration: 30
        real-time-analysis-enabled: true
        behavior-analysis-enabled: true
        network-analysis-enabled: true
```

```bash
# 清理异常数据
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
DELETE FROM risk_events
WHERE risk_score < 0 OR risk_score > 100 OR created_at < NOW() - INTERVAL '180 days';"
```

### 问题 3: 策略执行失败

**症状**
- 用户被错误拒绝访问
- 策略缓存未更新
- 会话管理异常

**诊断命令**

```bash
# 查看策略审计日志
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
SELECT * FROM policy_audit
WHERE created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC
LIMIT 20;"

# 检查策略缓存
curl -X GET "http://localhost:8080/actuator/metrics/zerotrust.policy.cache.hit"

# 查看会话状态
curl -X GET "http://localhost:8080/actuator/metrics/zerotrust.sessions.active"
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    zerotrust:
      policy:
        trust-score-threshold: 70
        max-concurrent-sessions: 3
        session-timeout: 30
        cache-enabled: true
        cache-ttl: 300
        enforce-mode: false  # 临时禁用强制模式进行测试
```

```bash
# 清空策略缓存
curl -X POST "http://localhost:8080/actuator/zerotrust/policy/cache/clear"

# 重启服务
systemctl restart basebackend-scheduler
```

---

## 性能问题

### 问题 1: OAuth2 令牌验证延迟

**症状**
- API 响应时间 > 500ms
- CPU 使用率过高
- JWK Set 频繁重新加载

**诊断命令**

```bash
# 检查令牌验证延迟
curl -w "@curl-format.txt" -o /dev/null -s "http://localhost:8080/api/test" \
  -H "Authorization: Bearer ${JWT_TOKEN}"

# 查看 JWK Set 缓存状态
curl -X GET "http://localhost:8080/actuator/metrics/oauth2.jwk.cache.size"

# 检查网络延迟到授权服务器
ping auth.example.com
traceroute auth.example.com
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    oauth2:
      jwt:
        cache-jwk-set: true
        jwk-set-cache-ttl: 3600  # 增加缓存时间
        jwk-set-cache-size: 10   # 增加缓存大小
        clock-skew: 300
```

```bash
# 调整 JVM 参数
java -jar basebackend-scheduler-4.0.0.jar \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UseCGroupMemoryLimitForHeap
```

### 问题 2: mTLS 连接池耗尽

**症状**
- 连接超时错误
- "Connection pool exhausted"
- 服务间调用失败

**诊断命令**

```bash
# 检查连接池状态
curl -X GET "http://localhost:8080/actuator/metrics/mtls.connection.pool.size"

# 查看活跃连接数
curl -X GET "http://localhost:8080/actuator/metrics/mtls.connections.active"

# 监控连接创建速率
curl -X GET "http://localhost:8080/actuator/prometheus" | \
  grep "mtls_connections_total"
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    mtls:
      client:
        max-pool-size: 100      # 增加连接池大小
        min-idle-connections: 20
        connection-ttl: 60000
        idle-timeout: 300000
        enable-session-cache: true
```

```bash
# 调整连接超时
curl -X POST "http://localhost:8080/actuator/zerotrust/pool/reset"

# 重启服务清理连接池
systemctl restart basebackend-scheduler
```

### 问题 3: 零信任风险评估慢

**症状**
- 风险评估延迟 > 1s
- 异步任务队列堆积
- CPU 使用率飙升

**诊断命令**

```bash
# 检查异步任务队列
curl -X GET "http://localhost:8080/actuator/metrics/zerotrust.async.queue.size"

# 查看任务执行时间
curl -X GET "http://localhost:8080/actuator/metrics/zerotrust.async.task.duration"

# 检查数据库查询性能
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "
EXPLAIN ANALYZE
SELECT * FROM risk_events
WHERE created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC
LIMIT 100;"
```

**解决方案**

```yaml
# application.yml
basebackend:
  security:
    zerotrust:
      async:
        core-pool-size: 15      # 增加核心线程数
        max-pool-size: 50
        queue-capacity: 1000
        task-timeout-seconds: 300
      policy:
        cache-enabled: true
        cache-ttl: 600          # 增加缓存时间
```

```sql
-- 优化数据库索引
CREATE INDEX CONCURRENTLY idx_risk_events_created_at
ON risk_events(created_at DESC);

CREATE INDEX CONCURRENTLY idx_device_fingerprints_user_device
ON device_fingerprints(user_id, device_id);

ANALYZE risk_events;
ANALYZE device_fingerprints;
```

---

## 网络问题

### 问题 1: 服务间通信失败

**症状**
- Connection refused
- SSL handshake timeout
- Network unreachable

**诊断命令**

```bash
# 检查服务状态
systemctl status basebackend-scheduler
systemctl status basebackend-user-api

# 检查端口监听
netstat -tlnp | grep 8443
netstat -tlnp | grep 8080

# 测试网络连通性
telnet scheduler 8443
ping scheduler

# 检查防火墙规则
iptables -L -n
firewall-cmd --list-all
```

**解决方案**

```bash
# 1. 开放必要端口
firewall-cmd --permanent --add-port=8080/tcp
firewall-cmd --permanent --add-port=8443/tcp
firewall-cmd --reload

# 2. 检查 DNS 配置
echo "nameserver 8.8.8.8" >> /etc/resolv.conf

# 3. 验证路由
ip route show

# 4. 重启网络服务
systemctl restart network
```

### 问题 2: 负载均衡器配置错误

**症状**
- 502 Bad Gateway
- 健康检查失败
- 请求间歇性失败

**诊断命令**

```bash
# 检查负载均衡器配置
cat /etc/nginx/conf.d/basebackend.conf

# 测试后端服务健康状态
curl -k -H "Host: scheduler.example.com" \
  https://localhost:8443/actuator/health

# 检查 SSL 证书
openssl x509 -in /etc/ssl/certs/server.crt -noout -text

# 查看负载均衡器日志
tail -f /var/log/nginx/error.log
```

**解决方案**

```nginx
# nginx.conf
upstream basebackend_scheduler {
    server scheduler1:8443 max_fails=3 fail_timeout=30s;
    server scheduler2:8443 max_fails=3 fail_timeout=30s;
    keepalive 32;
}

server {
    listen 443 ssl http2;
    server_name scheduler.example.com;

    ssl_certificate /etc/ssl/certs/server.crt;
    ssl_certificate_key /etc/ssl/private/server.key;

    location / {
        proxy_pass https://basebackend_scheduler;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_ssl_verify on;
        proxy_ssl_trusted_certificate /etc/ssl/certs/ca-cert.pem;
    }
}
```

---

## 日志分析

### 关键日志模式

**OAuth2 错误**

```bash
# 查找 OAuth2 相关错误
grep -E "oauth2|OAuth2|JWT" /var/log/basebackend/scheduler.log | \
  grep -E "ERROR|FAIL|EXCEPTION"

# 统计错误类型
grep -E "oauth2.*ERROR" /var/log/basebackend/scheduler.log | \
  awk '{print $NF}' | sort | uniq -c | sort -nr

# 查看详细错误
grep -A 10 "JWK Set" /var/log/basebackend/scheduler.log | tail -20
```

**mTLS 错误**

```bash
# 查找 mTLS 相关错误
grep -E "mtls|mTLS|SSL|TLS|Certificate" /var/log/basebackend/scheduler.log | \
  grep -E "ERROR|FAIL|EXCEPTION"

# 检查证书相关日志
grep -E "Certificate|Handshake" /var/log/basebackend/scheduler.log | tail -50

# 分析 SSL 握手失败
grep "SSLHandshakeException" /var/log/basebackend/scheduler.log | \
  awk '{print $4}' | sort | uniq -c
```

**零信任错误**

```bash
# 查找零信任相关错误
grep -E "zerotrust|ZeroTrust|Device|Risk" /var/log/basebackend/scheduler.log | \
  grep -E "ERROR|FAIL|EXCEPTION"

# 查看风险评估日志
grep "RiskAssessmentEngine" /var/log/basebackend/scheduler.log | tail -50

# 分析设备指纹收集
grep "DeviceFingerprint" /var/log/basebackend/scheduler.log | \
  grep -E "COLLECT|FAILED|SUCCESS"
```

### 日志级别配置

```yaml
# logback-spring.xml
<configuration>
    <!-- 安全相关组件使用 DEBUG 级别 -->
    <logger name="com.basebackend.security.oauth2" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <logger name="com.basebackend.security.mtls" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <logger name="com.basebackend.security.zerotrust" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <!-- SSL 调试 (仅在故障排除时启用) -->
    <logger name="javax.net.ssl" level="DEBUG" additivity="false">
        <appender-ref ref="SSL-FILE"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### ELK Stack 配置

**Logstash 配置**

```ruby
# logstash.conf
input {
  file {
    path => "/var/log/basebackend/scheduler.log"
    start_position => "beginning"
    sincedb_path => "/dev/null"
  }
}

filter {
  if [path] =~ "scheduler" {
    grok {
      match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} \[%{DATA:thread}\] %{LOGLEVEL:level} %{DATA:logger} \[%{DATA:userId},%{DATA:sessionId},%{DATA:deviceId}\] - %{GREEDYDATA:message}" }
    }

    # 提取安全事件
    if [message] =~ /OAuth2|mTLS|ZeroTrust/ {
      mutate { add_tag => ["security"] }
    }

    # 提取错误信息
    if [level] == "ERROR" {
      mutate { add_tag => ["error"] }
    }

    # 解析异常堆栈
    if [message] =~ /Exception/ {
      grok {
        match => { "message" => "(?m)^%{GREEDYDATA:exception}" }
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "basebackend-security-%{+YYYY.MM.dd}"
  }

  stdout { codec => rubydebug }
}
```

**Kibana 仪表板**

```json
{
  "version": "7.10.0",
  "objects": [
    {
      "attributes": {
        "title": "BaseBackend Security Dashboard",
        "type": "dashboard"
      },
      "visState": {
        "type": "dashboard"
      },
      "uiStateJSON": "{}",
      "kibanaSavedObjectMeta": {
        "searchSourceJSON": "{}"
      }
    }
  ]
}
```

---

## 快速诊断脚本

**一键健康检查**

```bash
#!/bin/bash
# health-check.sh

echo "=== BaseBackend Security Health Check ==="
echo "时间: $(date)"
echo ""

# 检查服务状态
echo "1. 检查服务状态"
systemctl is-active basebackend-scheduler
echo ""

# 检查端口监听
echo "2. 检查端口监听"
netstat -tlnp | grep -E "8080|8443"
echo ""

# 检查 OAuth2 连接
echo "3. 检查 OAuth2 连接"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  "http://localhost:8080/actuator/health"
echo ""

# 检查证书有效期
echo "4. 检查证书有效期"
EXPIRY=$(keytool -list -v -keystore /etc/ssl/mtls/server/server.jks \
  -storepass changeit 2>/dev/null | grep "Valid from:" | tail -1)
echo "证书有效期: $EXPIRY"
echo ""

# 检查数据库连接
echo "5. 检查数据库连接"
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME} -c "SELECT 1;" \
  2>/dev/null && echo "数据库连接正常" || echo "数据库连接失败"
echo ""

# 检查 Redis 连接
echo "6. 检查 Redis 连接"
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping \
  2>/dev/null && echo "Redis 连接正常" || echo "Redis 连接失败"
echo ""

# 检查日志错误
echo "7. 检查最近错误"
ERROR_COUNT=$(grep -c "ERROR" /var/log/basebackend/scheduler.log \
  | tail -100)
echo "最近 100 行日志中 ERROR 数量: $ERROR_COUNT"
echo ""

echo "=== 健康检查完成 ==="
```

---

## 总结

本快速参考指南涵盖了 Phase 4 安全组件的常见问题和解决方案。关键要点：

1. **快速诊断**: 使用提供的诊断命令快速定位问题
2. **日志分析**: 通过日志模式快速识别错误类型
3. **配置优化**: 调整配置参数解决性能问题
4. **监控完善**: 建立完善的监控和告警机制

遇到问题时，首先查看日志，使用诊断命令定位根本原因，然后根据本指南提供的解决方案进行修复。

---

**文档版本**: 1.0.0
**最后更新**: 2025-11-26
**维护人员**: Claude Code (浮浮酱)
