# 工作流前端 API 对接指南

## 概述

本指南提供 Camunda 工作流后端 API 的完整对接说明，帮助前端开发人员快速集成工作流功能。

**基础 URL**: `http://localhost:8089`

## 📋 API 接口清单

### 1. 流程定义管理

#### 1.1 部署流程定义
```http
POST /api/camunda/process-definitions/deployments
Content-Type: multipart/form-data

请求参数:
- file: BPMN 文件
- tenantId: 租户ID (可选)
- deploymentName: 部署名称 (可选)

响应示例:
{
  "success": true,
  "message": "流程定义部署成功",
  "data": "22501:7c2b4a9f-b8c1-4d3e-a7f0-1a2b3c4d5e6f"
}
```

#### 1.2 分页查询流程定义
```http
GET /api/camunda/process-definitions?pageNo=1&pageSize=20&latestVersion=true&tenantId=default

响应示例:
{
  "success": true,
  "data": {
    "total": 5,
    "pageNo": 1,
    "pageSize": 20,
    "data": [
      {
        "id": "order_approval:2:12345",
        "key": "order_approval",
        "name": "订单审批流程",
        "version": 2,
        "deploymentId": "22501:7c2b4a9f-b8c1-4d3e-a7f0-1a2b3c4d5e6f",
        "resourceName": "order_approval.bpmn",
        "tenantId": "default",
        "suspended": false,
        "createTime": "2025-01-01T10:00:00.000Z"
      }
    ]
  }
}
```

#### 1.3 获取流程定义详情
```http
GET /api/camunda/process-definitions/{definitionId}

响应示例:
{
  "success": true,
  "data": {
    "id": "order_approval:2:12345",
    "key": "order_approval",
    "name": "订单审批流程",
    "version": 2,
    "description": "用于订单审批的标准流程",
    "startFormKey": "start_form",
    "suspended": false,
    "variables": []
  }
}
```

#### 1.4 启动流程实例
```http
POST /api/camunda/process-definitions/start
Content-Type: application/json

请求体:
{
  "processDefinitionId": "order_approval:2:12345",
  "businessKey": "ORDER-20250101-001",
  "variables": {
    "orderId": "10001",
    "orderAmount": 999.99,
    "applicant": "张三"
  },
  "tenantId": "default"
}

响应示例:
{
  "success": true,
  "message": "流程实例启动成功",
  "data": {
    "id": "12345-abcd-efgh-5678",
    "businessKey": "ORDER-20250101-001",
    "processDefinitionId": "order_approval:2:12345",
    "processDefinitionKey": "order_approval",
    "processDefinitionName": "订单审批流程",
    "processDefinitionVersion": 2,
    "tenantId": "default",
    "state": "running",
    "startTime": "2025-01-01T10:30:00.000Z"
  }
}
```

#### 1.5 挂起/激活流程定义
```http
PUT /api/camunda/process-definitions/{definitionId}/suspend
Content-Type: application/json

请求体:
{
  "suspended": true,
  "includeInstance": false,
  "executionDate": null
}

响应示例:
{
  "success": true,
  "message": "流程定义已挂起"
}
```

#### 1.6 下载 BPMN 文件
```http
GET /api/camunda/process-definitions/{definitionId}/bpmn

响应: 文件下载 (application/xml)
```

---

### 2. 流程实例管理

#### 2.1 分页查询流程实例
```http
GET /api/camunda/process-instances?pageNo=1&pageSize=20&state=running&tenantId=default

响应示例:
{
  "success": true,
  "data": {
    "total": 3,
    "data": [
      {
        "id": "12345-abcd-efgh-5678",
        "processDefinitionId": "order_approval:2:12345",
        "processDefinitionKey": "order_approval",
        "processDefinitionName": "订单审批流程",
        "businessKey": "ORDER-20250101-001",
        "tenantId": "default",
        "state": "running",
        "startTime": "2025-01-01T10:30:00.000Z",
        "endTime": null,
        "durationInMillis": null
      }
    ]
  }
}
```

#### 2.2 获取流程实例详情
```http
GET /api/camunda/process-instances/{instanceId}?withVariables=true

响应示例:
{
  "success": true,
  "data": {
    "id": "12345-abcd-efgh-5678",
    "processDefinitionId": "order_approval:2:12345",
    "processDefinitionName": "订单审批流程",
    "businessKey": "ORDER-20250101-001",
    "tenantId": "default",
    "state": "running",
    "startTime": "2025-01-01T10:30:00.000Z",
    "currentActivity": "UserTask_Approve",
    "variables": [
      {
        "name": "orderAmount",
        "value": 999.99,
        "type": "Double"
      }
    ]
  }
}
```

#### 2.3 获取流程变量
```http
GET /api/camunda/process-instances/{instanceId}/variables?local=false

响应示例:
{
  "success": true,
  "data": [
    {
      "name": "orderAmount",
      "value": 999.99,
      "type": "Double",
      "scope": "global"
    }
  ]
}
```

