# BaseBackend Scheduler Feign 客户端使用指南

## 📖 概述

`basebackend-scheduler` 模块提供了完整的 Feign 客户端接口，允许其他服务通过 Feign 客户端直接调用调度器服务，无需使用 REST API。

## 🎯 功能特性

- ✅ **完整覆盖**: 支持流程定义、流程实例、任务、表单模板等核心功能
- ✅ **类型安全**: 使用强类型 DTO，避免字符串错误
- ✅ **降级处理**: 每个客户端都配备了 FallbackFactory，保证服务可用性
- ✅ **监控友好**: 集成日志记录和异常处理
- ✅ **超时控制**: 配置了合理的连接和读取超时时间

## 📦 客户端列表

### 1. ProcessDefinitionFeignClient - 流程定义客户端

**服务名**: `basebackend-scheduler`
**路径前缀**: `/api/camunda/process-definitions`

#### 核心功能
- 查询流程定义详情
- 获取最新版本的流程定义
- 启动流程实例
- 激活/挂起流程定义
- 检查流程定义是否存在
- 获取流程定义版本列表

#### 使用示例

```java
@Service
public class WorkflowService {

    @Autowired
    private ProcessDefinitionFeignClient processDefinitionClient;

    public void startWorkflow(String processKey, String businessKey, Map<String, Object> variables) {
        // 启动流程实例
        ProcessDefinitionStartRequest request = new ProcessDefinitionStartRequest();
        request.setProcessDefinitionKey(processKey);
        request.setBusinessKey(businessKey);
        request.setVariables(variables);
        request.setStarter("system");

        Result<String> result = processDefinitionClient.startProcessInstance(request);
        if (result.isSuccess()) {
            String instanceId = result.getData();
            log.info("流程启动成功，实例ID: {}", instanceId);
        } else {
            throw new BusinessException("流程启动失败: " + result.getMessage());
        }
    }

    public ProcessDefinitionFeignDTO getLatestProcessDefinition(String key) {
        Result<ProcessDefinitionFeignDTO> result = processDefinitionClient.getLatestVersion(key, null);
        if (result.isSuccess()) {
            return result.getData();
        }
        throw new BusinessException("获取流程定义失败: " + result.getMessage());
    }
}
```

---

### 2. ProcessInstanceFeignClient - 流程实例客户端

**服务名**: `basebackend-scheduler`
**路径前缀**: `/api/camunda/process-instances`

#### 核心功能
- 查询流程实例详情
- 根据业务键查询流程实例
- 激活/挂起/删除流程实例
- 管理流程变量（获取、设置、删除）
- 检查流程实例是否存在
- 获取流程实例统计信息

#### 使用示例

```java
@Service
public class ProcessInstanceService {

    @Autowired
    private ProcessInstanceFeignClient instanceClient;

    public ProcessInstanceFeignDTO getProcessInstance(String instanceId) {
        Result<ProcessInstanceFeignDTO> result = instanceClient.getById(instanceId, true);
        if (result.isSuccess()) {
            return result.getData();
        }
        throw new BusinessException("获取流程实例失败: " + result.getMessage());
    }

    public void setProcessVariable(String instanceId, String variableName, Object value) {
        Result<Void> result = instanceClient.setVariable(instanceId, variableName, value);
        if (!result.isSuccess()) {
            throw new BusinessException("设置流程变量失败: " + result.getMessage());
        }
    }

    public Map<String, Object> getAllVariables(String instanceId) {
        Result<Map<String, Object>> result = instanceClient.getVariables(instanceId);
        return result.isSuccess() ? result.getData() : Collections.emptyMap();
    }
}
```

---

### 3. TaskFeignClient - 任务客户端

**服务名**: `basebackend-scheduler`
**路径前缀**: `/api/camunda/tasks`

#### 核心功能
- 查询任务详情和任务列表
- 任务操作（认领、释放、完成任务、委派）
- 管理任务变量
- 获取用户待办任务数量
- 获取任务统计信息
- 根据流程实例查询当前活动任务

#### 使用示例

```java
@Service
public class TaskService {

    @Autowired
    private TaskFeignClient taskClient;

    public void completeTask(String taskId, String userId, Map<String, Object> variables) {
        TaskActionRequest request = new TaskActionRequest();
        request.setTaskId(taskId);
        request.setUserId(userId);
        request.setVariables(variables);

        Result<Void> result = taskClient.complete(taskId, request);
        if (!result.isSuccess()) {
            throw new BusinessException("任务完成失败: " + result.getMessage());
        }
    }

    public List<TaskFeignDTO> getUserTasks(String userId) {
        Result<List<TaskFeignDTO>> result = taskClient.getList(
            assignee = userId,
            candidateUser = null,
            candidateGroup = null,
            processInstanceId = null,
            processDefinitionKey = null,
            name = null,
            state = "open",
            tenantId = null,
            limit = 100
        );

        return result.isSuccess() ? result.getData() : Collections.emptyList();
    }

    public void claimTask(String taskId, String userId) {
        Result<Void> result = taskClient.claim(taskId, userId);
        if (!result.isSuccess()) {
            throw new BusinessException("任务认领失败: " + result.getMessage());
        }
    }
}
```

---

### 4. FormTemplateFeignClient - 表单模板客户端

**服务名**: `basebackend-scheduler`
**路径前缀**: `/api/camunda/form-templates`

#### 核心功能
- 查询表单模板详情和列表
- 根据编码、流程定义键、业务类型查询
- 检查表单编码唯一性
- 获取表单分类和业务类型
- 启用/禁用表单模板
- 获取表单模板统计信息

#### 使用示例

