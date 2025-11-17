# Phase 11+: 安全审计日志实施指南

## 📋 概述

本文档详细描述了BaseBackend项目的安全审计日志实施，包括自动日志记录、事件分类、安全事件分析和告警机制。安全审计日志是监控安全状况、追踪安全事件和满足合规要求的重要手段。

---

## 🎯 实施目标

### 核心目标
1. ✅ 实现自动安全事件记录
2. ✅ 支持多种事件类型（登录、权限、数据访问等）
3. ✅ 集成Kafka异步消息处理
4. ✅ 提供注解式审计配置
5. ✅ 实现事件分级和告警
6. ✅ 支持审计日志查询和分析

### 技术栈
- **消息队列**: Apache Kafka
- **日志存储**: Elasticsearch
- **分析引擎**: Apache Spark
- **告警系统**: Prometheus AlertManager
- **可视化**: Grafana

---

## 🏗️ 架构设计

### 审计架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    安全审计架构                                │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   应用层      │  │   拦截器      │  │   切面       │    │
│  │              │  │              │  │              │    │
│  │ • 手动记录   │  │ • HTTP拦截   │  │ • @Audited   │    │
│  │ • API调用    │  │ • 认证拦截   │  │ • @DataAudit │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼─────────────────┼─────────────────┼─────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Kafka      │  │  存储层       │  │  分析层      │    │
│  │              │  │              │  │              │    │
│  │ • 异步处理   │  │ • Elasticsearch│  │ • 日志查询   │    │
│  │ • 事件分类   │  │ • 索引优化   │  │ • 统计报表   │    │
│  │ • 持久化     │  │ • 数据归档   │  │ • 异常检测   │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
└─────────┼─────────────────┼─────────────────┼─────────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            │
┌─────────────────────────────────────────────────────────────┐
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   可视化     │  │   告警系统     │  │   合规报告    │    │
│  │              │  │              │  │              │    │
│  │ • Grafana   │  │ • Prometheus │  │ • 审计报表   │    │
│  │ • 日志查看   │  │ • 异常告警   │  │ • 符合性检查 │    │
│  │ • 仪表盘     │  │ • 邮件通知   │  │ • 数据导出   │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### 审计流程

#### 1. 事件收集
```
用户操作 -> 拦截器/切面 -> 事件封装 -> 发送到Kafka
```

#### 2. 事件处理
```
Kafka -> 消费者 -> 存储到Elasticsearch -> 索引优化
```

#### 3. 事件分析
```
Elasticsearch -> 日志查询 -> 统计报表 -> 异常检测
```

#### 4. 告警触发
```
异常检测 -> 告警规则 -> AlertManager -> 通知渠道
```

---

## 📦 模块结构

### basebackend-security 模块 - 审计组件
```
basebackend-security/
├── src/main/java/com/basebackend/security/
│   ├── audit/
│   │   ├── SecurityAuditService.java       # 审计服务
│   │   ├── SecurityAuditInterceptor.java   # 拦截器
│   │   ├── SecurityAuditAspect.java        # 切面
│   │   ├── SecurityAuditConfig.java        # 配置
│   │   └── annotations/
│   │       ├── Audited.java                # 审计注解
│   │       ├── DataAudit.java              # 数据审计注解
│   │       ├── PermissionAudit.java        # 权限审计注解
│   │       └── FileAudit.java              # 文件审计注解
│   └── config/
│       └── SecurityAuditConfig.java        # 审计配置
```

---

## 🔧 详细配置

### 1. Kafka配置

#### application.yml中的Kafka配置
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      batch-size: 16384
      buffer-memory: 33554432
    consumer:
      group-id: security-audit-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      enable-auto-commit: false
