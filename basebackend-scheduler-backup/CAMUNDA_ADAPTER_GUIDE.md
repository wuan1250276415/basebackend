# Camunda API 适配层指南

## 概述

Camunda API 适配层是对 Camunda 引擎原生 API 的封装，旨在提供更简洁、更易用的接口。它隐藏了 Camunda API 的复杂性，提供了统一的操作方式，并增强了错误处理和日志记录功能。

## 设计原则

### 1. 简化复杂操作

封装常用的多步操作，提供默认值和智能处理。

### 2. 统一异常处理

将 Camunda 原生异常转换为项目统一的异常类型。

### 3. 支持链式调用

提供流畅的 API 体验，支持链式调用。

### 4. 完整的日志记录

自动记录关键操作，便于调试和监控。

### 5. 泛型支持

支持泛型类型转换，减少类型转换代码。

## 核心组件

### CamundaAdapter

主要适配器类，提供对 Camunda 引擎的访问入口。

**位置**：`src/main/java/com/basebackend/scheduler/camunda/adapter/CamundaAdapter.java`

**主要方法**：
- `runtime()` - 获取流程实例运行时操作接口
- `task()` - 获取任务操作接口
- `definition()` - 获取流程定义操作接口
- `history()` - 获取历史数据查询接口

## 使用示例

### 1. 启动流程实例

```java
// 基础启动
String instanceId = camundaAdapter.runtime()
        .startProcessByKey("order_approval");

// 带业务键启动
String instanceId = camundaAdapter.runtime()
        .startProcessByKey("order_approval", "ORDER_12345");

// 带变量启动
Map<String, Object> variables = new HashMap<>();
variables.put("orderId", "12345");
variables.put("amount", 1000.0);

String instanceId = camundaAdapter.runtime()
        .startProcessByKey("order_approval", variables);

// 完整参数启动
String instanceId = camundaAdapter.runtime()
        .startProcessByKey("order_approval", "ORDER_12345", variables);
```

### 2. 任务操作

```java
// 认领任务
camundaAdapter.task().claim("TASK_ID_12345", "alice@example.com");

// 完成任务
camundaAdapter.task().complete("TASK_ID_12345");

// 带变量完成任务
Map<String, Object> variables = new HashMap<>();
variables.put("approved", true);
variables.put("comment", "审批通过");
camundaAdapter.task().complete("TASK_ID_12345", variables);

// 委托任务
camundaAdapter.task().delegate("TASK_ID_12345", "bob@example.com");

// 释放任务
camundaAdapter.task().unclaim("TASK_ID_12345");
```

### 3. 变量操作

```java
// 设置单个变量
camundaAdapter.runtime().setVariable("INSTANCE_ID", "status", "IN_PROGRESS");

// 批量设置变量
Map<String, Object> variables = new HashMap<>();
variables.put("status", "IN_PROGRESS");
variables.put("updatedBy", "system");
camundaAdapter.runtime().setVariables("INSTANCE_ID", variables);

// 获取变量
String status = camundaAdapter.runtime()
        .getVariable("INSTANCE_ID", "status", String.class);
```

### 4. 查询操作

```java
// 查询待办任务
List<Task> tasks = camundaAdapter.task()
        .findByAssignee("alice@example.com");

// 查询候选任务
List<Task> candidateTasks = camundaAdapter.task()
        .findByCandidateUser("alice@example.com");

// 查询组任务
List<Task> groupTasks = camundaAdapter.task()
        .findByCandidateGroup("managers");

// 查询流程实例
List<ProcessInstance> instances = camundaAdapter.runtime()
        .findByDefinitionKey("order_approval");

// 查询历史数据
List<HistoricProcessInstance> history = camundaAdapter.history()
        .findHistoricProcessInstancesByBusinessKey("ORDER_12345");
```

### 5. 查询对象使用

```java
// 复杂查询（使用 Query 对象）
TaskQuery query = camundaAdapter.task()
        .createTaskQuery()
        .taskAssignee("alice@example.com")
        .taskCreatedAfter(Date.from(Instant.now().minus(7, ChronoUnit.DAYS)))
        .orderByTaskCreateTime()
        .desc();

List<Task> tasks = query.list();
long count = query.count();

// 分页查询
List<Task> page = query.listPage(0, 10);
```

### 6. 流程定义查询

```java
// 查询最新版本
Optional<ProcessDefinition> latest = camundaAdapter.definition()
        .findLatestVersionByKey("order_approval");

if (latest.isPresent()) {
    ProcessDefinition def = latest.get();
    log.info("版本: {}", def.getVersion());
}

// 检查是否已部署
boolean deployed = camundaAdapter.definition()
        .isDeployed("order_approval");

// 查询所有流程定义（最新版本）
List<ProcessDefinition> all = camundaAdapter.definition()
        .findAllLatestVersion();
```

## 适配器类说明

### RuntimeOperation

流程实例运行时操作类。

**主要功能**：
- 启动流程实例
- 终止流程实例
- 变量管理
- 流程实例查询

### TaskOperation

任务操作类。

**主要功能**：
- 任务认领/释放
- 任务完成/委托
- 任务变量管理
- 任务查询

### DefinitionOperation

流程定义操作类。

**主要功能**：
- 流程定义查询
- 版本管理
- 部署检查

### HistoryOperation

历史数据操作类。

**主要功能**：
- 历史流程实例查询
- 历史任务查询
- 审计数据查询

## 优势对比

### 使用前（原生 Camunda API）