```java
@Service
public class FormService {

    @Autowired
    private FormTemplateFeignClient formClient;

    public FormTemplateFeignDTO getFormByCode(String code) {
        Result<FormTemplateFeignDTO> result = formClient.getByCode(code);
        if (result.isSuccess()) {
            return result.getData();
        }
        throw new BusinessException("获取表单模板失败: " + result.getMessage());
    }

    public FormTemplateFeignDTO getFormByProcessDefinition(String processKey) {
        Result<FormTemplateFeignDTO> result = formClient.getByProcessDefinitionKey(processKey, null);
        if (result.isSuccess()) {
            return result.getData();
        }
        throw new BusinessException("获取流程表单失败: " + result.getMessage());
    }

    public List<FormTemplateFeignDTO> getFormsByBusinessType(String businessType) {
        Result<List<FormTemplateFeignDTO>> result = formClient.getByBusinessType(businessType, true);
        return result.isSuccess() ? result.getData() : Collections.emptyList();
    }
}
```

---

## 🔧 配置说明

### 1. 添加依赖

在调用方模块的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-scheduler</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 启用 Feign 客户端

在调用方的启动类或配置类中启用 Feign：

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.basebackend.scheduler.feign")
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 3. application.yml 配置

```yaml
# Feign 配置
feign:
  client:
    config:
      # 默认配置
      default:
        connect-timeout: 5000        # 连接超时（毫秒）
        read-timeout: 10000          # 读取超时（毫秒）
        logger-level: basic          # 日志级别

      # 针对调度器服务的配置
      basebackend-scheduler:
        connect-timeout: 8000        # 连接超时（毫秒）
        read-timeout: 15000          # 读取超时（毫秒）
        logger-level: full           # 日志级别（调试时可开启）

  # 启用熔断器
  circuitbreaker:
    enabled: true

# 服务发现 (Nacos)
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.66.126:8848
        namespace: dev
        group: DEFAULT_GROUP
```

---

## 🛡️ 降级处理策略

每个 Feign 客户端都配备了 FallbackFactory，当调度器服务不可用时，会触发降级处理：

| 操作类型 | 降级策略 |
|----------|----------|
| **查询操作** | 返回空列表或空对象 |
| **检查操作** | 返回 false 或默认值 |
| **操作类操作** | 返回错误信息，提示稍后重试 |
| **统计数据** | 返回空统计 |

### 降级处理示例

```java
// 例如，查询流程实例列表失败时
Result<List<ProcessInstanceFeignDTO>> result = instanceClient.getList(...);
// 返回: Result.success("调度器服务暂时不可用，返回空列表", Collections.emptyList())
```

---

## ⚠️ 注意事项

### 1. 服务间认证

在生产环境中，建议配置服务间认证：

```java
@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 添加内部服务认证头
            template.header("X-Internal-Auth", "your-secret-token");
            template.header("X-Caller-Service", "basebackend-system-api");
        };
    }
}
```

### 2. 超时设置

根据业务场景调整超时时间：

- **简单查询**: 3-5秒
- **复杂查询**: 10-15秒
- **启动流程**: 5-10秒
- **任务操作**: 3-5秒

### 3. 错误处理

总是检查 Result 的 isSuccess 方法：

```java
Result<String> result = processDefinitionClient.startProcessInstance(request);
if (!result.isSuccess()) {
    log.error("启动流程失败: {}", result.getMessage());
    throw new BusinessException("流程启动失败: " + result.getMessage());
}
String instanceId = result.getData();
```

### 4. 线程安全

Feign 客户端是线程安全的，可以注入到单例 Bean 中使用。

---

## 📊 性能优化建议

### 1. 使用连接池

启用 HTTP 连接池可以显著提升性能：

```yaml
feign:
  httpclient:
    enabled: true
    max-connections: 200        # 最大连接数
    max-connections-per-route: 50  # 每个路由的最大连接数
```

### 2. 启用响应压缩

```yaml
feign:
  compression:
    request:
      enabled: true
      mime-types: text/xml,application/xml,application/json
      min-request-size: 2048
    response:
      enabled: true
```

### 3. 合理设置超时

根据网络延迟和服务响应时间合理设置超时时间。

### 4. 批量操作

对于大量数据的查询，建议使用分页：

```java
// 错误示例：一次性查询大量数据
List<TaskFeignDTO> tasks = taskClient.getList(assignee = userId, null, null, null, null, null, null, null, 10000);

// 正确示例：分页查询
List<TaskFeignDTO> tasks = taskClient.getList(assignee = userId, null, null, null, null, null, null, null, 100);
```

---

## 🔍 故障排查

### 问题1: 连接超时

```
错误信息: Read timed out
```

**解决方案**:
1. 检查调度器服务是否正常运行
2. 增加超时时间配置
3. 检查网络连接

### 问题2: 服务发现失败

```
错误信息: No instances available for basebackend-scheduler
```

**解决方案**:
1. 检查 Nacos 连接配置
2. 确认调度器服务已注册到 Nacos
3. 检查服务名是否正确

### 问题3: 降级处理触发

```
日志: [Feign降级] 调度器服务不可用
```

**说明**: 这表示调度器服务暂时不可用，降级处理已生效。可以通过以下方式排查：
1. 检查调度器服务状态
2. 查看调度器服务日志
3. 检查网络连接

---

## 📚 参考资料

- [Spring Cloud OpenFeign 官方文档](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
- [Camunda 官方文档](https://docs.camunda.org/)
- [Feign GitHub](https://github.com/OpenFeign/feign)

---

**文档版本**: v1.0
**创建时间**: 2025-11-25
**维护者**: Claude Code (浮浮酱)
