# Admin-API 服务拆分执行计划

> **执行分支**: feature/admin-api-splitting  
> **开始日期**: 2025-11-18  
> **预计完成**: 2-3周

---

## 一、拆分目标

根据 `ADMIN_API_SPLITTING_ANALYSIS.md` 的分析，将 basebackend-admin-api 拆分为以下微服务：

1. ✅ **用户认证与权限服务** (basebackend-user-auth-service) - 已存在user-api
2. ✅ **用户与组织服务** (basebackend-user-organization-service) - 已存在user-api和system-api
3. ✅ **系统配置服务** (basebackend-system-config-service) - 已存在system-api
4. 🆕 **通知中心服务** (basebackend-notification-service) - 需要新建
5. 🆕 **文件存储服务** (basebackend-file-storage-service) - 已存在file-service
6. 🆕 **可观测性服务** (basebackend-observability-service) - 需要新建
7. 🆕 **配置中心服务** (basebackend-config-center-service) - 可选

## 二、当前状态分析

### 2.1 已存在的微服务

| 服务 | 状态 | 说明 |
|------|------|------|
| basebackend-user-api | ✅ 已完成 | 包含用户、角色、权限、认证功能 |
| basebackend-system-api | ✅ 已完成 | 包含部门、菜单、字典功能 |
| basebackend-auth-api | ✅ 已完成 | 独立的认证服务 |
| basebackend-file-service | ✅ 已存在 | 文件服务模块 |

### 2.2 需要新建的微服务

| 服务 | 优先级 | 说明 |
|------|--------|------|
| basebackend-notification-service | ⭐⭐⭐⭐⭐ | 通知中心 |
| basebackend-observability-service | ⭐⭐⭐⭐⭐ | 可观测性服务 |

### 2.3 需要提取的公共功能

| 功能 | 当前位置 | 目标位置 | 优先级 |
|------|----------|----------|--------|
| OperationLogAspect | admin-api | basebackend-logging | ⭐⭐⭐⭐⭐ |
| PermissionAspect | admin-api | basebackend-security | ⭐⭐⭐⭐⭐ |
| 注解类 | admin-api | basebackend-security | ⭐⭐⭐⭐⭐ |

## 三、执行计划

### Phase 1: 公共功能提取（1-2天）

#### Day 1: 提取AOP切面和注解

**任务1.1: 提取OperationLogAspect到basebackend-logging**
- [ ] 复制OperationLogAspect到basebackend-logging
- [ ] 调整包名和依赖
- [ ] 更新所有引用
- [ ] 测试功能

**任务1.2: 提取PermissionAspect到basebackend-security**
- [ ] 复制PermissionAspect到basebackend-security
- [ ] 调整包名和依赖
- [ ] 更新所有引用
- [ ] 测试功能

**任务1.3: 提取注解类到basebackend-security**
- [ ] 复制RequiresPermission、RequiresRole、DataScope
- [ ] 调整包名
- [ ] 更新所有引用

### Phase 2: 创建通知中心服务（2-3天）

#### Day 2-3: 创建basebackend-notification-service

**任务2.1: 创建项目结构**
```bash
basebackend-notification-service/
├── src/main/java/com/basebackend/notification/
│   ├── NotificationServiceApplication.java
│   ├── controller/
│   │   ├── NotificationController.java
│   │   └── WebhookController.java
│   ├── service/
│   │   ├── NotificationService.java
│   │   ├── SSENotificationService.java
│   │   └── WebhookService.java
│   ├── entity/
│   │   ├── UserNotification.java
│   │   ├── SysWebhookConfig.java
│   │   └── SysWebhookLog.java
│   ├── dto/
│   │   └── NotificationDTO.java
│   ├── mapper/
│   │   └── NotificationMapper.java
│   └── consumer/
│       └── NotificationConsumer.java
└── src/main/resources/
    ├── application.yml
    ├── bootstrap.yml
    └── mapper/
```