#### 2.4 设置流程变量
```http
PUT /api/camunda/process-instances/{instanceId}/variables
Content-Type: application/json

请求体:
{
  "variables": {
    "approvalResult": "approved",
    "approvedBy": "李四",
    "approvedTime": "2025-01-01T11:00:00.000Z"
  }
}

响应示例:
{
  "success": true,
  "message": "流程变量设置成功"
}
```

#### 2.5 终止流程实例
```http
POST /api/camunda/process-instances/{instanceId}/terminate
Content-Type: application/json

请求体:
{
  "reason": "用户取消操作"
}

响应示例:
{
  "success": true,
  "message": "流程实例已终止"
}
```

#### 2.6 挂起/激活流程实例
```http
PUT /api/camunda/process-instances/{instanceId}/suspend
Content-Type: application/json

请求体:
{
  "suspended": true,
  "executionDate": null
}

响应示例:
{
  "success": true,
  "message": "流程实例已挂起"
}
```

#### 2.7 删除流程实例
```http
DELETE /api/camunda/process-instances/{instanceId}?deleteReason=用户删除&skipCustomListeners=false

响应示例:
{
  "success": true,
  "message": "流程实例删除成功"
}
```

#### 2.8 迁移流程实例
```http
POST /api/camunda/process-instances/{instanceId}/migrate
Content-Type: application/json

请求体:
{
  "sourceProcessDefinitionId": "order_approval:1:54321",
  "targetProcessDefinitionId": "order_approval:2:12345",
  "processInstanceIds": ["12345-abcd-efgh-5678"],
  "mapEqualActivities": true,
  "skipCustomListeners": true,
  "skipIoMappings": true,
  "instructions": []
}

响应示例:
{
  "success": true,
  "message": "流程实例迁移任务已提交"
}
```

---

### 3. 任务管理

#### 3.1 分页查询任务
```http
GET /api/camunda/tasks?pageNo=1&pageSize=20&assignee=zhangsan&tenantId=default

响应示例:
{
  "success": true,
  "data": {
    "total": 5,
    "data": [
      {
        "id": "task-001",
        "name": "审批订单",
        "assignee": "zhangsan",
        "owner": null,
        "description": "请审批订单金额",
        "processInstanceId": "12345-abcd-efgh-5678",
        "processDefinitionId": "order_approval:2:12345",
        "processDefinitionName": "订单审批流程",
        "taskDefinitionKey": "UserTask_Approve",
        "priority": 50,
        "createTime": "2025-01-01T10:35:00.000Z",
        "dueDate": "2025-01-01T18:00:00.000Z",
        "tenantId": "default",
        "state": "created"
      }
    ]
  }
}
```

#### 3.2 获取任务详情
```http
GET /api/camunda/tasks/{taskId}

响应示例:
{
  "success": true,
  "data": {
    "id": "task-001",
    "name": "审批订单",
    "assignee": "zhangsan",
    "description": "请审批订单金额",
    "processInstanceId": "12345-abcd-efgh-5678",
    "processDefinitionId": "order_approval:2:12345",
    "processDefinitionName": "订单审批流程",
    "taskDefinitionKey": "UserTask_Approve",
    "priority": 50,
    "createTime": "2025-01-01T10:35:00.000Z",
    "dueDate": "2025-01-01T18:00:00.000Z",
    "followUpDate": null,
    "tenantId": "default",
    "formKey": "task_form_001",
    "variables": [
      {
        "name": "orderAmount",
        "value": 999.99,
        "type": "Double"
      }
    ]
  }
}
```

#### 3.3 认领任务
```http
POST /api/camunda/tasks/{taskId}/claim
Content-Type: application/json

请求体:
{
  "userId": "zhangsan"
}

响应示例:
{
  "success": true,
  "message": "任务认领成功"
}
```

#### 3.4 释放任务
```http
POST /api/camunda/tasks/{taskId}/release
Content-Type: application/json

响应示例:
{
  "success": true,
  "message": "任务释放成功"
}
```

#### 3.5 完成任务
```http
POST /api/camunda/tasks/{taskId}/complete
Content-Type: application/json

请求体:
{
  "variables": {
    "approvalResult": "approved",
    "approvalComment": "同意审批"
  },
  "completeTask": true
}

响应示例:
{
  "success": true,
  "message": "任务完成成功"
}
```

#### 3.6 委托任务
```http
POST /api/camunda/tasks/{taskId}/delegate
Content-Type: application/json

请求体:
{
  "userId": "lisi"
}

响应示例:
{
  "success": true,
  "message": "任务委托成功"
}
```

#### 3.7 获取任务变量
```http
GET /api/camunda/tasks/{taskId}/variables?local=true

响应示例:
{
  "success": true,
  "data": [
    {
      "name": "orderAmount",
      "value": 999.99,
      "type": "Double",
      "scope": "local"
    }
  ]
}
```

#### 3.8 设置任务变量
```http
PUT /api/camunda/tasks/{taskId}/variables
Content-Type: application/json

请求体:
{
  "variables": {
    "approvalResult": "approved",
    "approvalComment": "同意审批"
  }
}

响应示例:
{
  "success": true,
  "message": "任务变量设置成功"
}
```

