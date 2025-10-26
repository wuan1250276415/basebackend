# Camunda 工作流引擎快速开始

## 🚀 5 分钟快速体验

### 1. 启动服务

```bash
cd basebackend-scheduler
mvn spring-boot:run
```

等待服务启动完成，看到 "Started SchedulerApplication" 日志。

### 2. 访问管理界面

打开浏览器访问：http://localhost:8085/scheduler/camunda/app/

登录信息：
- 用户名：`admin`
- 密码：`admin`

### 3. 启动示例流程

使用 curl 或 Postman 发送请求：

```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "order-approval-process",
    "businessKey": "ORDER-001",
    "variables": {
      "orderId": "ORDER-001",
      "amount": 8000,
      "approver": "admin",
      "email": "admin@example.com",
      "emailSubject": "订单提交通知",
      "emailContent": "您的订单已提交，订单号：ORDER-001"
    }
  }'
```

响应示例：
```json
{
  "success": true,
  "data": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "businessKey": "ORDER-001",
    "processDefinitionId": "order-approval-process:1:xxxx"
  },
  "message": "流程实例启动成功"
}
```

### 4. 查看流程执行

在 Cockpit 界面中：
1. 点击 "Processes"
2. 选择 "订单审批流程"
3. 点击流程实例 ID
4. 查看流程图和当前节点

### 5. 完成任务（可选）

如果金额 > 10000，会创建审批任务：

```bash
# 查询待办任务
curl -X GET http://localhost:8085/scheduler/api/workflow/tasks/pending/admin

# 完成任务
curl -X POST http://localhost:8085/scheduler/api/workflow/tasks/{taskId}/complete \
  -H "Content-Type: application/json" \
  -d '{
    "approved": true,
    "comment": "审批通过"
  }'
```

---

## 📝 更多示例

### 示例1：数据同步流程

```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "data-sync-process",
    "businessKey": "SYNC-20251023-001",
    "variables": {
      "sourceSystem": "MySQL",
      "targetSystem": "MongoDB",
      "dataType": "user-data",
      "batchSize": 1000
    }
  }'
```

### 示例2：微服务编排

```bash
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "microservice-orchestration",
    "businessKey": "MS-ORCHESTRATION-001",
    "variables": {
      "userId": "USER-123",
      "operation": "aggregateData",
      "payload": {
        "startDate": "2025-10-01",
        "endDate": "2025-10-23"
      }
    }
  }'
```

---

## 🔍 监控和查询

### 查询所有流程定义

```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/definitions
```

### 查询运行中的流程实例

```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/instances/running
```

### 根据业务键查询流程

```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/instances/business-key/ORDER-001
```

### 查询流程变量

```bash
curl -X GET http://localhost:8085/scheduler/api/workflow/instances/{processInstanceId}/variables
```

---

## 🛠️ 常用操作

### 挂起流程实例

```bash
curl -X PUT http://localhost:8085/scheduler/api/workflow/instances/{processInstanceId}/suspend
```

### 激活流程实例

```bash
curl -X PUT http://localhost:8085/scheduler/api/workflow/instances/{processInstanceId}/activate
```

### 删除流程实例

```bash
curl -X DELETE "http://localhost:8085/scheduler/api/workflow/instances/{processInstanceId}?deleteReason=测试完成"
```

---

## 📚 下一步

- 查看完整文档：[docs/CAMUNDA-GUIDE.md](./CAMUNDA-GUIDE.md)
- 学习 BPMN 2.0：https://camunda.com/bpmn/
- 下载 Camunda Modeler：https://camunda.com/download/modeler/
- 设计自己的流程并部署到系统

---

## 💡 提示

1. **管理界面三大工具**：
   - **Cockpit**：监控流程执行
   - **Tasklist**：处理待办任务
   - **Admin**：用户和权限管理

2. **业务键的作用**：
   - 业务键是流程实例的唯一标识
   - 可以通过业务键快速查询流程
   - 建议使用有意义的业务ID（如订单号）

3. **流程变量**：
   - 流程变量在整个流程中共享
   - 可以在任务中读取和设置变量
   - 变量会持久化到数据库

4. **错误处理**：
   - 所有示例流程都包含错误处理机制
   - 可在 Cockpit 中查看错误详情
   - 支持自动重试和人工干预