```

### 2. 安全审计配置

```yaml
# 安全审计配置
security:
  audit:
    # 启用审计
    enabled: true

    # 异步处理
    async:
      enabled: true
      thread-pool-size: 10

    # Kafka配置
    kafka:
      enabled: true
      topic: security-audit
      critical-topic: security-audit-critical
      partition-num: 3
      replication-factor: 1

    # 事件类型
    events:
      # 登录事件
      login:
        enabled: true
        include-user-agent: true
        include-ip-address: true

      # 数据访问事件
      data-access:
        enabled: true
        include-response-time: true
        sensitive-data-alert: true

      # 权限变更事件
      permission-change:
        enabled: true
        alert-on-change: true

      # API调用事件
      api-call:
        enabled: true
        sample-rate: 1.0
        include-headers: false
        max-body-size: 1024

    # 敏感操作
    sensitive-operations:
      - 用户密码修改
      - 权限变更
      - 系统配置修改
      - 敏感数据导出

    # 告警规则
    alert-rules:
      # 多次登录失败
      - name: "multiple-login-failures"
        condition: "count > 5"
        timeframe: "5m"
        severity: "WARNING"

      # 异常数据访问
      - name: "unusual-data-access"
        condition: "volume > 1000"
        timeframe: "1h"
        severity: "WARNING"

      # 敏感权限变更
      - name: "sensitive-permission-change"
        condition: "always"
        severity: "CRITICAL"
```

### 3. Elasticsearch配置

```yaml
# Elasticsearch配置
elasticsearch:
  cluster-name: basebackend-audit
  node-name: audit-node-1
  discovery-zen-ping-unicast-hosts: localhost:9300
  http-port: 9200

  # 索引配置
  index:
    # 每日创建新索引
    rollover: true
    # 索引生命周期策略
    lifecycle-policy: security-audit-policy
    # 索引模板
    template-name: security-audit-template

  # 分片和副本
  number_of_shards: 3
  number_of_replicas: 1

  # TTL配置
  ttl:
    # 普通审计日志7天
    normal: 7d
    # 关键审计日志30天
    critical: 30d
    # 超敏感审计日志90天
    sensitive: 90d
```

---

## 📝 使用示例

### 1. 手动记录审计事件

#### 记录登录事件
```java
@RestController
public class AuthController {

    @Autowired
    private SecurityAuditService auditService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            // 验证用户凭据
            boolean success = authenticateUser(request);

            // 记录登录事件
            auditService.logLogin(
                request.getUsername(),
                success,
                getClientIpAddress(httpRequest),
                httpRequest.getHeader("User-Agent"),
                success ? "登录成功" : "用户名或密码错误"
            );

            if (success) {
                return ResponseEntity.ok(generateToken());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        } catch (Exception e) {
            auditService.logSecurityException(
                "LOGIN_ERROR",
                "登录过程中发生异常: " + e.getMessage(),
                "ERROR",
                Map.of("username", request.getUsername(), "error", e.getMessage())
            );
            throw e;
        }
    }
}
```

#### 记录权限变更事件
```java
@Service
public class PermissionService {

    @Autowired
    private SecurityAuditService auditService;

    @Audited(value = "权限修改", resource = "用户权限")
    public void updateUserPermissions(Long adminUserId, Long targetUserId, List<String> permissions) {
        try {
            // 修改权限
            userPermissionMapper.updatePermissions(targetUserId, permissions);

            // 记录权限变更事件
            auditService.logPermissionChange(
                adminUserId.toString(),
                targetUserId.toString(),
                "MODIFY",
                String.join(",", permissions),
                "用户权限管理"
            );

        } catch (Exception e) {
            auditService.logSecurityException(
                "PERMISSION_CHANGE_ERROR",
                "权限修改失败",
                "ERROR",
                Map.of("adminUserId", adminUserId, "targetUserId", targetUserId, "error", e.getMessage())
            );
            throw e;
        }
    }
}
```

### 2. 使用注解自动审计

#### @Audited注解示例
```java
@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users/{id}")
    @Audited(value = "查询用户", resource = "用户信息", description = "查询用户详细信息")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    @Audited(value = "创建用户", resource = "用户信息", description = "创建新用户")
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User newUser = userService.createUser(user);
        return ResponseEntity.ok(newUser);
    }

    @PutMapping("/users/{id}")
    @Audited(value = "修改用户", resource = "用户信息", description = "修改用户信息")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/users/{id}")
    @Audited(value = "删除用户", resource = "用户信息", description = "删除用户")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### @DataAudit注解示例
```java
@Repository
public class UserMapper {

    @Insert("INSERT INTO users (username, email, phone) VALUES (#{username}, #{email}, #{phone})")
    @DataAudit(operation = "INSERT", table = "users", description = "插入用户记录")
    int insertUser(User user);

    @Update("UPDATE users SET email = #{email}, phone = #{phone} WHERE id = #{id}")
    @DataAudit(operation = "UPDATE", table = "users", description = "更新用户记录")
    int updateUser(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    @DataAudit(operation = "DELETE", table = "users", description = "删除用户记录")
    int deleteUser(Long id);

    @Select("SELECT * FROM users WHERE id = #{id}")
    @DataAudit(operation = "SELECT", table = "users", description = "查询用户记录")
    User selectUser(Long id);
}
```