**任务2.2: 从admin-api迁移代码**
- [ ] 迁移NotificationController
- [ ] 迁移NotificationService
- [ ] 迁移WebhookController和Service
- [ ] 迁移Entity和DTO
- [ ] 迁移Mapper和XML

**任务2.3: 配置和测试**
- [ ] 配置Nacos注册
- [ ] 配置RocketMQ
- [ ] 配置数据库
- [ ] 编写单元测试
- [ ] 编写集成测试

### Phase 3: 创建可观测性服务（2-3天）

#### Day 4-5: 创建basebackend-observability-service

**任务3.1: 创建项目结构**
```bash
basebackend-observability-service/
├── src/main/java/com/basebackend/observability/
│   ├── ObservabilityServiceApplication.java
│   ├── controller/
│   │   ├── MetricsController.java
│   │   ├── LogController.java
│   │   ├── TraceController.java
│   │   └── AlertController.java
│   ├── service/
│   │   ├── MetricsQueryService.java
│   │   ├── LogQueryService.java
│   │   ├── TraceQueryService.java
│   │   └── AlertManagementService.java
│   ├── entity/
│   │   └── Alert.java
│   └── dto/
│       ├── MetricsDTO.java
│       └── AlertDTO.java
└── src/main/resources/
    ├── application.yml
    └── bootstrap.yml
```

**任务3.2: 从admin-api迁移代码**
- [ ] 迁移observability包下的所有Controller
- [ ] 迁移observability包下的所有Service
- [ ] 迁移相关Entity和DTO
- [ ] 配置Prometheus集成
- [ ] 配置Grafana集成

**任务3.3: 配置和测试**
- [ ] 配置Nacos注册
- [ ] 配置Prometheus
- [ ] 配置数据库
- [ ] 编写单元测试
- [ ] 编写集成测试

### Phase 4: 整合和优化（2-3天）

#### Day 6-7: 整合现有服务

**任务4.1: 检查user-api**
- [ ] 确认包含所有用户认证功能
- [ ] 确认包含所有权限管理功能
- [ ] 补充缺失的功能

**任务4.2: 检查system-api**
- [ ] 确认包含所有部门管理功能
- [ ] 确认包含所有菜单管理功能
- [ ] 确认包含所有字典管理功能
- [ ] 补充缺失的功能

**任务4.3: 检查file-service**
- [ ] 确认文件上传下载功能
- [ ] 确认文件管理功能
- [ ] 补充缺失的功能

#### Day 8: 更新网关路由

