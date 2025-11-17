# Nacos 配置迁移指南

> 本文档指导如何将各微服务模块的配置统一迁移到 Nacos 配置中心进行管理

## 📋 目录

- [1. 概述](#1-概述)
- [2. Nacos 命名空间设计](#2-nacos-命名空间设计)
- [3. 配置分类](#3-配置分类)
- [4. 迁移步骤](#4-迁移步骤)
- [5. 配置示例](#5-配置示例)
- [6. 验证清单](#6-验证清单)

---

## 1. 概述

### 1.1 迁移目标

将以下配置从各模块的 `application.yml` 迁移到 Nacos：

- **数据库配置** - MySQL、Druid 连接池
- **缓存配置** - Redis、Redisson
- **消息队列配置** - RocketMQ
- **可观测性配置** - Micrometer、Prometheus
- **安全配置** - JWT、CSRF、Origin 校验
- **服务发现配置** - Nacos Discovery
- **分布式事务配置** - Seata

### 1.2 迁移优势

✅ **统一管理** - 所有配置集中在 Nacos，便于维护
✅ **环境隔离** - dev/test/prod 环境配置分离
✅ **动态刷新** - 配置变更无需重启应用
✅ **版本管理** - Nacos 自动保存配置历史
✅ **权限控制** - 细粒度的配置访问权限

---

## 2. Nacos 命名空间设计

### 2.1 命名空间结构

```
Nacos
├── dev (开发环境)
│   ├── common-config.yml          # 通用配置
│   ├── database-config.yml        # 数据库配置
│   ├── redis-config.yml           # Redis 配置
│   ├── rocketmq-config.yml        # RocketMQ 配置
│   ├── observability-config.yml   # 可观测性配置
│   └── security-config.yml        # 安全配置
│
├── test (测试环境)
│   └── [同 dev 结构]
│
└── prod (生产环境)
    └── [同 dev 结构]
```

### 2.2 命名空间 ID

| 环境 | Namespace ID | 描述 |
|------|--------------|------|
| 开发环境 | `dev` | 本地开发使用 |
| 测试环境 | `test` | CI/CD 测试使用 |
| 生产环境 | `prod` | 线上生产环境 |

---

## 3. 配置分类

### 3.1 通用配置 (common-config.yml)

**Data ID**: `common-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
# 应用基础配置
spring:
  application:
    name: basebackend

# 日志配置
logging:
  level:
    root: INFO
    com.basebackend: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 3.2 数据库配置 (database-config.yml)

**Data ID**: `database-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/basebackend?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: ${MYSQL_PASSWORD:root123456}  # 支持环境变量

    # Druid 连接池配置
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20

      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: ${DRUID_MONITOR_PASSWORD:admin123}

      # 过滤器配置
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 2000
        wall:
          enabled: true

# MyBatis Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: AUTO
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 3.3 Redis 配置 (redis-config.yml)

**Data ID**: `redis-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 5000ms

      # Lettuce 连接池配置
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 2000ms

# Redisson 配置
redisson:
  single-server-config:
    address: "redis://localhost:6379"
    password: ${REDIS_PASSWORD:}
    database: 0
    connection-pool-size: 20
    connection-minimum-idle-size: 5
    idle-connection-timeout: 10000
    timeout: 3000
    retry-attempts: 3
    retry-interval: 1500
```

### 3.4 RocketMQ 配置 (rocketmq-config.yml)

**Data ID**: `rocketmq-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: basebackend-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
    max-message-size: 4194304  # 4MB
  consumer:
    pull-batch-size: 10
```

### 3.5 可观测性配置 (observability-config.yml)

**Data ID**: `observability-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${spring.profiles.active:dev}
```

### 3.6 安全配置 (security-config.yml)

**Data ID**: `security-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
# JWT 配置
jwt:
  secret: ${JWT_SECRET:your-secret-key-here-change-in-production}
  expiration: 86400000  # 24小时（毫秒）
  refresh-expiration: 604800000  # 7天（毫秒）

# Web 安全基线配置
security:
  baseline:
    allowed-origins:
      - http://localhost:3000
      - https://localhost:3000
    enforce-referer: true

  # 密钥管理器配置
  secret-manager:
    cache-ttl: PT15M  # 15分钟
```

### 3.7 Seata 配置 (seata-config.yml)

**Data ID**: `seata-config.yml`
**Group**: `DEFAULT_GROUP`
**格式**: YAML

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: ${spring.application.name}-group
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: localhost:8848
      namespace: dev
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: dev
      group: SEATA_GROUP
```

---

## 4. 迁移步骤

### 4.1 前置准备

1. **启动 Nacos 服务器**
   ```bash
   # 单机模式启动
   sh nacos/bin/startup.sh -m standalone
   # 或 Windows
   nacos/bin/startup.cmd -m standalone
   ```

2. **创建命名空间**
   - 访问 Nacos 控制台: http://localhost:8848/nacos
   - 登录（默认 nacos/nacos）
   - 命名空间管理 → 新建命名空间
   - 创建 `dev`、`test`、`prod` 三个命名空间

### 4.2 配置导入

#### 方式一：Web 控制台手动导入

1. 进入 **配置管理** → **配置列表**
2. 选择对应的命名空间（如 `dev`）
3. 点击 **发布配置**
4. 填写：
   - **Data ID**: `database-config.yml`
   - **Group**: `DEFAULT_GROUP`
   - **配置格式**: YAML
   - **配置内容**: 复制上述配置内容
5. 点击 **发布**
6. 重复步骤 3-5，导入其他配置文件

#### 方式二：使用 Nacos Open API 批量导入

创建导入脚本 `import-configs.sh`:

```bash
#!/bin/bash

NACOS_SERVER="http://localhost:8848"
NAMESPACE="dev"  # 改为对应的命名空间 ID
GROUP="DEFAULT_GROUP"

# 导入配置的函数
import_config() {
  local data_id=$1
  local config_file=$2

  curl -X POST "$NACOS_SERVER/nacos/v1/cs/configs" \
    -d "dataId=$data_id" \
    -d "group=$GROUP" \
    -d "content=$(cat $config_file)" \
    -d "type=yaml" \
    -d "tenant=$NAMESPACE"

  echo "Imported $data_id"
}

# 导入所有配置
import_config "common-config.yml" "./nacos-configs/common-config.yml"
import_config "database-config.yml" "./nacos-configs/database-config.yml"
import_config "redis-config.yml" "./nacos-configs/redis-config.yml"
import_config "rocketmq-config.yml" "./nacos-configs/rocketmq-config.yml"
import_config "observability-config.yml" "./nacos-configs/observability-config.yml"
import_config "security-config.yml" "./nacos-configs/security-config.yml"
import_config "seata-config.yml" "./nacos-configs/seata-config.yml"

echo "All configurations imported successfully!"
```

### 4.3 更新 bootstrap.yml

在各微服务模块的 `src/main/resources/bootstrap.yml` 中配置 Nacos：

```yaml
spring:
  application:
    name: basebackend-admin-api  # 根据实际模块修改

  profiles:
    active: dev  # 或 test、prod

  cloud:
    nacos:
      # 服务发现配置
      discovery:
        server-addr: localhost:8848
        namespace: ${spring.profiles.active}
        group: DEFAULT_GROUP

      # 配置中心配置
      config:
        server-addr: localhost:8848
        namespace: ${spring.profiles.active}
        group: DEFAULT_GROUP
        file-extension: yml
        refresh-enabled: true  # 开启动态刷新

        # 扩展配置（共享配置）
        extension-configs:
          - data-id: common-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: database-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: redis-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: rocketmq-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: observability-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: security-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: seata-config.yml
            group: DEFAULT_GROUP
            refresh: true
```

### 4.4 清理本地配置

从各模块的 `application.yml` 中移除已迁移到 Nacos 的配置：

```yaml
# application.yml - 只保留本地特有配置
server:
  port: 8080  # 各模块端口不同，保留在本地

# 其他配置已迁移到 Nacos，可以删除或注释
```

### 4.5 支持动态刷新

对于需要动态刷新的配置类，添加 `@RefreshScope` 注解：

```java
@Component
@RefreshScope  // 支持 Nacos 配置动态刷新
@ConfigurationProperties(prefix = "security.baseline")
public class SecurityBaselineProperties {
    private List<String> allowedOrigins = new ArrayList<>();
    private boolean enforceReferer = true;
    // getters and setters...
}
```

---

## 5. 配置示例

### 5.1 完整的 bootstrap.yml 示例

```yaml
# basebackend-admin-api/src/main/resources/bootstrap.yml
spring:
  application:
    name: basebackend-admin-api

  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}

  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        namespace: ${spring.profiles.active}
        group: DEFAULT_GROUP
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}

      config:
        server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
        namespace: ${spring.profiles.active}
        group: DEFAULT_GROUP
        file-extension: yml
        refresh-enabled: true
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}

        extension-configs:
          - data-id: common-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: database-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: redis-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: rocketmq-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: observability-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: security-config.yml
            group: DEFAULT_GROUP
            refresh: true

          - data-id: seata-config.yml
            group: DEFAULT_GROUP
            refresh: true

# 日志配置（保留在本地）
logging:
  level:
    root: INFO
    com.basebackend: DEBUG
```

### 5.2 环境变量配置

为了支持不同环境，建议使用环境变量：

```bash
# .env 文件（不要提交到 Git）
SPRING_PROFILES_ACTIVE=dev
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

# 数据库
MYSQL_PASSWORD=your-db-password

# Redis
REDIS_PASSWORD=your-redis-password

# JWT
JWT_SECRET=your-jwt-secret-key-change-in-production

# Druid 监控
DRUID_MONITOR_PASSWORD=your-druid-password
```

---

## 6. 验证清单

### 6.1 配置导入验证

- [ ] 所有配置文件已成功导入 Nacos
- [ ] 配置内容正确，无格式错误
- [ ] 环境变量占位符正确（如 `${MYSQL_PASSWORD}`）
- [ ] 命名空间隔离正确（dev/test/prod）

### 6.2 服务启动验证

- [ ] 服务能正常启动
- [ ] 日志显示成功连接到 Nacos
- [ ] 日志显示成功加载配置：
  ```
  Located property source: [BootstrapPropertySource {name='bootstrap'}]
  Located property source: CompositePropertySource {name='NACOS',
    propertySources=[NacosPropertySource {name='database-config.yml'}, ...]}
  ```

### 6.3 功能验证

- [ ] **数据库连接** - 能正常查询数据
- [ ] **Redis 连接** - 能正常读写缓存
- [ ] **RocketMQ 连接** - 能正常发送/接收消息
- [ ] **JWT 认证** - Token 生成和验证正常
- [ ] **Druid 监控** - 访问 http://localhost:8080/druid 正常

### 6.4 动态刷新验证

1. 修改 Nacos 中的配置（如修改日志级别）
2. 观察应用日志，确认配置已刷新
3. 验证新配置生效

### 6.5 回滚方案

如果迁移出现问题，可以快速回滚：

1. **临时回滚** - 修改 `bootstrap.yml`，禁用 Nacos Config：
   ```yaml
   spring:
     cloud:
       nacos:
         config:
           enabled: false  # 禁用 Nacos 配置
   ```

2. **完全回滚** - 恢复本地 `application.yml` 配置文件

---

## 7. 最佳实践

### 7.1 配置管理

- **敏感信息加密** - 使用 Nacos 的加密特性或外部密钥管理系统
- **配置分组** - 不同类型的配置使用不同的 Group
- **版本管理** - 重要配置变更前先备份（Nacos 自动保存历史版本）
- **权限控制** - 生产环境配置设置严格的访问权限

### 7.2 环境隔离

- **命名空间隔离** - dev/test/prod 使用不同的命名空间
- **配置差异化** - 不同环境的配置值不同（如数据库地址、密码等）
- **环境变量** - 使用环境变量覆盖默认值

### 7.3 监控告警

- **配置变更监控** - 监控 Nacos 配置的变更历史
- **服务健康检查** - 监控服务是否成功连接 Nacos
- **配置刷新告警** - 配置刷新失败时发送告警

---

## 8. 常见问题

### Q1: 服务启动时找不到 Nacos 配置怎么办？

**A:** 检查以下几点：
1. Nacos 服务是否正常运行
2. `bootstrap.yml` 中的 `server-addr` 是否正确
3. 命名空间 ID 是否正确
4. Data ID 和 Group 是否匹配

### Q2: 配置修改后服务没有刷新怎么办？

**A:** 确认：
1. `refresh-enabled: true` 是否设置
2. 配置类是否添加了 `@RefreshScope` 注解
3. 检查 Nacos 日志，确认配置推送成功

### Q3: 如何在本地开发时不使用 Nacos？

**A:** 在 `bootstrap.yml` 中添加 profile 条件：
```yaml
spring:
  cloud:
    nacos:
      config:
        enabled: ${NACOS_CONFIG_ENABLED:true}
```

然后在本地运行时设置环境变量 `NACOS_CONFIG_ENABLED=false`

---

## 9. 总结

通过将配置迁移到 Nacos，可以实现：

✅ **统一管理** - 所有环境的配置集中管理
✅ **动态刷新** - 配置变更无需重启
✅ **环境隔离** - dev/test/prod 配置分离
✅ **版本管理** - 配置历史可追溯
✅ **权限控制** - 细粒度的访问控制

**迁移完成后，记得：**
1. 删除或注释本地 `application.yml` 中已迁移的配置
2. 更新部署文档，说明 Nacos 依赖
3. 配置监控告警，确保配置服务的高可用性

---

*文档版本: v1.0*
*最后更新: 2025-01-13*
*作者: BaseBackend Team*
