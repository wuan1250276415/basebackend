# 异常处理优化指南

## 概述

本文档介绍 basebackend-scheduler 模块的异常处理机制优化，包括统一异常处理器、错误码映射、工具类等核心组件。

## 架构设计

### 核心组件

1. **GlobalExceptionHandler** - 统一全局异常处理器
   - 位置：`src/main/java/com/basebackend/scheduler/exception/GlobalExceptionHandler.java`
   - 职责：处理所有模块异常，统一返回格式

2. **ExceptionMapping** - 异常映射配置
   - 位置：`src/main/java/com/basebackend/scheduler/exception/ExceptionMapping.java`
   - 职责：定义错误码与HTTP状态码的映射关系

3. **ExceptionUtils** - 异常处理工具类
   - 位置：`src/main/java/com/basebackend/scheduler/exception/ExceptionUtils.java`
   - 职责：提供异常处理的常用工具方法

## 特性

### 1. 统一响应格式

所有异常统一使用 `Result` 格式返回：

```json
{
  "code": 400,
  "message": "参数校验失败: username: 用户名不能为空",
  "data": null,
  "timestamp": 1704038400000
}
```

### 2. 智能错误码映射

支持精确匹配和前缀匹配：

```java
// 精确匹配
"VALIDATION_FAILED" -> 400

// 前缀匹配
"PARAM_" -> 400
"INTERNAL_" -> 500
"ENGINE_" -> 500
```

### 3. 追踪ID支持

自动提取和生成追踪ID：

- 优先级：X-Trace-Id > X-Request-Id > 请求属性 > UUID
- 用于日志关联和故障排查

### 4. 日志级别控制

根据异常严重程度自动选择日志级别：

- **ERROR**：服务器错误（5xx）、数据库错误、系统错误
- **WARN**：业务错误（4xx）、参数验证失败

### 5. 敏感信息过滤

自动过滤异常消息中的敏感信息：

- 数据库连接信息
- SQL语句
- 文件路径
- 内部系统信息

## 使用指南

### 1. 抛出业务异常

```java
// Camunda相关异常
throw new CamundaServiceException("CAMUNDA_001", "流程实例不存在");

// 工作流异常
throw new WorkflowException("WORKFLOW_001", "流程定义已挂起");

// 调度器异常
throw new SchedulerException(SchedulerErrorCode.DATA_NOT_FOUND, "数据不存在");
```

### 2. 自定义错误码

在相应的错误码枚举中添加新错误码：

```java
public enum WorkflowErrorCode {
    CUSTOM_ERROR("CUSTOM_001", "自定义错误", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
```

### 3. 异常映射规则

如需添加新的错误码映射，在 `ExceptionMapping` 类中修改：

```java
// 精确匹配
mapping.put("MY_ERROR_CODE", "422");

// 前缀匹配
rules.put("MY_PREFIX_", "422");
```

### 4. 异常分类

异常按严重程度分为三类：

- **HIGH**：服务器内部错误、数据库错误、工作流引擎错误
- **MEDIUM**：冲突、权限问题
- **LOW**：参数错误、业务逻辑错误

## 处理流程

```
客户端请求
    ↓
Controller层
    ↓
抛出异常
    ↓
GlobalExceptionHandler 捕获
    ↓
判断异常类型
    ↓
提取错误码和消息
    ↓
应用映射规则
    ↓
构建 Result 响应
    ↓
返回给客户端
```

## 覆盖的异常类型

### 业务异常
- `CamundaServiceException`
- `WorkflowException`
- `SchedulerException`

### 参数验证异常
- `MethodArgumentNotValidException`
- `BindException`
- `MethodArgumentTypeMismatchException`
- `MissingServletRequestParameterException`
- `HttpMessageNotReadableException`

### 系统异常
- `ProcessEngineException`
- `OptimisticLockingException`
- `IllegalArgumentException`
- `IllegalStateException`

### 兜底异常
- `Exception` - 所有未捕获的异常

## 最佳实践

### 1. 错误码设计原则

- 使用有意义的名称，如 `VALIDATION_FAILED` 而不是 `ERROR_001`
- 保持一致性，统一前缀和命名规范
- 区分客户端错误（4xx）和服务器错误（5xx）

### 2. 异常消息规范

- 用户友好：避免技术术语，使用通俗易懂的语言
- 准确：准确描述错误原因
- 简洁：避免过长消息
- 安全：不泄露敏感信息

### 3. 日志记录规范

- 高优先级异常使用 ERROR 级别
- 包含 traceId 便于追踪
- 记录必要上下文信息
- 避免在日志中记录敏感数据

### 4. 测试建议

编写异常处理测试：

```java
@Test
public void testCamundaServiceExceptionHandling() {
    // 测试 CamundaServiceException 的处理
}

@Test
public void testValidationExceptionHandling() {
    // 测试参数验证异常的处理
}
```

## 配置说明

### 优先级设置

全局异常处理器设置了最高优先级：

```java
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
```

确保在其他异常处理器之前执行。

### 响应格式配置

使用 `Result` 作为统一响应格式，来自 `basebackend-common` 模块。

## 迁移指南

### 从旧版本迁移

如果你之前使用的是分离的异常处理器：

1. 移除 `SchedulerGlobalExceptionHandler`
2. 移除 `WorkflowGlobalExceptionHandler`
3. 确保所有 Controller 使用 `Result` 格式
4. 测试异常处理流程

### 向后兼容性

- 错误码格式保持不变
- HTTP状态码映射保持一致
- 响应格式统一为 `Result`

## 常见问题

### Q: 如何添加新的异常类型？

A: 在 `GlobalExceptionHandler` 中添加对应的 `@ExceptionHandler` 方法。

### Q: 如何自定义错误码映射？

A: 修改 `ExceptionMapping` 类中的 `EXACT_MATCH_MAPPING` 或 `PREFIX_MATCH_RULES`。

### Q: 如何调整日志级别？

A: 修改 `isServerError` 方法中的判断逻辑。

### Q: 如何禁用敏感信息过滤？

A: 修改 `sanitizeMessage` 方法，或创建配置选项。

## 版本信息

- 版本：1.0.0
- 创建日期：2025-01-01
- 作者：BaseBackend Team
