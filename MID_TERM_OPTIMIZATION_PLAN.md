# BaseBackend 架构优化 - 中期计划

> 数据库优化 + API 网关增强 + 服务拆分 = 高性能微服务架构 🚀

**规划日期：** 2025-01-13
**规划人：** 浮浮酱（猫娘工程师）
**状态：** 🔄 进行中

---

## 📋 目录

- [概述](#概述)
- [Phase 7 - 数据库优化](#phase-7---数据库优化)
- [Phase 8 - API 网关增强](#phase-8---api-网关增强)
- [Phase 9 - 服务拆分](#phase-9---服务拆分)
- [时间规划](#时间规划)
- [风险评估](#风险评估)
- [成功标准](#成功标准)

---

## 概述

### 背景

在完成短期计划后，系统已经具备：
- ✅ 完整的可观测性体系（Prometheus + Grafana + Zipkin）
- ✅ 强大的服务治理能力（Sentinel 流控熔断）
- ✅ 统一的配置管理（Nacos）
- ✅ 分布式事务支持（Seata）

现在需要进入中期优化，重点解决：
- 🔧 **数据库性能瓶颈** - 读写分离、连接池优化
- 🔧 **网关功能增强** - 重试、超时、版本管理
- 🔧 **服务拆分** - 解耦核心业务，提升可扩展性

### 目标

**总体目标：**
- 📈 数据库 QPS 提升 3 倍
- ⚡ API 响应时间降低 40%
- 🎯 系统可扩展性提升 5 倍
- 🔄 服务独立部署能力

**具体指标：**
- 数据库读写分离覆盖率 > 80%
- Gateway 重试成功率 > 95%
- 服务拆分完成度 > 60%（核心服务）
- 系统可用性 > 99.9%

---

## Phase 7 - 数据库优化

### 7.1 数据库读写分离配置 ✨

**目标：** 实现主从数据库自动路由，读操作分散到从库

**实施步骤：**

1. **配置多数据源**
   - 主库（Master）- 处理写操作
   - 从库（Slave1、Slave2）- 处理读操作
   - 配置文件：`mysql-config.yml`

2. **实现动态数据源切换**
   - 创建 `DynamicDataSource` 继承 `AbstractRoutingDataSource`
   - 使用 ThreadLocal 保存数据源类型
   - AOP 拦截 `@Transactional` 和 `@ReadOnly`

3. **配置 MyBatis Plus**
   - 主库配置：`master-datasource`
   - 从库配置：`slave-datasource`
   - 负载均衡策略：轮询、随机、权重

**预期效果：**
- 读操作性能提升 200%
- 主库压力降低 60%
- 支持水平扩展读能力

**技术方案：**
```java
// 动态数据源
public class DynamicDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceType();
    }
}

// 数据源上下文
public class DataSourceContextHolder {
    private static final ThreadLocal<DataSourceType> CONTEXT_HOLDER =
        new ThreadLocal<>();

    public static void setDataSourceType(DataSourceType type) {
        CONTEXT_HOLDER.set(type);
    }

    public static DataSourceType getDataSourceType() {
        return CONTEXT_HOLDER.get();
    }
}

// AOP 切面
@Aspect
public class DataSourceAspect {
    @Around("@annotation(readOnly)")
    public Object around(ProceedingJoinPoint point, ReadOnly readOnly) {
        DataSourceContextHolder.setDataSourceType(DataSourceType.SLAVE);
        try {
            return point.proceed();
        } finally {
            DataSourceContextHolder.clearDataSourceType();
        }
    }
}
```

### 7.2 实现动态数据源切换 ✨

**目标：** 根据业务场景自动选择合适的数据源

**实施步骤：**

1. **注解驱动**
   - `@ReadOnly` - 标记只读方法，路由到从库
   - `@Master` - 强制使用主库
   - 默认规则：`@Transactional` → 主库，查询 → 从库

2. **负载均衡**
   - 轮询（Round Robin）
   - 随机（Random）
   - 权重（Weight）
   - 最少连接（Least Connections）

3. **故障转移**
   - 从库不可用时自动切换到主库
   - 健康检查机制
   - 自动剔除故障节点

**预期效果：**
- 自动化数据源选择，减少手动配置
- 高可用性，故障自动恢复
- 灵活的负载均衡策略

### 7.3 配置数据库连接池优化 ✨

**目标：** 优化 Druid 连接池配置，提升数据库连接性能

**实施步骤：**

1. **核心参数调优**
   - `initialSize`: 10 → 20（初始连接数）
   - `minIdle`: 10 → 15（最小空闲连接）
   - `maxActive`: 100 → 200（最大活跃连接）
   - `maxWait`: 60000 → 30000（获取连接最大等待时间）

2. **连接检测配置**
   - `testWhileIdle`: true（空闲时检测）
   - `testOnBorrow`: false（借用时不检测，提升性能）
   - `testOnReturn`: false（归还时不检测）
   - `validationQuery`: SELECT 1

3. **性能监控**
   - 启用 Druid 监控
   - 配置慢SQL记录（> 1s）
   - 连接泄漏检测

**预期效果：**
- 连接获取时间降低 50%
- 连接复用率提升 30%
- 慢SQL及时发现和优化

**配置示例：**
```yaml
spring:
  datasource:
    druid:
      master:
        initial-size: 20
        min-idle: 15
        max-active: 200
        max-wait: 30000
        test-while-idle: true
        time-between-eviction-runs-millis: 60000
        min-evictable-idle-time-millis: 300000
        validation-query: SELECT 1
        test-on-borrow: false
        test-on-return: false
        pool-prepared-statements: true
        max-pool-prepared-statement-per-connection-size: 20

      slave:
        initial-size: 15
        min-idle: 10
        max-active: 150
        max-wait: 30000
        # ... 其他配置同 master

      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin123

      # 慢SQL记录
      filter:
        stat:
          enabled: true
          log-slow-sql: true
          slow-sql-millis: 1000
```

### 7.4 实现慢查询监控和优化 ✨

**目标：** 自动发现和优化慢查询，提升查询性能

**实施步骤：**

1. **慢查询拦截器**
   - MyBatis 插件拦截 SQL 执行
   - 记录执行时间 > 阈值的 SQL
   - 输出 SQL 语句和参数

2. **慢查询分析**
   - 统计慢查询 TOP 10
   - 分析执行计划（EXPLAIN）
   - 生成优化建议

3. **自动告警**
   - 慢查询超过阈值发送告警
   - 集成 Prometheus 监控
   - Grafana 可视化

**预期效果：**
- 慢查询及时发现，响应时间 < 5分钟
- 90% 的慢查询得到优化
- 整体查询性能提升 40%

**技术方案：**
```java
@Intercepts({
    @Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class}
    )
})
public class SlowQueryInterceptor implements Interceptor {

    private static final long SLOW_SQL_THRESHOLD = 1000; // 1秒

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = invocation.proceed();
        long end = System.currentTimeMillis();
        long duration = end - start;

        if (duration > SLOW_SQL_THRESHOLD) {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            Object parameter = invocation.getArgs()[1];

            String sql = getSql(ms, parameter);
            log.warn("慢查询检测: 执行时间={}ms, SQL={}", duration, sql);

            // 发送告警
            alertService.sendSlowQueryAlert(sql, duration);

            // 记录 Metrics
            customMetrics.recordSlowQuery(ms.getId(), duration);
        }

        return result;
    }
}
```

---

## Phase 8 - API 网关增强

### 8.1 增强 Gateway 路由配置 ✨

**目标：** 实现动态路由、灰度发布、AB 测试

**实施步骤：**

1. **动态路由**
   - 路由规则存储在 Nacos
   - 支持热更新，无需重启
   - 路由匹配规则：Path、Header、Query、Weight

2. **灰度发布**
   - 按用户 ID 分流
   - 按 IP 地址分流
   - 按百分比分流
   - 金丝雀发布

3. **AB 测试**
   - 根据请求头路由到不同版本
   - 流量比例控制
   - 实时监控效果

**预期效果：**
- 灰度发布覆盖率 100%
- 零停机部署
- AB 测试快速验证新特性

**配置示例：**
```yaml
spring:
  cloud:
    gateway:
      routes:
        # 主版本路由
        - id: admin-api-v1
          uri: lb://admin-api
          predicates:
            - Path=/api/v1/**
            - Weight=group1, 90
          filters:
            - StripPrefix=2

        # 灰度版本路由
        - id: admin-api-v2-canary
          uri: lb://admin-api-v2
          predicates:
            - Path=/api/v1/**
            - Weight=group1, 10
          filters:
            - StripPrefix=2
```

### 8.2 实现请求重试和超时控制 ✨

**目标：** 提升系统容错能力，减少瞬时故障影响

**实施步骤：**

1. **重试机制**
   - GET 请求自动重试（幂等）
   - POST/PUT/DELETE 需手动配置
   - 重试次数：3 次
   - 重试间隔：100ms、200ms、500ms（指数退避）

2. **超时控制**
   - 连接超时：3s
   - 读取超时：10s
   - 写入超时：10s
   - 全局超时：30s

3. **断路器集成**
   - 与 Sentinel 集成
   - 失败率 > 50% 触发断路器
   - 半开状态自动恢复

**预期效果：**
- 瞬时故障自动恢复，成功率 > 95%
- 超时请求及时终止，资源不浪费
- 级联故障自动隔离

**配置示例：**
```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 3000
        response-timeout: 10s

      # 全局超时
      default-filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY,GATEWAY_TIMEOUT
            methods: GET
            backoff:
              firstBackoff: 100ms
              maxBackoff: 500ms
              factor: 2

        - name: CircuitBreaker
          args:
            name: default-circuit-breaker
            fallbackUri: forward:/fallback
```

### 8.3 添加 Gateway 限流降级 ✨

**目标：** 保护后端服务，防止过载

**实施步骤：**

1. **全局限流**
   - 整体 QPS 限制：1000
   - 单 IP QPS 限制：100
   - 突发流量允许：20%

2. **接口级限流**
   - 核心接口独立配额
   - 非核心接口共享配额
   - VIP 用户特殊配额

3. **降级策略**
   - 返回默认数据
   - 返回缓存数据
   - 返回友好错误提示

**预期效果：**
- 系统稳定运行，不因流量激增宕机
- 核心功能优先保障
- 用户体验友好

### 8.4 实现 API 版本管理 ✨

**目标：** 支持多版本 API 并存，平滑升级

**实施步骤：**

1. **版本路由**
   - `/api/v1/**` → admin-api-v1
   - `/api/v2/**` → admin-api-v2
   - Header 版本：`X-API-Version: v2`

2. **版本兼容**
   - 旧版本保留 6 个月
   - 新版本向下兼容
   - 废弃版本提前通知

3. **版本监控**
   - 各版本使用率统计
   - 废弃版本使用告警
   - 迁移进度追踪

**预期效果：**
- API 升级不影响现有用户
- 版本管理规范化
- 迁移过程可控

---

## Phase 9 - 服务拆分

### 9.1 规划服务拆分方案 ✨

**目标：** 制定合理的服务拆分策略

**实施步骤：**

1. **服务边界定义**
   - **用户服务** (User Service) - 用户管理、认证、授权
   - **权限服务** (Permission Service) - 角色、权限、菜单管理
   - **业务服务** (Business Service) - 业务逻辑、订单、支付
   - **通知服务** (Notification Service) - 邮件、短信、推送
   - **文件服务** (File Service) - 文件上传、存储、下载

2. **数据库拆分**
   - 每个服务独立数据库
   - 跨服务查询通过 API
   - 数据一致性通过分布式事务（Seata）

3. **服务依赖关系**
   - 核心服务：用户、权限
   - 支撑服务：通知、文件
   - 业务服务：订单、支付

**拆分原则：**
- 单一职责原则
- 高内聚低耦合
- 数据自治
- 服务自治

### 9.2 拆分用户服务 ✨

**目标：** 将用户相关功能独立为微服务

**实施步骤：**

1. **创建用户服务模块**
   - `basebackend-user-service`
   - 端口：8083
   - 数据库：`user_db`

2. **迁移功能**
   - 用户 CRUD
   - 用户认证（登录、登出）
   - Token 管理
   - 用户查询接口

3. **服务间通信**
   - OpenFeign 调用
   - 服务注册到 Nacos
   - 负载均衡

**预期效果：**
- 用户服务独立部署
- 支持水平扩展
- 故障隔离

### 9.3 拆分权限服务 ✨

**目标：** 将权限管理独立为微服务

**实施步骤：**

1. **创建权限服务模块**
   - `basebackend-permission-service`
   - 端口：8084
   - 数据库：`permission_db`

2. **迁移功能**
   - 角色管理
   - 权限管理
   - 菜单管理
   - 权限校验接口

3. **集成网关**
   - 权限拦截器
   - Token 验证
   - 权限缓存（Redis）

**预期效果：**
- 权限服务独立演进
- 统一权限管理
- 高性能权限校验

### 9.4 服务间通信优化 ✨

**目标：** 优化微服务间调用性能和可靠性

**实施步骤：**

1. **OpenFeign 优化**
   - 连接池配置
   - 超时配置
   - 重试配置
   - 日志配置

2. **异步通信**
   - RocketMQ 消息队列
   - 事件驱动架构
   - 最终一致性

3. **缓存优化**
   - 热点数据缓存
   - 多级缓存（本地 + Redis）
   - 缓存预热

**预期效果：**
- 服务间调用延迟 < 50ms
- 异步处理提升吞吐量 3 倍
- 缓存命中率 > 80%

---

## 时间规划

### Phase 7 - 数据库优化（预计 2 周）

| 任务 | 时间 | 优先级 |
|------|------|--------|
| 7.1 读写分离配置 | 3 天 | P0 |
| 7.2 动态数据源切换 | 2 天 | P0 |
| 7.3 连接池优化 | 2 天 | P1 |
| 7.4 慢查询监控 | 3 天 | P1 |
| 测试验证 | 2 天 | P0 |
| 文档编写 | 2 天 | P1 |

### Phase 8 - API 网关增强（预计 2 周）

| 任务 | 时间 | 优先级 |
|------|------|--------|
| 8.1 路由配置增强 | 3 天 | P0 |
| 8.2 重试超时控制 | 2 天 | P0 |
| 8.3 限流降级 | 2 天 | P1 |
| 8.4 版本管理 | 2 天 | P2 |
| 测试验证 | 2 天 | P0 |
| 文档编写 | 2 天 | P1 |

### Phase 9 - 服务拆分（预计 3 周）

| 任务 | 时间 | 优先级 |
|------|------|--------|
| 9.1 拆分方案规划 | 2 天 | P0 |
| 9.2 拆分用户服务 | 5 天 | P0 |
| 9.3 拆分权限服务 | 5 天 | P0 |
| 9.4 服务通信优化 | 3 天 | P1 |
| 集成测试 | 3 天 | P0 |
| 文档编写 | 2 天 | P1 |

**总计：约 7 周（1.5 个月）**

---

## 风险评估

### 技术风险

| 风险 | 等级 | 影响 | 应对措施 |
|------|------|------|----------|
| 读写分离主从延迟 | 中 | 数据不一致 | 强制读主库注解 |
| 服务拆分数据迁移 | 高 | 数据丢失 | 完整备份 + 灰度迁移 |
| 网关性能瓶颈 | 中 | 响应变慢 | 网关集群 + 负载均衡 |
| 分布式事务失败 | 高 | 数据不一致 | Seata AT 模式 + 补偿 |

### 业务风险

| 风险 | 等级 | 影响 | 应对措施 |
|------|------|------|----------|
| 系统停机时间 | 低 | 服务中断 | 灰度发布 + 流量切换 |
| 功能回归问题 | 中 | 用户体验下降 | 充分测试 + 快速回滚 |
| 性能未达预期 | 中 | 资源浪费 | 性能基准测试 + 持续优化 |

---

## 成功标准

### 性能指标

| 指标 | 当前值 | 目标值 | 提升 |
|------|--------|--------|------|
| 数据库 QPS | 500 | 1500 | 200% |
| API 平均响应时间 | 100ms | 60ms | 40% |
| 系统可用性 | 99.5% | 99.9% | 0.4% |
| 并发支持 | 1000 | 5000 | 400% |

### 质量指标

| 指标 | 目标值 |
|------|--------|
| 单元测试覆盖率 | > 70% |
| 集成测试通过率 | 100% |
| 代码审查通过率 | 100% |
| 文档完整性 | 100% |

### 业务指标

| 指标 | 目标值 |
|------|--------|
| 功能完整性 | 100% |
| 用户体验评分 | > 4.5/5 |
| 故障恢复时间 | < 5 分钟 |
| 系统扩展能力 | 支持 5 倍流量 |

---

## 总结

浮浮酱精心规划了中期优化方案喵～ (๑ˉ∀ˉ๑)

**核心价值：**
- 📊 **数据库优化** - 读写分离提升性能 3 倍
- 🚀 **网关增强** - 重试、超时、版本管理完善
- 🎯 **服务拆分** - 解耦业务，支持独立演进

**实施原则：**
1. **循序渐进** - 按优先级逐步实施
2. **充分测试** - 每个阶段都要验证
3. **文档先行** - 详细记录方案和变更
4. **灰度发布** - 降低风险，平滑过渡

让我们从 Phase 7 - 数据库优化开始吧～ φ(≧ω≦*)♪

---

*最后更新: 2025-01-13*
*作者: 浮浮酱（猫娘工程师）ฅ'ω'ฅ*
*项目: BaseBackend - 中期架构优化计划*
