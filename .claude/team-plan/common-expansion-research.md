# Team Research: basebackend-common 模块扩展

## 增强后的需求

**目标**：对 basebackend-common 进行全面扩展，将跨业务模块重复实现的通用能力抽象下沉为 common 子模块，消除碎片化，统一企业级基础能力。

**技术约束**：Java 17 / Spring Boot 3.1.5 / MyBatis Plus 3.5.5，新子模块放入 basebackend-common 聚合内部（与 common-core, common-util 同级）。

**范围边界**：仅涉及通用抽象层，不涉及具体中间件绑定（如 RocketMQ、Sentinel 的具体实现留在各自模块）。

**验收标准**：各业务模块可通过依赖新子模块替代自身重复实现，且不引入循环依赖。

---

## 约束集

### 硬约束

- [HC-1] 新子模块必须为 basebackend-common 的 `<module>` 子项，artifactId 遵循 `basebackend-common-{name}` 命名 — 来源：用户决策
- [HC-2] 不可引入 Spring Cloud 级别依赖（如 Sentinel、Nacos SDK），common 层仅依赖 Spring Boot core/web — 来源：现有架构约束（common-core 的 pom 无任何 cloud 依赖）
- [HC-3] 不可与 basebackend-cache、basebackend-database、basebackend-logging 等已有基础设施模块形成循环依赖 — 来源：Maven 模块图
- [HC-4] 所有新子模块必须兼容现有 MyBatis Plus 3.5.5 + Lombok 注解驱动风格 — 来源：项目编码规范
- [HC-5] common-storage 已存在（15个文件），存储相关扩展应在其内部进行而非新建 — 来源：现有模块

### 软约束

- [SC-1] 新抽象层应提供 SPI 接口 + 默认实现模式，参考 common-storage 的 StorageProvider SPI 设计 — 来源：代码库惯例
- [SC-2] 自动配置类遵循 `*AutoConfiguration` 命名，配置属性遵循 `basebackend.common.{feature}.*` 前缀 — 来源：common-starter 惯例
- [SC-3] 测试至少覆盖核心注解处理器和工具类 — 来源：项目测试策略（LINE >= 30%）
- [SC-4] 尽量使用注解驱动（`@Annotation` + AOP Aspect），降低业务代码侵入性 — 来源：现有 @Sensitive, @RateLimit 等模式

### 依赖关系

- [DEP-1] common-core → 所有新子模块（新子模块依赖 Result, BusinessException, ErrorCode 基类）
- [DEP-2] common-util → common-masking（脱敏可能依赖 StringUtils）
- [DEP-3] common-starter → 所有新子模块的 AutoConfiguration 注册
- [DEP-4] 新子模块 → 不可反向依赖 admin-api/user-api/system-api 等业务模块

### 风险

- [RISK-1] 脱敏统一化可能与 basebackend-database 的 @Sensitive 注解和 basebackend-logging 的 PiiMaskingService 产生职责重叠 — 缓解：common-masking 仅提供核心策略接口和通用实现，database/logging 各自适配
- [RISK-2] 限流抽象下沉可能与 gateway 的 Sentinel 和 web 的 RateLimitAspect 冲突 — 缓解：common 仅定义 @RateLimit 注解和 RateLimiter SPI，不绑定具体实现
- [RISK-3] 审计日志统一后需要 Feign 调用链路支持（user-api → system-api 的 OperationLogFeignClient 已存在） — 缓解：common-audit 仅提供注解和本地事件发布，远程持久化由各服务自行处理

---

## 扩展方向详述（6个新子模块）

### 方向 1: `common-masking` — 数据脱敏统一抽象

**碎片化现状**（20+ 文件，分布在 5 个模块）：
| 模块 | 实现 | 文件 |
|------|------|------|
| basebackend-database | `@Sensitive` 注解 + `MaskingUtil` + `MaskingConfig` + `PermissionMaskingInterceptor` | 6 files |
| basebackend-logging | `PiiMaskingService` + `MaskingProperties` | 2 files |
| basebackend-observability | `EnhancedMaskingService` + `MaskingStrategy` + `MaskingConverter` | 3 files |
| basebackend-user-api | SecurityServiceImpl 内嵌脱敏逻辑 | 1 file |
| basebackend-notification | NotificationServiceImpl 内嵌脱敏逻辑 | 1 file |

**建议能力**：
- 统一脱敏策略枚举（PHONE, EMAIL, ID_CARD, BANK_CARD, ADDRESS, CUSTOM）
- `@Mask` / `@Sensitive` 统一注解
- `MaskingStrategy` SPI 接口 + 默认实现
- Jackson 序列化脱敏（`@JsonSerialize` 集成）
- 估计文件数：6-8 files

---

### 方向 2: `common-ratelimit` — 限流抽象层

**碎片化现状**（20+ 文件，分布在 5 个模块）：
| 模块 | 实现 |
|------|------|
| basebackend-gateway | `SentinelGatewayRuleConfig` + `RateLimitRuleManager` |
| basebackend-security | `AuthenticationRateLimiter` |
| basebackend-web | `RateLimitAspect` |
| basebackend-notification | `NotificationRateLimiter` |
| basebackend-file-service | `RateLimitPolicy` + `RedisRateLimiter` |

**建议能力**：
- `@RateLimit` 统一注解（key, limit, window, strategy）
- `RateLimiter` SPI 接口（支持本地/Redis/Sentinel 适配）
- 默认本地滑动窗口实现（无外部依赖）
- 限流降级回调 `RateLimitFallback`
- 估计文件数：5-7 files

---

### 方向 3: `common-audit` — 审计日志统一框架

