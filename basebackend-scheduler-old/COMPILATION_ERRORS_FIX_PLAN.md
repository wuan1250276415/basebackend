# BaseBackend-Scheduler 编译错误修复计划

## 错误统计
总计：**100个编译错误**

## 问题分类

### 1. Micrometer Gauge API 使用错误 (7个错误)
**文件**:
- `WorkflowMetrics.java` (3个)
- `BusinessMetrics.java` (4个)

**问题**: `Gauge.builder()` 方法缺少必需参数
```java
// 错误写法
Gauge.builder("metric.name")

// 正确写法
Gauge.builder("metric.name", this, obj -> getValue())
```

**修复**: 需要提供对象实例和 ToDoubleFunction

### 2. Camunda API 版本不兼容 (15+个错误)
**涉及文件**:
- `TaskController.java`
- `ProcessStatisticsServiceImpl.java`
- `HistoricProcessInstanceServiceImpl.java`
- `ProcessInstanceController.java`

**问题**:
- `taskTenantId()` 方法不存在 → 使用 `tenantIdIn()`
- `unclaim()` 方法不存在 → 使用 `setAssignee(null)`
- `deleteReasonLike()` 方法不存在 → 使用其他查询方法
- `includeProcessVariables()` 方法不存在
- `taskCreatedAfter()` 接受 Date 而非 Instant
- `startedBy()` 方法不存在

### 3. Date/Instant 类型转换问题 (10+个错误)
**问题**: Camunda API 使用 `java.util.Date`，代码使用 `java.time.Instant`

**修复**: 需要转换
```java
// Instant to Date
Date.from(instant)

// Date to Instant  
date.toInstant()
```

### 4. DTO 类缺少 getter/setter 方法 (40+个错误)
**涉及文件**:
- `FormTemplatePageQuery.java`
- `FormTemplateCreateRequest.java`
- `FormTemplateUpdateRequest.java`
- `FormTemplateEntity.java`
- `TaskStatisticsDTO.java`
- `HistoricProcessInstanceDTO.java`
- `ProcessInstanceDTO.java`
- `ProcessInstanceDetailDTO.java`
- `ProcessVariableDTO.java`
- `HistoricVariableInstanceDTO.java`

**问题**: 缺少 Lombok 注解或字段定义

**修复**: 添加 `@Data` 注解或手动添加 getter/setter

### 5. CommonErrorCode 枚举缺少常量 (5个错误)
**文件**: `FormTemplateController.java`

**缺少的常量**:
- `RESOURCE_NOT_FOUND`
- `RESOURCE_ALREADY_EXISTS`

**修复**: 在 `basebackend-common` 模块的 `CommonErrorCode` 中添加这些常量

### 6. int 到 Long/String 类型转换 (10+个错误)
**问题**: 将 `int` 类型赋值给 `Long` 或 `String` 类型字段

**修复**: 
```java
// int to Long
Long.valueOf(intValue)

// int to String
String.valueOf(intValue)
```

### 7. MonitoringInterceptor 类型错误 (2个错误)
**文件**: `MonitoringInterceptor.java`

**问题**: 传递 `Class` 对象给需要 `MeterRegistry` 的方法

**修复**: 需要注入 `MeterRegistry` 实例而非 Class

### 8. Jakarta Mail 依赖缺失 (1个错误)
**文件**: `SendEmailDelegate.java`

**状态**: ✅ 已修复 - 添加了 `spring-boot-starter-mail` 依赖

## 修复优先级

### 高优先级 (阻塞编译)
1. ✅ 添加 Jakarta Mail 依赖
2. 修复 CommonErrorCode 缺失常量
3. 修复 DTO 类缺少的 getter/setter

### 中优先级 (功能性问题)
4. 修复 Camunda API 不兼容问题
5. 修复 Date/Instant 类型转换
6. 修复 int 到 Long/String 转换

### 低优先级 (优化)
7. 修复 Micrometer Gauge API 使用
8. 修复 MonitoringInterceptor 类型问题

## 建议的修复策略

### 方案 A: 逐个修复 (推荐用于生产环境)
1. 先修复依赖问题
2. 修复 DTO 类
3. 修复 Camunda API 兼容性
4. 修复类型转换问题
5. 修复监控指标问题

### 方案 B: 临时禁用 (快速解决)
1. 注释掉有问题的功能模块
2. 先让项目编译通过
3. 后续逐步修复

### 方案 C: 降级依赖版本
1. 降级 Camunda 到兼容版本
2. 调整 Micrometer 版本

## 下一步行动

由于错误数量巨大，建议：

1. **立即行动**: 修复 CommonErrorCode 缺失常量
2. **批量修复**: 使用 IDE 的批量重构功能修复 DTO 类
3. **API 适配**: 创建 Camunda API 适配层，统一处理版本差异
4. **类型转换**: 创建工具类处理 Date/Instant 转换

## 预计工作量

- 修复 CommonErrorCode: 5分钟
- 修复 DTO 类: 30分钟
- 修复 Camunda API: 1-2小时
- 修复类型转换: 30分钟
- 修复监控指标: 30分钟
- 测试验证: 1小时

**总计**: 约 4-5小时

## 注意事项

1. 修复前建议创建 Git 分支
2. 每修复一类问题后进行编译验证
3. 优先修复阻塞性问题
4. 保持代码风格一致性
5. 添加必要的注释说明修改原因