#### @PermissionAudit注解示例
```java
@Service
public class RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @PermissionAudit(action = "GRANT", target = "role", permission = "角色权限")
    public void grantRole(Long adminUserId, Long userId, String roleName) {
        roleMapper.grantRole(userId, roleName);
        auditService.logPermissionChange(
            adminUserId.toString(),
            userId.toString(),
            "GRANT",
            roleName,
            "角色授权"
        );
    }

    @PermissionAudit(action = "REVOKE", target = "role", permission = "角色权限")
    public void revokeRole(Long adminUserId, Long userId, String roleName) {
        roleMapper.revokeRole(userId, roleName);
        auditService.logPermissionChange(
            adminUserId.toString(),
            userId.toString(),
            "REVOKE",
            roleName,
            "角色撤销"
        );
    }
}
```

#### @FileAudit注解示例
```java
@Service
public class FileService {

    @Autowired
    private FileStorage fileStorage;

    @PostMapping("/files/upload")
    @FileAudit(operation = "UPLOAD", fileType = "document")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileId = fileStorage.store(file);
        return ResponseEntity.ok(fileId);
    }

    @GetMapping("/files/{fileId}/download")
    @FileAudit(operation = "DOWNLOAD", fileType = "document")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileId) {
        byte[] fileData = fileStorage.retrieve(fileId);
        return ResponseEntity.ok(fileData);
    }

    @DeleteMapping("/files/{fileId}")
    @FileAudit(operation = "DELETE", fileType = "document")
    public ResponseEntity<Void> deleteFile(@PathVariable String fileId) {
        fileStorage.delete(fileId);
        return ResponseEntity.noContent().build();
    }
}
```

### 3. 敏感数据访问审计

```java
@Service
public class SensitiveDataService {

    @Autowired
    private SecurityAuditService auditService;

    public User getSensitiveUserInfo(Long userId, String requesterUsername, String requesterIp) {
        User user = userMapper.selectUser(userId);

        // 记录敏感数据访问事件
        auditService.logSensitiveDataAccess(
            requesterUsername,
            "身份证号",
            userId.toString(),
            requesterIp
        );

        return user;
    }

    public void exportUserData(Long userId, String format, String requesterUsername, String requesterIp) {
        // 导出用户数据
        byte[] data = exportUserData(userId, format);

        // 记录导出事件
        auditService.logSensitiveDataAccess(
            requesterUsername,
            "用户数据导出",
            userId.toString() + ":" + format,
            requesterIp
        );

        return data;
    }
}
```

### 4. 系统配置变更审计

```java
@Service
public class SystemConfigService {

    @Autowired
    private SecurityAuditService auditService;

    @Audited(value = "系统配置修改", resource = "系统配置", description = "修改系统配置")
    public void updateConfig(String configKey, String oldValue, String newValue, String adminUsername, String ipAddress) {
        try {
            configMapper.updateConfig(configKey, newValue);

            // 记录配置变更事件
            auditService.logConfigChange(
                adminUsername,
                configKey,
                oldValue,
                newValue,
                ipAddress
            );

        } catch (Exception e) {
            auditService.logSecurityException(
                "CONFIG_CHANGE_ERROR",
                "系统配置修改失败",
                "CRITICAL",
                Map.of("configKey", configKey, "error", e.getMessage())
            );
            throw e;
        }
    }
}
```

---

## 🔍 审计日志查询

### 1. 使用Elasticsearch查询

#### 查询登录失败事件
```bash
curl -X GET "localhost:9200/security-audit-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {"term": {"eventType": "LOGIN"}},
        {"term": {"success": false}}
      ]
    }
  },
  "sort": [
    {"timestamp": {"order": "desc"}}
  ],
  "size": 100
}'
```

#### 查询敏感数据访问事件
```bash
curl -X GET "localhost:9200/security-audit-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "term": {"eventType": "SENSITIVE_DATA_ACCESS"}
  },
  "aggs": {
    "by_user": {
      "terms": {"field": "username"}
    }
  }
}'
```