**任务4.4: 配置Gateway路由**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: notification-service
          uri: lb://basebackend-notification-service
          predicates:
            - Path=/api/notifications/**,/api/webhooks/**
        
        - id: observability-service
          uri: lb://basebackend-observability-service
          predicates:
            - Path=/api/metrics/**,/api/logs/**,/api/traces/**,/api/alerts/**
```

### Phase 5: 测试和文档（1-2天）

#### Day 9-10: 全面测试

**任务5.1: 功能测试**
- [ ] 测试通知服务
- [ ] 测试可观测性服务
- [ ] 测试服务间调用
- [ ] 测试网关路由

**任务5.2: 性能测试**
- [ ] 压力测试
- [ ] 并发测试
- [ ] 响应时间测试

**任务5.3: 文档更新**
- [ ] 更新API文档
- [ ] 更新部署文档
- [ ] 更新架构图
- [ ] 编写迁移指南

## 四、详细任务清单

### 4.1 通知中心服务迁移清单

#### 从admin-api迁移的文件

**Controller:**
```
basebackend-admin-api/controller/NotificationController.java
  → basebackend-notification-service/controller/NotificationController.java

basebackend-admin-api/controller/messaging/WebhookConfigController.java
  → basebackend-notification-service/controller/WebhookController.java

basebackend-admin-api/controller/messaging/WebhookLogController.java
  → basebackend-notification-service/controller/WebhookLogController.java

basebackend-admin-api/controller/messaging/DeadLetterController.java
  → basebackend-notification-service/controller/DeadLetterController.java
```

**Service:**
```
basebackend-admin-api/service/NotificationService.java
  → basebackend-notification-service/service/NotificationService.java

basebackend-admin-api/service/SSENotificationService.java
  → basebackend-notification-service/service/SSENotificationService.java

basebackend-admin-api/service/messaging/WebhookConfigService.java
  → basebackend-notification-service/service/WebhookService.java
```

**Entity:**
```
basebackend-admin-api/entity/UserNotification.java
  → basebackend-notification-service/entity/UserNotification.java

basebackend-admin-api/entity/messaging/SysWebhookConfig.java
  → basebackend-notification-service/entity/SysWebhookConfig.java

basebackend-admin-api/entity/messaging/SysWebhookLog.java
  → basebackend-notification-service/entity/SysWebhookLog.java
```

**Consumer:**
```
basebackend-admin-api/consumer/NotificationConsumer.java
  → basebackend-notification-service/consumer/NotificationConsumer.java
```

### 4.2 可观测性服务迁移清单

#### 从admin-api迁移的文件

**Controller:**
```
basebackend-admin-api/controller/observability/MetricsController.java
  → basebackend-observability-service/controller/MetricsController.java

basebackend-admin-api/controller/observability/LogController.java
  → basebackend-observability-service/controller/LogController.java

basebackend-admin-api/controller/observability/TraceController.java
  → basebackend-observability-service/controller/TraceController.java

basebackend-admin-api/controller/observability/AlertController.java
  → basebackend-observability-service/controller/AlertController.java
```

**Service:**
```
basebackend-admin-api/service/observability/MetricsQueryService.java
  → basebackend-observability-service/service/MetricsQueryService.java

basebackend-admin-api/service/observability/LogQueryService.java
  → basebackend-observability-service/service/LogQueryService.java
```

## 五、风险和注意事项

### 5.1 技术风险

⚠️ **数据一致性**
- 通知服务需要访问用户信息
- 解决方案：通过Feign调用user-api

⚠️ **性能影响**
- 服务间调用增加网络开销
- 解决方案：添加本地缓存

⚠️ **事务一致性**
- 跨服务事务需要特殊处理
- 解决方案：使用最终一致性或Seata

### 5.2 运维风险

⚠️ **监控复杂度**
- 需要监控更多服务
- 解决方案：完善监控告警系统

⚠️ **部署复杂度**
- 部署步骤增加
- 解决方案：使用Docker Compose或K8s

## 六、成功标准

### 6.1 功能标准
- [ ] 所有API功能正常
- [ ] 服务间调用正常
- [ ] 数据一致性保证

### 6.2 性能标准
- [ ] 响应时间 < 200ms (95%)
- [ ] 错误率 < 0.1%
- [ ] 并发支持 > 1000 QPS

### 6.3 质量标准
- [ ] 单元测试覆盖率 > 80%
- [ ] 集成测试通过
- [ ] 文档完整

## 七、回滚计划

如果拆分出现问题，可以：

1. **保留admin-api** - 作为备份
2. **网关切换** - 快速切回admin-api
3. **数据回滚** - 恢复数据库

## 八、下一步行动

### 立即执行
1. ✅ 创建feature/admin-api-splitting分支
2. 📝 创建通知中心服务项目结构
3. 📝 创建可观测性服务项目结构
4. 🔧 提取公共AOP切面

### 本周完成
- 完成通知中心服务基础功能
- 完成可观测性服务基础功能
- 完成公共功能提取

### 下周完成
- 完成所有服务的集成测试
- 完成文档更新
- 完成部署配置

---

**文档版本**: v1.0  
**创建时间**: 2025-11-18  
**负责人**: 架构团队
