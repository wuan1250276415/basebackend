# Camunda 7 工作流引擎使用指南

## 📋 目录

1. [简介](#简介)
2. [快速开始](#快速开始)
3. [管理界面](#管理界面)
4. [REST API 使用](#rest-api-使用)
5. [流程开发](#流程开发)
6. [示例流程](#示例流程)
7. [最佳实践](#最佳实践)
8. [常见问题](#常见问题)

---

## 简介

本项目已集成 **Camunda 7 BPM Platform**，提供强大的工作流编排能力，支持：

- ✅ **BPMN 2.0 标准**：完整支持 BPMN 2.0 规范
- ✅ **可视化设计**：使用 Camunda Modeler 设计流程
- ✅ **管理界面**：Cockpit、Tasklist、Admin 三大管理工具
- ✅ **REST API**：完整的流程管理 API
- ✅ **嵌入式部署**：无需额外服务器，集成到 Spring Boot

---

## 快速开始

### 1. 启动服务

```bash
# 构建项目
mvn clean install -DskipTests

# 启动 scheduler 服务
cd basebackend-scheduler
mvn spring-boot:run
```

服务启动后访问：
- **应用地址**: http://localhost:8085/scheduler
- **Camunda 管理界面**: http://localhost:8085/scheduler/camunda/app/
- **REST API**: http://localhost:8085/scheduler/api/workflow

### 2. 登录管理界面

默认管理员账号：
- **用户名**: `admin`
- **密码**: `admin`

可通过环境变量 `CAMUNDA_ADMIN_PASSWORD` 修改密码。

---

## 管理界面

### Cockpit（驾驶舱）

**访问地址**: http://localhost:8085/scheduler/camunda/app/cockpit

**功能**:
- 查看所有流程定义和实例
- 监控流程执行状态
- 查看流程图和当前节点
- 历史数据分析
- 性能指标监控

### Tasklist（任务列表）

**访问地址**: http://localhost:8085/scheduler/camunda/app/tasklist

**功能**:
- 查看待办任务
- 认领和完成任务
- 查看任务详情和变量
- 任务分配和委派

### Admin（管理员）

**访问地址**: http://localhost:8085/scheduler/camunda/app/admin

**功能**:
- 用户和组管理
- 权限配置
- 系统设置
- 租户管理

---

## REST API 使用

### 流程定义管理

#### 查询所有流程定义
```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/definitions
```

#### 部署流程定义
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/definitions \
  -H "Content-Type: multipart/form-data" \
  -F "name=订单审批流程" \
  -F "file=@order-approval-process.bpmn"
```

#### 根据Key查询流程定义
```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/definitions/key/order-approval-process
```

#### 挂起/激活流程定义
```bash
# 挂起
curl -X PUT http://localhost:8085/scheduler/api/workflow/definitions/{id}/suspend

# 激活
curl -X PUT http://localhost:8085/scheduler/api/workflow/definitions/{id}/activate
```

---

### 流程实例管理

#### 启动流程实例
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "order-approval-process",
    "businessKey": "ORDER-001",
    "variables": {
      "orderId": "ORDER-001",
      "amount": 15000,
      "approver": "manager",
      "email": "user@example.com",
      "emailSubject": "订单审批通知",
      "emailContent": "您有一个新的订单需要审批"
    }
  }'
```

#### 查询运行中的流程实例
```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/instances/running
```

#### 根据业务键查询流程实例
```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/instances/business-key/ORDER-001
```

#### 设置流程变量
```bash
curl -X PUT http://localhost:8085/scheduler/api/workflow/instances/{id}/variables \
  -H "Content-Type: application/json" \
  -d '{
    "status": "approved",
    "approvedBy": "manager",
    "approvedTime": "2025-10-23T10:30:00Z"
  }'
```

---

### 任务管理

#### 查询待办任务
```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/tasks/pending/manager
```

#### 查询候选任务
```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/tasks/candidate/user123
```

#### 认领任务
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/tasks/{taskId}/claim \
  -H "Content-Type: application/json" \
  -d '{"userId": "manager"}'
```

#### 完成任务
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "comment": "审批通过",
    "approvedBy": "manager"
  }'
```

#### 委派任务
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/tasks/{taskId}/delegate \
  -H "Content-Type: application/json" \
  -d '{"userId": "deputy-manager"}'
```

---

## 流程开发

### 1. 使用 Camunda Modeler 设计流程

**下载地址**: https://camunda.com/download/modeler/

**步骤**:
1. 打开 Camunda Modeler
2. 创建新的 BPMN 图
3. 拖拽元素设计流程
4. 配置任务属性（ID、Name、Assignee等）
5. 保存为 `.bpmn` 文件

### 2. 创建 JavaDelegate

在 `com.basebackend.scheduler.camunda.delegate` 包下创建委托类：

```java
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("myCustomDelegate")
public class MyCustomDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // 获取流程变量
        String orderId = (String) execution.getVariable("orderId");

        // 执行业务逻辑
        // ...

        // 设置输出变量
        execution.setVariable("result", "success");
    }
}
```

### 3. 在 BPMN 中引用 JavaDelegate

**方法1：使用 delegateExpression**
```xml
<bpmn:serviceTask id="Task_1" name="处理订单"
                  camunda:delegateExpression="${myCustomDelegate}">
</bpmn:serviceTask>
```

**方法2：使用 class**
```xml
<bpmn:serviceTask id="Task_1" name="处理订单"
                  camunda:class="com.basebackend.scheduler.camunda.delegate.MyCustomDelegate">
</bpmn:serviceTask>
```

**方法3：使用 expression**
```xml
<bpmn:serviceTask id="Task_1" name="设置变量"
                  camunda:expression="${execution.setVariable('approved', true)}">
</bpmn:serviceTask>
```

### 4. 部署流程

将 `.bpmn` 文件放到 `src/main/resources/processes/` 目录下，服务启动时会自动部署。

---

## 示例流程

### 1. 订单审批流程

**流程Key**: `order-approval-process`

**流程说明**:
1. 订单提交
2. 验证订单
3. 金额判断：
   - 金额 > 10000：需要经理审批
   - 金额 ≤ 10000：自动审批
4. 处理订单
5. 发送通知

**启动示例**:
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "order-approval-process",
    "businessKey": "ORDER-001",
    "variables": {
      "orderId": "ORDER-001",
      "amount": 15000,
      "approver": "manager"
    }
  }'
```

### 2. 数据同步流程

**流程Key**: `data-sync-process`

**流程说明**:
1. 提取数据（Extract）
2. 转换数据（Transform）
3. 加载数据（Load）
4. 验证结果
5. 错误处理（如果失败）

**启动示例**:
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "data-sync-process",
    "businessKey": "SYNC-001",
    "variables": {
      "sourceSystem": "MySQL",
      "targetSystem": "MongoDB",
      "dataType": "user-data"
    }
  }'
```

### 3. 微服务编排流程

**流程Key**: `microservice-orchestration`

**流程说明**:
1. 并行调用三个微服务：
   - 用户服务
   - 订单服务
   - 支付服务
2. 等待所有服务返回
3. 聚合结果

**启动示例**:
```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "microservice-orchestration",
    "businessKey": "ORCHESTRATION-001",
    "variables": {
      "userId": "USER-123",
      "payload": {"action": "getData"}
    }
  }'
```

---

## 最佳实践

### 1. 流程设计

- ✅ **使用有意义的ID和Name**：便于理解和维护
- ✅ **合理使用网关**：控制流程分支和并行
- ✅ **设置超时时间**：防止任务长时间挂起
- ✅ **添加错误边界事件**：处理异常情况
- ✅ **使用业务键**：便于通过业务ID查询流程

### 2. 变量管理

- ✅ **使用简单类型**：String、Integer、Boolean
- ✅ **避免存储大对象**：流程变量会持久化到数据库
- ✅ **使用前缀**：区分不同模块的变量
- ✅ **及时清理变量**：避免变量过多影响性能

### 3. 任务分配

- ✅ **明确办理人**：使用 `assignee` 属性
- ✅ **使用候选用户/组**：支持任务池模式
- ✅ **设置优先级**：区分任务重要程度
- ✅ **设置到期时间**：提醒任务超时

### 4. 性能优化

- ✅ **异步任务**：耗时操作使用异步
- ✅ **批量操作**：避免频繁启动流程
- ✅ **历史数据清理**：定期清理过期历史数据
- ✅ **合理设置历史级别**：平衡性能和可追溯性

### 5. 监控和告警

- ✅ **启用指标收集**：监控流程执行情况
- ✅ **配置告警规则**：及时发现异常
- ✅ **定期查看 Cockpit**：了解流程健康状况

---

## 常见问题

### Q1: 如何修改管理员密码？

设置环境变量：
```bash
export CAMUNDA_ADMIN_PASSWORD=newpassword
```

### Q2: 流程部署失败怎么办？

检查：
1. BPMN 文件格式是否正确
2. JavaDelegate 类是否存在
3. Spring Bean 是否正确注册
4. 查看日志获取详细错误信息

### Q3: 如何查看流程执行日志？

1. 登录 Cockpit 界面
2. 选择流程实例
3. 查看 Activity Instance History

### Q4: 任务无法完成怎么办？

检查：
1. 任务是否已被认领
2. 流程变量是否正确设置
3. 条件表达式是否正确

### Q5: 如何实现流程版本管理？

Camunda 自动支持版本管理：
- 相同 Key 的流程部署会自动创建新版本
- 默认启动最新版本
- 可指定版本启动流程

---

## 相关资源

- **Camunda 官方文档**: https://docs.camunda.org/manual/7.21/
- **BPMN 2.0 规范**: https://www.omg.org/spec/BPMN/2.0/
- **Camunda Modeler**: https://camunda.com/download/modeler/
- **社区论坛**: https://forum.camunda.org/

---

## 技术支持

如遇问题，请：
1. 查看 Camunda Cockpit 中的错误信息
2. 检查应用日志：`logs/scheduler.log`
3. 参考官方文档
4. 联系项目维护团队