```java
ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
RuntimeService runtimeService = processEngine.getRuntimeService();

// 启动流程实例
ProcessInstance instance = runtimeService.startProcessInstanceByKey(
    "order_approval",
    "ORDER_12345",
    variables
);

// 完成任务
TaskService taskService = processEngine.getTaskService();
taskService.claim(taskId, "alice@example.com");
taskService.complete(taskId, variables);

// 查询任务
TaskQuery query = taskService.createTaskQuery()
    .taskAssignee("alice@example.com");
List<Task> tasks = query.list();
```

### 使用后（适配层）

```java
// 直接使用适配器
String instanceId = camundaAdapter.runtime()
        .startProcessByKey("order_approval", "ORDER_12345", variables);

// 任务操作
camundaAdapter.task()
        .claim(taskId, "alice@example.com")
        .complete(taskId, variables);

// 查询任务
List<Task> tasks = camundaAdapter.task()
        .findByAssignee("alice@example.com");
```

## 最佳实践

### 1. 异常处理

虽然适配层简化了操作，但仍然需要处理异常：

```java
try {
    camundaAdapter.runtime().startProcessByKey("order_approval");
} catch (ProcessEngineException e) {
    // 处理 Camunda 引擎异常
    throw new CamundaServiceException("ENGINE_ERROR", e.getMessage());
}
```

### 2. 变量命名

使用有意义的变量名：

```java
// 好的命名
variables.put("orderId", "12345");
variables.put("approvalAmount", 1000.0);
variables.put("approvedBy", "manager@example.com");

// 避免使用
variables.put("v1", "12345");
variables.put("a", 1000.0);
```

### 3. 批量操作

对于大量操作，建议使用 Query 对象：

```java
// 高效的批量查询
TaskQuery query = camundaAdapter.task()
        .createTaskQuery()
        .taskAssignee(userId)
        .active()
        .orderByTaskCreateTime()
        .desc();

List<Task> tasks = query.listPage(pageNo * pageSize, pageSize);
long total = query.count();
```

### 4. 日志记录

适配层已内置日志记录，但可以根据需要添加业务日志：

```java
String instanceId = camundaAdapter.runtime()
        .startProcessByKey("order_approval", businessKey, variables);

log.info("订单审批流程已启动 [orderId={}, instanceId={}]",
        businessKey, instanceId);
```

### 5. 类型安全

使用泛型方法确保类型安全：

```java
// 类型安全
Boolean approved = camundaAdapter.runtime()
        .getVariable(instanceId, "approved", Boolean.class);

// 避免强制类型转换
Boolean approved = (Boolean) camundaAdapter.runtime()
        .getVariable(instanceId, "approved");
```

## 性能考虑

### 1. 查询优化

- 使用索引字段进行查询（ assignee, definitionKey 等）
- 使用分页查询大数据集
- 避免查询全部数据

### 2. 变量管理

- 仅存储必要的变量
- 对于大数据，考虑使用外部存储
- 避免频繁的变量读写

### 3. 缓存

- 可以对流程定义进行缓存
- 缓存常用的查询结果

## 迁移指南

### 从原生 API 迁移

1. 替换服务获取方式：
   ```java
   // 旧方式
   ProcessEngine engine = ProcessEngines.getDefaultProcessEngine();
   RuntimeService service = engine.getRuntimeService();

   // 新方式
   RuntimeOperation runtime = camundaAdapter.runtime();
   ```

2. 简化 API 调用：
   ```java
   // 旧方式
   ProcessInstance instance = service.startProcessInstanceByKey(
       "key", "businessKey", variables);

   // 新方式
   String instanceId = camundaAdapter.runtime()
       .startProcessByKey("key", "businessKey", variables);
   ```

3. 使用简化查询：
   ```java
   // 旧方式
   List<Task> tasks = service.createTaskQuery()
       .taskAssignee(userId)
       .list();

   // 新方式
   List<Task> tasks = camundaAdapter.task()
       .findByAssignee(userId);
   ```

## 扩展指南

### 添加自定义方法

如果需要添加自定义操作，可以扩展适配器：

```java
@Component
public class CustomCamundaAdapter extends CamundaAdapter {

    public CustomCamundaAdapter(ProcessEngine processEngine) {
        super(processEngine);
    }

    /**
     * 自定义方法：批量完成任务
     */
    public void batchCompleteTasks(List<String> taskIds) {
        for (String taskId : taskIds) {
            try {
                task().complete(taskId);
                log.debug("任务已完成 [taskId={}]", taskId);
            } catch (Exception e) {
                log.error("任务完成失败 [taskId={}]", taskId, e);
                throw e;
            }
        }
    }
}
```

## 常见问题

### Q: 如何处理分页？

A: 使用 Query 对象的 `listPage()` 方法：

```java
List<Task> tasks = query.listPage(offset, limit);
```

### Q: 如何查询特定状态的流程实例？

A: 使用 `active()` 或 `suspended()` 方法：

```java
List<ProcessInstance> running = camundaAdapter.runtime()
        .createProcessInstanceQuery()
        .active()
        .list();
```

### Q: 如何获取任务变量和流程变量？

A: 使用对应的 `getVariable()` 方法：

```java
// 任务变量
Object taskVar = camundaAdapter.task()
        .getVariable(taskId, "variableName", String.class);

// 流程变量
Object processVar = camundaAdapter.runtime()
        .getVariable(instanceId, "variableName", String.class);
```

### Q: 如何检查流程实例是否存在？

A: 使用 `createProcessInstanceQuery()` 并检查结果：

```java
long count = camundaAdapter.runtime()
        .createProcessInstanceQuery()
        .processInstanceId(instanceId)
        .count();

boolean exists = count > 0;
```

## 版本信息

- 版本：1.0.0
- 创建日期：2025-01-01
- 作者：BaseBackend Team