#### 查询API调用统计
```bash
curl -X GET "localhost:9200/security-audit-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "range": {
      "timestamp": {
        "gte": "now-24h"
      }
    }
  },
  "aggs": {
    "api_calls": {
      "terms": {"field": "endpoint"},
      "aggs": {
        "avg_response_time": {
          "avg": {"field": "responseTime"}
        }
      }
    }
  }
}'
```

### 2. 自定义查询工具

```java
@Component
public class AuditQueryService {

    @Autowired
    private RestHighLevelClient elasticsearchClient;

    /**
     * 查询用户的安全事件
     */
    public List<Map<String, Object>> getUserEvents(String username, LocalDateTime start, LocalDateTime end) {
        try {
            SearchRequest searchRequest = new SearchRequest("security-audit-*");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("username", username))
                .must(QueryBuilders.rangeQuery("timestamp")
                    .gte(start.toString())
                    .lte(end.toString()));

            sourceBuilder.query(boolQuery);
            sourceBuilder.sort("timestamp", SortOrder.DESC);
            sourceBuilder.size(1000);

            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

            List<Map<String, Object>> results = new ArrayList<>();
            for (SearchHit hit : searchResponse.getHits().getHits()) {
                results.add(hit.getSourceAsMap());
            }

            return results;
        } catch (Exception e) {
            log.error("查询用户事件失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 统计安全事件
     */
    public Map<String, Object> getSecurityStatistics(LocalDateTime start, LocalDateTime end) {
        try {
            SearchRequest searchRequest = new SearchRequest("security-audit-*");
            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("timestamp")
                    .gte(start.toString())
                    .lte(end.toString()));

            sourceBuilder.query(boolQuery);

            // 按事件类型统计
            TermsAggregationBuilder eventTypeAgg = AggregationBuilders
                .terms("event_types")
                .field("eventType")
                .size(20);

            // 按严重性统计
            TermsAggregationBuilder severityAgg = AggregationBuilders
                .terms("severities")
                .field("severity")
                .size(10);

            sourceBuilder.aggregation(eventTypeAgg);
            sourceBuilder.aggregation(severityAgg);

            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

            return parseAggregationResults(searchResponse);
        } catch (Exception e) {
            log.error("获取安全统计失败", e);
            throw new RuntimeException(e);
        }
    }
}
```

---

## 📊 审计可视化

### 1. Grafana仪表板配置

#### 创建审计数据源
```json
{
  "name": "Elasticsearch Security Audit",
  "type": "elasticsearch",
  "url": "http://localhost:9200",
  "database": "security-audit-*",
  "jsonData": {
    "interval": "Daily",
    "timeField": "timestamp"
  }
}
```

#### 关键指标仪表板
```yaml
# Grafana Dashboard JSON
dashboard:
  title: "Security Audit Dashboard"
  panels:
    - title: "登录事件趋势"
      type: "graph"
      targets:
        - query: "SELECT timestamp, COUNT(*) FROM security-audit WHERE eventType='LOGIN' GROUP BY timestamp"

    - title: "登录失败Top用户"
      type: "table"
      targets:
        - query: "SELECT username, COUNT(*) as failure_count FROM security-audit WHERE eventType='LOGIN' AND success=false GROUP BY username ORDER BY failure_count DESC LIMIT 10"

    - title: "API调用统计"
      type: "graph"
      targets:
        - query: "SELECT endpoint, COUNT(*) as call_count FROM security-audit WHERE eventType='API_CALL' GROUP BY endpoint"

    - title: "敏感数据访问"
      type: "logs"
      targets:
        - query: "eventType:SENSITIVE_DATA_ACCESS"
```

### 2. 关键告警规则

```yaml
# Prometheus告警规则
groups:
  - name: security-audit
    rules:
      # 登录失败告警
      - alert: HighLoginFailureRate
        expr: |
          (
            increase(security_audit_login_failures_total[5m]) /
            increase(security_audit_login_attempts_total[5m])
          ) > 0.5
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "登录失败率过高"
          description: "过去5分钟内登录失败率超过50%"

      # 敏感数据访问告警
      - alert: SensitiveDataAccess
        expr: increase(security_audit_sensitive_data_access_total[1m]) > 10
        for: 0s
        labels:
          severity: critical
        annotations:
          summary: "敏感数据访问异常"
          description: "过去1分钟内敏感数据访问超过10次"

      # 权限变更告警
      - alert: PermissionChange
        expr: increase(security_audit_permission_changes_total[1m]) > 0
        for: 0s
        labels:
          severity: warning
        annotations:
          summary: "权限变更事件"
          description: "检测到权限变更操作"

      # 异常API调用告警
      - alert: HighAPIErrors
        expr: increase(security_audit_api_errors_total[5m]) > 100
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "API错误率过高"
          description: "过去5分钟内API错误超过100次"
```