**碎片化现状**（30+ 文件，分布在 6 个模块）：
| 模块 | 实现 |
|------|------|
| basebackend-database | `AuditInterceptor` (MyBatis 级) |
| basebackend-user-api | `UserOperationLogServiceImpl` + `SecurityServiceImpl` |
| basebackend-system-api | `OperationLogService` + `OperationLogInternalController` |
| basebackend-file-service | `AuditService` + `FileShareAuditLogMapper` |
| basebackend-scheduler | `AuditLogService` + `AuditLogEntity` + `AuditLogMapper` |
| basebackend-feign-api | `OperationLogFeignClient` + `Fallback` |

**建议能力**：
- `@AuditLog` 统一注解（module, action, description, recordParams）
- `AuditEvent` 标准事件模型
- `AuditEventPublisher` 本地事件发布（Spring ApplicationEvent）
- `AuditEventListener` SPI（由各业务模块自行实现持久化）
- SpEL 表达式支持动态描述
- 估计文件数：6-8 files

---

### 方向 4: `common-event` — 统一领域事件抽象

**碎片化现状**（16 文件，分布在 5 个模块）：
| 模块 | 实现 |
|------|------|
| basebackend-nacos | `ConfigChangeEvent` + `GrayReleaseHistoryEvent` + `ConfigChangeListener` |
| basebackend-messaging | `EventPublisher` |
| basebackend-common-security | `SecretRotationEvent` + `SecretRotationListener` |
| basebackend-database | `TenantDataSourceConfig`（内嵌事件） |
| basebackend-cache | `CacheWarmingManager`（内嵌事件） |

**建议能力**：
- `DomainEvent` 基类（eventId, timestamp, source, type）
- `DomainEventPublisher` 统一发布接口
- `@DomainEventListener` 注解
- 本地事件总线（基于 Spring ApplicationEventPublisher）
- 事件序列化 / 审计追踪支持
- 估计文件数：5-7 files

---

### 方向 5: `common-tree` — 树形结构工具

**碎片化现状**（5 文件，分布在 3 个模块）：
| 模块 | 实现 |
|------|------|
| basebackend-admin-api | `MenuController` 树构建 + `ApplicationResourceServiceImpl` |
| basebackend-system-api | `ApplicationResourceController` + `ApplicationResourceServiceImpl` |
| basebackend-user-api | `RoleServiceImpl` 树构建 |

**建议能力**：
- `TreeNode<T>` 泛型接口（id, parentId, children, sort）
- `TreeBuilder` 工具类（list → tree, tree → flat, 查找子树, 路径追溯）
- `@TreeField` 注解（标记 id/parentId/children/sort 字段）
- 支持递归和非递归两种构建算法
- 估计文件数：3-5 files

---

### 方向 6: `common-export` — 数据导入导出

**现状**：完全空白（0 文件），项目中无任何 Excel/CSV 导入导出能力。

**建议能力**：
- `@ExportField` 注解（label, order, format, converter）
- `ExportService` SPI（支持 Excel/CSV）
- 默认 EasyExcel 实现（optional 依赖）
- `ImportService` SPI + 校验框架
- 导入错误行收集 + 结果报告
- 大数据量流式导出支持
- 估计文件数：8-12 files

---

## 成功判据

- [OK-1] 各业务模块（admin-api, user-api, system-api, file-service, notification-service）可通过 Maven 依赖新子模块替代自身重复实现
- [OK-2] 新子模块编译通过，不引入循环依赖（`mvn clean install -pl basebackend-common -am` 成功）
- [OK-3] 每个新子模块至少包含 1 个核心注解 + 1 个 SPI 接口 + 1 个默认实现
- [OK-4] 脱敏、限流、审计日志的注解可在 Controller/Service 层直接使用（AOP 驱动）
- [OK-5] 树形结构工具可替代 admin-api 和 system-api 中的手动树构建代码
- [OK-6] 不破坏现有功能（所有现有测试继续通过）

## 开放问题（已解决）

- Q1: 扩展优先级？ → A: 全部都要 → 约束：6 个方向全部纳入研究范围
- Q2: 模块归属？ → A: 放入 common 内部 → 约束：[HC-1] artifactId = `basebackend-common-{name}`

---

## 扩展总览（新增 6 子模块后的 common 结构）

```
basebackend-common/
├── basebackend-common-core          (已有) 核心基类
├── basebackend-common-dto           (已有) 共享DTO
├── basebackend-common-util          (已有) 通用工具
├── basebackend-common-context       (已有) 上下文传递
├── basebackend-common-security      (已有) 安全基础
├── basebackend-common-starter       (已有) 自动配置
├── basebackend-common-storage       (已有) 存储抽象
├── basebackend-common-masking       (新增) 数据脱敏统一抽象
├── basebackend-common-ratelimit     (新增) 限流抽象层
├── basebackend-common-audit         (新增) 审计日志框架
├── basebackend-common-event         (新增) 领域事件抽象
├── basebackend-common-tree          (新增) 树形结构工具
└── basebackend-common-export        (新增) 数据导入导出
```

## 建议实施优先级

| 优先级 | 子模块 | 理由 |
|--------|--------|------|
| P0 | common-masking | 碎片化最严重（5模块20+文件），安全合规刚需 |
| P0 | common-audit | 碎片化严重（6模块30+文件），企业级刚需 |
| P1 | common-ratelimit | 碎片化严重（5模块20+文件），性能保护刚需 |
| P1 | common-event | 模块间解耦的基础设施，影响后续架构演进 |
| P2 | common-tree | 重复度中等（3模块5文件），但实现简单收益明确 |
| P2 | common-export | 完全空白能力，企业后台系统标配 |
