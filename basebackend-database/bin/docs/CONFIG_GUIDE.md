# 配置说明文档

本文档详细说明 basebackend-database 模块的所有配置项。

## 目录

- [审计系统配置](#审计系统配置)
- [多租户配置](#多租户配置)
- [数据安全配置](#数据安全配置)
- [健康监控配置](#健康监控配置)
- [动态数据源配置](#动态数据源配置)
- [故障转移配置](#故障转移配置)
- [SQL 统计配置](#sql-统计配置)
- [Flyway 配置](#flyway-配置)

## 审计系统配置

### 基础配置

```yaml
database:
  enhanced:
    audit:
      # 是否启用审计系统
      enabled: true
      
      # 是否异步处理审计日志
      # true: 异步处理，不影响业务性能
      # false: 同步处理，确保审计日志立即写入
      async: true
      
      # 线程池配置（仅在 async=true 时生效）
      thread-pool:
        # 核心线程数
        core-size: 2
        # 最大线程数
        max-size: 5
        # 队列容量
        queue-capacity: 1000
      
      # 审计日志保留天数
      # 超过此天数的日志将被归档或删除
      retention-days: 90
      
      # 排除的表（不记录审计日志）
      excluded-tables:
        - sys_audit_log
        - sys_audit_log_archive
        - sys_sql_statistics
```

### 归档配置

```yaml
database:
  enhanced:
    audit:
      archive:
        # 是否启用归档功能
        # true: 过期日志移动到归档表
        # false: 过期日志直接删除
        enabled: true
        
        # 归档数据保留天数
        # 归档表中的数据保留时间
        archive-retention-days: 365
        
        # 自动清理定时任务的 cron 表达式
        # 默认每天凌晨 2 点执行
        cleanup-cron: "0 0 2 * * ?"
        
        # 是否启用自动清理定时任务
        auto-cleanup-enabled: true
```

### 使用场景

#### 场景 1：高性能场景
```yaml
database:
  enhanced:
    audit:
      enabled: true
      async: true  # 异步处理
      thread-pool:
        core-size: 4
        max-size: 10
        queue-capacity: 2000
```

#### 场景 2：强一致性场景
```yaml
database:
  enhanced:
    audit:
      enabled: true
      async: false  # 同步处理，确保审计日志立即写入
```

#### 场景 3：长期存储场景
```yaml
database:
  enhanced:
    audit:
      retention-days: 180  # 主表保留 6 个月
      archive:
        enabled: true
        archive-retention-days: 1825  # 归档表保留 5 年
```

## 多租户配置

### 基础配置

```yaml
database:
  enhanced:
    multi-tenancy:
      # 是否启用多租户
      enabled: true
      
      # 隔离模式
      # SHARED_DB: 共享数据库，通过 tenant_id 字段隔离
      # SEPARATE_DB: 独立数据库，每个租户使用独立的数据库
      # SEPARATE_SCHEMA: 独立 Schema，每个租户使用独立的 Schema
      isolation-mode: SHARED_DB
      
      # 租户字段名（仅在 SHARED_DB 模式下使用）
      tenant-column: tenant_id
      
      # 排除的表（不添加租户过滤）
      excluded-tables:
        - sys_tenant_config
        - sys_dict
        - sys_config
```

### 不同隔离模式配置

#### 共享数据库模式（SHARED_DB）

```yaml
database:
  enhanced:
    multi-tenancy:
      enabled: true
      isolation-mode: SHARED_DB
      tenant-column: tenant_id
```

特点：
- 所有租户共享同一个数据库
- 通过 tenant_id 字段隔离数据
- 成本最低，管理最简单
- 适合租户数量多、数据量小的场景

#### 独立数据库模式（SEPARATE_DB）

```yaml
database:
  enhanced:
    multi-tenancy:
      enabled: true
      isolation-mode: SEPARATE_DB

# 需要配置租户数据源映射
spring:
  datasource:
    dynamic:
      datasource:
        tenant_001:
          url: jdbc:mysql://localhost:3306/tenant_001_db
          username: root
          password: password
        tenant_002:
          url: jdbc:mysql://localhost:3306/tenant_002_db
          username: root
          password: password
```

特点：
- 每个租户使用独立的数据库
- 数据隔离性最强
- 可以为不同租户提供不同的性能配置
- 适合租户数量少、数据量大的场景

#### 独立 Schema 模式（SEPARATE_SCHEMA）

```yaml
database:
  enhanced:
    multi-tenancy:
      enabled: true
      isolation-mode: SEPARATE_SCHEMA

# 需要配置租户 Schema 映射
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/
    username: root
    password: password
```

特点：
- 每个租户使用独立的 Schema
- 在独立数据库和共享数据库之间取得平衡
- 适合中等规模的多租户场景

## 数据安全配置

### 加密配置

```yaml
database:
  enhanced:
    security:
      encryption:
        # 是否启用加密
        enabled: true
        
        # 加密算法
        # AES: AES-256 加密
        # SM4: 国密 SM4 加密
        algorithm: AES
        
        # 加密密钥
        # 生产环境应从密钥管理服务（如 Vault）获取
        # 支持环境变量：${ENCRYPTION_KEY}
        secret-key: ${ENCRYPTION_KEY:your-secret-key-here}
```

### 脱敏配置

```yaml
database:
  enhanced:
    security:
      masking:
        # 是否启用脱敏
        enabled: true
        
        # 脱敏规则
        rules:
          # 手机号脱敏：保留后 4 位
          phone: "***-****-####"
          
          # 身份证号脱敏：保留前 6 位和后 4 位
          id-card: "######********####"
          
          # 银行卡号脱敏：保留前 4 位和后 4 位
          bank-card: "#### **** **** ####"
          
          # 邮箱脱敏：保留前 3 位和域名
          email: "***@domain.com"
          
          # 姓名脱敏：保留姓氏
          name: "*名"
```

### 脱敏规则说明

- `#`: 保留原字符
- `*`: 替换为星号
- 其他字符：直接显示

示例：
- `phone: "***-****-####"` → `13812345678` 显示为 `***-****-5678`
- `id-card: "######********####"` → `110101199001011234` 显示为 `110101********1234`

### 安全最佳实践

#### 生产环境配置

```yaml
database:
  enhanced:
    security:
      encryption:
        enabled: true
        algorithm: AES
        # 从环境变量或密钥管理服务获取密钥
        secret-key: ${ENCRYPTION_KEY}
      
      masking:
        enabled: true
        rules:
          phone: "***-****-####"
          id-card: "######********####"
          bank-card: "#### **** **** ####"
```

环境变量设置：
```bash
export ENCRYPTION_KEY="your-32-character-secret-key-here"
```

#### 开发环境配置

```yaml
database:
  enhanced:
    security:
      encryption:
        enabled: false  # 开发环境可以禁用加密
      
      masking:
        enabled: true  # 保持脱敏以测试功能
```

## 健康监控配置

```yaml
database:
  enhanced:
    health:
      # 是否启用健康监控
      enabled: true
      
      # 健康检查间隔（秒）
      check-interval: 30
      
      # 慢查询阈值（毫秒）
      # 超过此阈值的 SQL 将被记录为慢查询
      slow-query-threshold: 1000
      
      # 连接池使用率告警阈值（百分比）
      # 超过此阈值将触发告警
      pool-usage-threshold: 80
```

### 不同场景配置

#### 高性能场景
```yaml
database:
  enhanced:
    health:
      enabled: true
      check-interval: 60  # 降低检查频率
      slow-query-threshold: 500  # 更严格的慢查询阈值
      pool-usage-threshold: 90
```

#### 开发环境
```yaml
database:
  enhanced:
    health:
      enabled: true
      check-interval: 10  # 更频繁的检查
      slow-query-threshold: 2000  # 更宽松的阈值
      pool-usage-threshold: 95
```

## 动态数据源配置

```yaml
database:
  enhanced:
    dynamic-datasource:
      # 是否启用动态数据源
      enabled: true
      
      # 默认数据源
      primary: master
      
      # 严格模式
      # true: 数据源不存在时抛出异常
      # false: 数据源不存在时使用默认数据源
      strict: true

# 配置多个数据源
spring:
  datasource:
    dynamic:
      # 默认数据源
      primary: master
      
      # 严格模式
      strict: true
      
      # 数据源配置
      datasource:
        # 主库
        master:
          url: jdbc:mysql://localhost:3306/db_master
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
        
        # 从库
        slave:
          url: jdbc:mysql://localhost:3306/db_slave
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
        
        # 订单库
        order_db:
          url: jdbc:mysql://localhost:3306/db_order
          username: root
          password: password
          driver-class-name: com.mysql.cj.jdbc.Driver
```

### 读写分离配置

```yaml
spring:
  datasource:
    dynamic:
      primary: master
      datasource:
        master:
          url: jdbc:mysql://master-host:3306/db
          username: root
          password: password
        
        slave_1:
          url: jdbc:mysql://slave1-host:3306/db
          username: root
          password: password
        
        slave_2:
          url: jdbc:mysql://slave2-host:3306/db
          username: root
          password: password
```

## 故障转移配置

```yaml
database:
  enhanced:
    failover:
      # 是否启用故障转移
      enabled: true
      
      # 重连尝试次数
      max-retry: 3
      
      # 重连间隔（毫秒）
      retry-interval: 5000
      
      # 主库降级
      # true: 主库不可用时降级到只读模式（使用从库）
      # false: 主库不可用时抛出异常
      master-degradation: false
```

### 不同场景配置

#### 高可用场景
```yaml
database:
  enhanced:
    failover:
      enabled: true
      max-retry: 5
      retry-interval: 3000
      master-degradation: true  # 允许降级
```

#### 严格一致性场景
```yaml
database:
  enhanced:
    failover:
      enabled: true
      max-retry: 3
      retry-interval: 5000
      master-degradation: false  # 不允许降级
```

## SQL 统计配置

```yaml
database:
  enhanced:
    sql-statistics:
      # 是否启用 SQL 统计
      enabled: true
      
      # 统计数据保留天数
      retention-days: 30
      
      # 是否启用执行计划分析
      # 注意：执行计划分析会影响性能，生产环境慎用
      explain-enabled: false
```

### 不同环境配置

#### 生产环境
```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true
      retention-days: 30
      explain-enabled: false  # 生产环境禁用
```

#### 测试环境
```yaml
database:
  enhanced:
    sql-statistics:
      enabled: true
      retention-days: 7
      explain-enabled: true  # 测试环境可以启用
```

## Flyway 配置

```yaml
spring:
  flyway:
    # 是否启用 Flyway
    enabled: true
    
    # 迁移脚本位置
    locations: classpath:db/migration
    
    # 基线版本
    baseline-version: 1.0.0
    
    # 基线迁移
    # true: 对已存在的数据库执行基线迁移
    # false: 只对空数据库执行迁移
    baseline-on-migrate: true
    
    # 验证迁移
    # true: 启动时验证已执行的迁移脚本
    # false: 不验证
    validate-on-migrate: true
    
    # 允许乱序迁移
    # true: 允许执行版本号小于已执行版本的迁移
    # false: 不允许
    out-of-order: false
    
    # 表名
    table: flyway_schema_history
    
    # 编码
    encoding: UTF-8
    
    # 占位符配置
    placeholder-replacement: true
    placeholders:
      database: basebackend
```

### 不同环境配置

#### 开发环境
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    validate-on-migrate: false  # 开发环境可以不验证
    out-of-order: true  # 允许乱序
```

#### 生产环境
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: false
    validate-on-migrate: true  # 生产环境必须验证
    out-of-order: false  # 不允许乱序
```

## 完整配置示例

### 最小配置

```yaml
database:
  enhanced:
    audit:
      enabled: true
    health:
      enabled: true
```

### 推荐配置（生产环境）

```yaml
database:
  enhanced:
    # 审计系统
    audit:
      enabled: true
      async: true
      retention-days: 90
      archive:
        enabled: true
        archive-retention-days: 365
    
    # 多租户（按需启用）
    multi-tenancy:
      enabled: false
    
    # 数据安全
    security:
      encryption:
        enabled: true
        algorithm: AES
        secret-key: ${ENCRYPTION_KEY}
      masking:
        enabled: true
        rules:
          phone: "***-****-####"
          id-card: "######********####"
          bank-card: "#### **** **** ####"
    
    # 健康监控
    health:
      enabled: true
      check-interval: 30
      slow-query-threshold: 1000
      pool-usage-threshold: 80
    
    # 动态数据源
    dynamic-datasource:
      enabled: true
      primary: master
      strict: true
    
    # 故障转移
    failover:
      enabled: true
      max-retry: 3
      retry-interval: 5000
      master-degradation: false
    
    # SQL 统计
    sql-statistics:
      enabled: true
      retention-days: 30
      explain-enabled: false

# Flyway
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
    out-of-order: false
```

### 完整配置（所有功能启用）

```yaml
database:
  enhanced:
    audit:
      enabled: true
      async: true
      thread-pool:
        core-size: 2
        max-size: 5
        queue-capacity: 1000
      retention-days: 90
      excluded-tables:
        - sys_audit_log
        - sys_audit_log_archive
        - sys_sql_statistics
      archive:
        enabled: true
        archive-retention-days: 365
        cleanup-cron: "0 0 2 * * ?"
        auto-cleanup-enabled: true
    
    multi-tenancy:
      enabled: true
      isolation-mode: SHARED_DB
      tenant-column: tenant_id
      excluded-tables:
        - sys_tenant_config
        - sys_dict
    
    security:
      encryption:
        enabled: true
        algorithm: AES
        secret-key: ${ENCRYPTION_KEY}
      masking:
        enabled: true
        rules:
          phone: "***-****-####"
          id-card: "######********####"
          bank-card: "#### **** **** ####"
          email: "***@domain.com"
          name: "*名"
    
    health:
      enabled: true
      check-interval: 30
      slow-query-threshold: 1000
      pool-usage-threshold: 80
    
    dynamic-datasource:
      enabled: true
      primary: master
      strict: true
    
    failover:
      enabled: true
      max-retry: 3
      retry-interval: 5000
      master-degradation: false
    
    sql-statistics:
      enabled: true
      retention-days: 30
      explain-enabled: false

spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-version: 1.0.0
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
    table: flyway_schema_history
    encoding: UTF-8
```

## 配置优先级

配置的加载优先级（从高到低）：

1. 命令行参数
2. 环境变量
3. `application-{profile}.yml`
4. `application.yml`
5. `application-database-enhanced.yml`（默认配置）

## 配置验证

启动时会自动验证配置的合法性，如果配置有误会抛出异常并提示错误信息。

常见配置错误：

1. 加密密钥长度不正确（AES 需要 16/24/32 字节）
2. 数据源名称不存在
3. cron 表达式格式错误
4. 阈值配置超出合理范围

## 动态配置更新

部分配置支持动态更新（无需重启应用）：

- 慢查询阈值
- 连接池告警阈值
- 健康检查间隔

使用 Spring Cloud Config 或 Nacos 可以实现配置的动态刷新。