---

## 📈 审计数据分析

### 1. 异常检测

```java
@Service
public class AnomalyDetectionService {

    @Autowired
    private AuditQueryService auditQueryService;

    /**
     * 检测异常登录
     */
    public List<LoginAnomaly> detectLoginAnomalies(LocalDateTime start, LocalDateTime end) {
        List<LoginAnomaly> anomalies = new ArrayList<>();

        // 查询所有登录事件
        List<Map<String, Object>> loginEvents = auditQueryService.getLoginEvents(start, end);

        // 按用户分组
        Map<String, List<Map<String, Object>>> userEvents = loginEvents.stream()
            .collect(Collectors.groupingBy(event -> (String) event.get("username")));

        // 检测异常
        for (Map.Entry<String, List<Map<String, Object>>> entry : userEvents.entrySet()) {
            String username = entry.getKey();
            List<Map<String, Object>> events = entry.getValue();

            // 检测多次登录失败
            long failures = events.stream()
                .filter(event -> !Boolean.TRUE.equals(event.get("success")))
                .count();

            if (failures > 10) {
                anomalies.add(new LoginAnomaly(username, "多次登录失败", failures));
            }

            // 检测异常时间登录
            List<LocalDateTime> loginTimes = events.stream()
                .filter(event -> Boolean.TRUE.equals(event.get("success")))
                .map(event -> LocalDateTime.parse((String) event.get("timestamp")))
                .collect(Collectors.toList());

            if (hasOffHoursLogin(loginTimes)) {
                anomalies.add(new LoginAnomaly(username, "异常时间登录", loginTimes.size()));
            }

            // 检测多个IP登录
            Set<String> ipAddresses = events.stream()
                .map(event -> (String) event.get("ipAddress"))
                .collect(Collectors.toSet());

            if (ipAddresses.size() > 5) {
                anomalies.add(new LoginAnomaly(username, "多IP登录", ipAddresses.size()));
            }
        }

        return anomalies;
    }

    private boolean hasOffHoursLogin(List<LocalDateTime> loginTimes) {
        return loginTimes.stream()
            .anyMatch(time -> time.getHour() < 6 || time.getHour() > 22);
    }
}
```

### 2. 合规报告

```java
@Service
public class ComplianceReportService {

    /**
     * 生成审计合规报告
     */
    public ComplianceReport generateComplianceReport(LocalDateTime start, LocalDateTime end) {
        ComplianceReport report = new ComplianceReport();
        report.setStartTime(start);
        report.setEndTime(end);

        // 登录审计
        report.setLoginAudit(generateLoginAudit(start, end));

        // 权限审计
        report.setPermissionAudit(generatePermissionAudit(start, end));

        // 数据访问审计
        report.setDataAccessAudit(generateDataAccessAudit(start, end));

        // 系统配置审计
        report.setConfigAudit(generateConfigAudit(start, end));

        return report;
    }

    private LoginAudit generateLoginAudit(LocalDateTime start, LocalDateTime end) {
        LoginAudit audit = new LoginAudit();

        // 统计登录次数
        audit.setTotalLogins(getTotalLogins(start, end));
        audit.setSuccessfulLogins(getSuccessfulLogins(start, end));
        audit.setFailedLogins(getFailedLogins(start, end));

        // 统计失败原因
        audit.setFailureReasons(getFailureReasons(start, end));

        // 检测可疑活动
        audit.setSuspiciousActivities(detectSuspiciousLogins(start, end));

        return audit;
    }
}
```

---

## 🧪 测试验证

### 1. 审计事件测试