#### 3.9 获取任务评论
```http
GET /api/camunda/tasks/{taskId}/comments

响应示例:
{
  "success": true,
  "data": [
    {
      "id": "comment-001",
      "taskId": "task-001",
      "userId": "zhangsan",
      "message": "已完成审批",
      "time": "2025-01-01T11:00:00.000Z"
    }
  ]
}
```

#### 3.10 添加任务评论
```http
POST /api/camunda/tasks/{taskId}/comments
Content-Type: application/json

请求体:
{
  "message": "已完成审批"
}

响应示例:
{
  "success": true,
  "message": "评论添加成功"
}
```

---

### 4. 历史流程实例查询

#### 4.1 分页查询历史流程实例
```http
GET /api/camunda/historic/process-instances?pageNo=1&pageSize=20&state=completed

响应示例:
{
  "success": true,
  "data": {
    "total": 10,
    "data": [
      {
        "id": "12345-abcd-efgh-5678",
        "processDefinitionId": "order_approval:2:12345",
        "processDefinitionName": "订单审批流程",
        "businessKey": "ORDER-20250101-001",
        "state": "completed",
        "startTime": "2025-01-01T10:30:00.000Z",
        "endTime": "2025-01-01T11:30:00.000Z",
        "durationInMillis": 3600000,
        "deleteReason": null,
        "startUserId": "zhangsan",
        "tenantId": "default"
      }
    ]
  }
}
```

#### 4.2 获取历史流程实例详情
```http
GET /api/camunda/historic/process-instances/{instanceId}

响应示例:
{
  "success": true,
  "data": {
    "id": "12345-abcd-efgh-5678",
    "processDefinitionId": "order_approval:2:12345",
    "processDefinitionName": "订单审批流程",
    "businessKey": "ORDER-20250101-001",
    "state": "completed",
    "startTime": "2025-01-01T10:30:00.000Z",
    "endTime": "2025-01-01T11:30:00.000Z",
    "durationInMillis": 3600000,
    "startUserId": "zhangsan",
    "tenantId": "default",
    "activities": [
      {
        "activityId": "StartEvent_1",
        "activityName": "开始",
        "activityType": "startEvent",
        "startTime": "2025-01-01T10:30:00.000Z",
        "endTime": "2025-01-01T10:30:00.500Z",
        "durationInMillis": 500
      }
    ]
  }
}
```

---

### 5. 监控运维

#### 5.1 获取引擎概览指标
```http
GET /api/camunda/ops/metrics

响应示例:
{
  "success": true,
  "data": {
    "jobs.active": 5,
    "jobs.suspended": 0,
    "jobs.failed": 0,
    "jobs.timer": 3,
    "jobs.message": 2,
    "jobExecutor.lockTimeInMillis": 300000
  }
}
```

#### 5.2 查询失败的Job
```http
GET /api/camunda/ops/jobs/failed

响应示例:
{
  "success": true,
  "data": [
    {
      "id": "job-001",
      "processInstanceId": "12345-abcd-efgh-5678",
      "processDefinitionId": "order_approval:2:12345",
      "executionId": "execution-001",
      "exceptionMessage": "数据库连接失败",
      "retries": 0,
      "dueDate": "2025-01-01T10:30:00.000Z",
      "createTime": "2025-01-01T10:30:00.000Z"
    }
  ]
}
```

---

## 🔐 认证方式

所有 API 都需要 Bearer Token 认证：

```http
Authorization: Bearer {access_token}
```

## 📝 请求/响应规范

### 统一响应格式
```json
{
  "success": true|false,
  "message": "操作结果描述",
  "data": {}, // 成功时的数据
  "code": 200, // 状态码
  "timestamp": "2025-01-01T10:00:00.000Z"
}
```

### 分页响应格式
```json
{
  "success": true,
  "data": {
    "total": 100,
    "pageNo": 1,
    "pageSize": 20,
    "data": [] // 数据列表
  }
}
```

### 错误响应格式
```json
{
  "success": false,
  "message": "错误描述",
  "code": 400,
  "timestamp": "2025-01-01T10:00:00.000Z",
  "details": {
    "field": "processDefinitionId",
    "message": "流程定义ID不能为空"
  }
}
```

## ⚠️ 注意事项

1. **租户隔离**: 所有请求都建议带上 `tenantId` 参数
2. **分页参数**: 默认页码从 1 开始，每页默认 20 条记录
3. **变量类型**: 支持 String、Integer、Long、Double、Boolean、Date、Json 等类型
4. **文件上传**: 使用 `multipart/form-data` 格式
5. **日期格式**: 使用 ISO 8601 格式 `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`
6. **幂等性**: 关键操作建议实现幂等性检查
7. **权限控制**: 建议实现基于用户和角色的权限控制

## 📞 技术支持

如有问题，请联系：
- 后端开发团队
- 查看 API 文档：`http://localhost:8089/swagger-ui/index.html`