```java
@SpringBootTest
public class SecurityAuditTest {

    @Autowired
    private SecurityAuditService auditService;

    @Test
    public void testLoginEvent() {
        String username = "testuser";
        String ipAddress = "127.0.0.1";
        String userAgent = "Mozilla/5.0";

        // 记录登录事件
        auditService.logLogin(username, true, ipAddress, userAgent, "登录成功");

        // 验证事件记录
        // 这里可以查询Kafka或Elasticsearch验证事件是否被正确记录
    }

    @Test
    public void testPermissionChangeEvent() {
        String adminUsername = "admin";
        String targetUsername = "user1";
        String action = "GRANT";
        String permission = "USER_READ";
        String resource = "用户管理";

        auditService.logPermissionChange(adminUsername, targetUsername, action, permission, resource);

        // 验证事件记录
    }

    @Test
    public void testDataAccessEvent() {
        String username = "testuser";
        String resource = "用户信息";
        String operation = "READ";
        String ipAddress = "127.0.0.1";

        auditService.logDataAccess(username, resource, operation, ipAddress, true);

        // 验证事件记录
    }
}
```

### 2. 拦截器测试

```java
@SpringBootTest
@AutoConfigureMockMvc
public class SecurityAuditInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testApiCallAudit() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        // 验证审计事件是否被记录
        // 可以查询Kafka或Elasticsearch验证
    }
}
```

---

## 📚 最佳实践

### 1. 审计日志设计

- **最小化原则**: 只记录必要的安全事件
- **结构化记录**: 使用统一的事件格式
- **及时性**: 异步处理，不影响业务性能
- **完整性**: 记录所有关键信息

### 2. 性能优化

- **异步处理**: 使用异步方式记录审计日志
- **批处理**: 批量发送审计事件
- **采样**: 对高频事件进行采样
- **压缩**: 压缩审计数据

### 3. 数据保护

- **敏感信息脱敏**: 对敏感数据进行脱敏处理
- **访问控制**: 限制审计日志的访问权限
- **数据加密**: 对审计日志进行加密存储
- **定期归档**: 定期归档和清理旧数据

### 4. 合规要求

- **数据保留**: 满足法规要求的数据保留期限
- **审计追踪**: 提供完整的审计追踪能力
- **不可篡改**: 确保审计日志的完整性
- **可查询性**: 提供灵活的查询能力

---

## 🔧 故障排除

### 1. 常见问题

#### Kafka连接失败
```
java.lang.IllegalStateException: Unknown state: UNEXPECTED_SERVER_ERROR
```
**解决**: 检查Kafka服务状态，确保网络连接正常

#### Elasticsearch索引失败
```
ElasticsearchException[resource_already_exists_exception]
```
**解决**: 检查索引是否已存在，避免重复创建

#### 审计数据丢失
```
Kafka consumer offset not found
```
**解决**: 调整消费者offset重置策略

### 2. 监控指标

```yaml
# 审计监控指标
metrics:
  - name: audit_events_total
    type: counter
    description: Total audit events

  - name: audit_events_duration
    type: histogram
    description: Audit event processing duration

  - name: kafka_producer_errors_total
    type: counter
    description: Kafka producer errors

  - name: elasticsearch_index_errors_total
    type: counter
    description: Elasticsearch index errors
```

### 3. 健康检查

```java
@RestController
public class AuditHealthCheck {

    @GetMapping("/actuator/health/audit")
    public Map<String, Object> checkAuditHealth() {
        Map<String, Object> result = new HashMap<>();

        // 检查Kafka连接
        try {
            List<PartitionInfo> partitions = kafkaTemplate.getProducerFactory()
                .createProducer()
                .partitionsFor("security-audit");
            result.put("kafka", "OK - " + partitions.size() + " partitions");
        } catch (Exception e) {
            result.put("kafka", "ERROR: " + e.getMessage());
        }

        // 检查Elasticsearch连接
        try {
            ClusterHealthResponse health = elasticsearchClient
                .cluster()
                .health(ClusterHealthRequest.DEFAULT, RequestOptions.DEFAULT);
            result.put("elasticsearch", "OK - " + health.getStatus());
        } catch (Exception e) {
            result.put("elasticsearch", "ERROR: " + e.getMessage());
        }

        return result;
    }
}
```

---

## 📞 技术支持

### 联系方式
- **技术支持邮箱**: support@basebackend.com
- **技术文档**: https://docs.basebackend.com/audit
- **GitHub**: https://github.com/basebackend/security-audit

### 参考资料
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [Elastic Security](https://www.elastic.co/security)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
