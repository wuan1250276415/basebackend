# Camunda 工作流引擎 API 接口文档

## 概述

本文档描述了 `scheduler-integration` 模块中 Camunda 工作流引擎相关的 REST API 接口。

**基础路径**: `/api/camunda`

**认证方式**: Bearer Token (JWT)

---

## 一、流程定义管理 API

**路径前缀**: `/api/camunda/process-definitions`

### 1.1 部署流程定义

上传 BPMN 文件并部署到 Camunda 引擎。

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-definitions/deployments` |
| 描述 | 部署流程定义（支持 BPMN 2.0 XML） |

**请求体** (`ProcessDefinitionDeployRequest`):

```json
{
  "name": "请假流程",
  "source": "<?xml version=\"1.0\"...?>...",
  "tenantId": "tenant-001",
  "enableDuplicateFiltering": true
}
```

**响应**:

```json
{
  "code": 200,
  "message": "流程定义部署成功",
  "data": "deployment-id-xxx"
}
```

---

### 1.2 分页查询流程定义

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-definitions` |
| 描述 | 支持 Key、名称、租户、最新版本、挂起状态过滤 |

**请求参数** (`ProcessDefinitionPageQuery`):

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| current | int | 否 | 当前页，默认1 |
| size | int | 否 | 每页大小，默认10 |
| key | string | 否 | 流程定义 Key（模糊搜索） |
| name | string | 否 | 流程定义名称（模糊搜索） |
| tenantId | string | 否 | 租户 ID |
| latestVersion | boolean | 否 | 是否只查询最新版本 |
| suspended | boolean | 否 | 是否挂起状态 |

**响应** (`PageResult<ProcessDefinitionDTO>`):

```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "process-def-id",
        "key": "leave-request",
        "name": "请假流程",
        "version": 1,
        "deploymentId": "deployment-id",
        "suspended": false,
        "tenantId": "tenant-001"
      }
    ],
    "total": 100,
    "current": 1,
    "size": 10
  }
}
```

---

### 1.3 获取流程定义详情

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-definitions/{definitionId}` |
| 描述 | 根据流程定义 ID 获取详细信息 |

**路径参数**:

| 参数名 | 类型 | 描述 |
|--------|------|------|
| definitionId | string | 流程定义 ID |

**响应** (`ProcessDefinitionDetailDTO`):

```json
{
  "code": 200,
  "data": {
    "id": "process-def-id",
    "key": "leave-request",
    "name": "请假流程",
    "version": 1,
    "description": "员工请假审批流程",
    "deploymentId": "deployment-id",
    "deploymentTime": "2025-01-01T10:00:00",
    "suspended": false,
    "tenantId": "tenant-001"
  }
}
```

---

### 1.4 删除流程部署

| 属性 | 值 |
|------|-----|
| 路径 | `DELETE /api/camunda/process-definitions/deployments/{deploymentId}` |
| 描述 | 删除流程部署，可选级联删除关联的流程实例 |

**路径参数**:

| 参数名 | 类型 | 描述 |
|--------|------|------|
| deploymentId | string | 部署 ID |

**查询参数**:

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| cascade | boolean | false | 是否级联删除流程实例 |

---

### 1.5 下载 BPMN XML

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-definitions/{definitionId}/xml` |
| 描述 | 下载流程定义的 BPMN XML 文件 |
| 响应类型 | `application/xml` |

---

### 1.6 下载流程图

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-definitions/{definitionId}/diagram` |
| 描述 | 下载流程定义的流程图（PNG/SVG） |
| 响应类型 | `image/png` 或 `image/svg+xml` |

---

### 1.7 启动流程实例

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-definitions/start` |
| 描述 | 根据流程定义 ID 或 Key 启动新的流程实例 |

**请求体** (`ProcessDefinitionStartRequest`):

```json
{
  "processDefinitionId": "process-def-id",
  "processDefinitionKey": "leave-request",
  "businessKey": "LEAVE-2025-001",
  "variables": {
    "applicant": "张三",
    "days": 3
  },
  "tenantId": "tenant-001"
}
```

**响应** (`ProcessInstanceDTO`):

```json
{
  "code": 200,
  "message": "流程实例启动成功",
  "data": {
    "id": "instance-id",
    "processDefinitionId": "process-def-id",
    "businessKey": "LEAVE-2025-001",
    "tenantId": "tenant-001"
  }
}
```

---

### 1.8 挂起流程定义

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-definitions/{definitionId}/suspend` |
| 描述 | 挂起指定的流程定义 |

**请求体** (`ProcessDefinitionStateRequest`):

```json
{
  "includeProcessInstances": true
}
```

---

### 1.9 激活流程定义

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-definitions/{definitionId}/activate` |
| 描述 | 激活指定的流程定义 |

---

## 二、流程实例管理 API

**路径前缀**: `/api/camunda/process-instances`

### 2.1 查看流程实例详情

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-instances/{instanceId}` |
| 描述 | 根据实例 ID 获取详细信息 |

**查询参数**:

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| withVariables | boolean | false | 是否返回流程变量 |

---

### 2.2 挂起流程实例

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-instances/{instanceId}/suspend` |
| 描述 | 挂起运行中的流程实例及其任务 |

---

### 2.3 激活流程实例

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-instances/{instanceId}/activate` |
| 描述 | 恢复已挂起的流程实例 |

---

### 2.4 删除流程实例

| 属性 | 值 |
|------|-----|
| 路径 | `DELETE /api/camunda/process-instances/{instanceId}` |
| 描述 | 删除指定的流程实例 |

**查询参数**:

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| deleteReason | string | - | 删除原因 |
| skipCustomListeners | boolean | true | 是否跳过自定义监听器 |
| skipIoMappings | boolean | true | 是否跳过 IO 映射 |
| externallyTerminated | boolean | true | 是否标记为外部终止 |

---

### 2.5 查询流程变量

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-instances/{instanceId}/variables` |
| 描述 | 返回流程实例的全部流程变量 |

**查询参数**:

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| local | boolean | false | 是否只获取本地变量 |

---

### 2.6 获取单个流程变量

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/process-instances/{instanceId}/variables/{variableName}` |
| 描述 | 根据变量名查询指定流程变量 |

---

### 2.7 设置流程变量

| 属性 | 值 |
|------|-----|
| 路径 | `PUT /api/camunda/process-instances/{instanceId}/variables` |
| 描述 | 批量设置流程变量 |

**请求体** (`ProcessInstanceVariablesRequest`):

```json
{
  "variables": {
    "approved": true,
    "comments": "同意请假"
  },
  "local": false
}
```

---

### 2.8 删除流程变量

| 属性 | 值 |
|------|-----|
| 路径 | `DELETE /api/camunda/process-instances/{instanceId}/variables/{variableName}` |
| 描述 | 删除指定的流程变量 |

---

### 2.9 迁移流程实例

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/process-instances/{instanceId}/migrate` |
| 描述 | 将流程实例迁移到目标流程定义 |

**请求体** (`ProcessInstanceMigrationRequest`):

```json
{
  "targetProcessDefinitionId": "new-process-def-id",
  "activityMappings": [
    {
      "sourceActivityId": "task1",
      "targetActivityId": "newTask1"
    }
  ],
  "skipCustomListeners": false
}
```

---

## 三、任务管理 API

**路径前缀**: `/api/camunda/tasks`

### 3.1 分页查询任务

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/tasks` |
| 描述 | 支持租户、流程实例、任务分配人、候选用户/组过滤 |

**请求参数** (`TaskPageQuery`):

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| current | int | 否 | 当前页 |
| size | int | 否 | 每页大小 |
| tenantId | string | 否 | 租户 ID |
| processInstanceId | string | 否 | 流程实例 ID |
| assignee | string | 否 | 任务分配人 |
| candidateUser | string | 否 | 候选用户 |
| candidateGroup | string | 否 | 候选组 |
| name | string | 否 | 任务名称（模糊搜索） |

---

### 3.2 获取任务详情

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/tasks/{taskId}` |
| 描述 | 根据任务 ID 获取详细信息 |

**查询参数**:

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| withVariables | boolean | false | 是否返回任务变量 |

---

### 3.3 认领任务

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/tasks/{taskId}/claim` |
| 描述 | 将候选任务认领给自己或其他用户 |

**请求体** (`ClaimTaskRequest`):

```json
{
  "userId": "user-001"
}
```

---

### 3.4 释放任务

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/tasks/{taskId}/unclaim` |
| 描述 | 将已认领的任务释放，使其变为候选任务 |

---

### 3.5 完成任务

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/tasks/{taskId}/complete` |
| 描述 | 完成任务并推进流程实例 |

**请求体** (`CompleteTaskRequest`):

```json
{
  "variables": {
    "approved": true,
    "comments": "审批通过"
  }
}
```

---

### 3.6 委托任务

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/tasks/{taskId}/delegate` |
| 描述 | 将任务委托给其他用户处理 |

**请求体** (`DelegateTaskRequest`):

```json
{
  "userId": "user-002"
}
```

---

### 3.7 获取任务变量

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/tasks/{taskId}/variables` |
| 描述 | 返回任务的所有变量 |

---

### 3.8 设置任务变量

| 属性 | 值 |
|------|-----|
| 路径 | `PUT /api/camunda/tasks/{taskId}/variables` |
| 描述 | 设置或更新指定任务变量 |

**请求体** (`VariableUpsertRequest`):

```json
{
  "key": "reviewResult",
  "value": "approved",
  "local": false
}
```

---

### 3.9 删除任务变量

| 属性 | 值 |
|------|-----|
| 路径 | `DELETE /api/camunda/tasks/{taskId}/variables/{key}` |
| 描述 | 删除指定的任务变量 |

---

### 3.10 查询任务附件

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/tasks/{taskId}/attachments` |
| 描述 | 获取任务的所有附件信息 |

---

### 3.11 添加任务附件

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/tasks/{taskId}/attachments` |
| 描述 | 为任务添加新的附件 |

**请求体** (`AttachmentRequest`):

```json
{
  "name": "审批材料",
  "description": "请假申请材料",
  "type": "url",
  "url": "https://example.com/file.pdf"
}
```

---

### 3.12 查询任务评论

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/tasks/{taskId}/comments` |
| 描述 | 获取任务的所有评论信息 |

---

### 3.13 添加任务评论

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/tasks/{taskId}/comments` |
| 描述 | 为任务添加新的评论 |

**请求体** (`CommentRequest`):

```json
{
  "message": "已审核完成，待领导审批"
}
```

---

## 四、历史流程实例 API

**路径前缀**: `/api/camunda/history/process-instances`

### 4.1 历史流程实例分页查询

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/history/process-instances` |
| 描述 | 支持租户、业务键、流程定义、启动人、完成状态过滤 |

**请求参数** (`ProcessInstanceHistoryQuery`):

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| current | int | 否 | 当前页 |
| size | int | 否 | 每页大小 |
| tenantId | string | 否 | 租户 ID |
| businessKey | string | 否 | 业务键 |
| processDefinitionKey | string | 否 | 流程定义 Key |
| processDefinitionId | string | 否 | 流程定义 ID |
| startedBy | string | 否 | 启动人 |
| finished | boolean | 否 | 是否已完成 |

---

### 4.2 获取历史流程实例详情

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/history/process-instances/{instanceId}` |
| 描述 | 查看历史流程实例详情，包含流程变量与活动历史轨迹 |

---

### 4.3 查询历史流程实例状态

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/history/process-instances/{instanceId}/status` |
| 描述 | 查询历史流程实例的完成、未完成、终止状态 |

---

### 4.4 查询活动历史

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/history/process-instances/{instanceId}/activities` |
| 描述 | 分页查询指定历史流程实例的活动执行轨迹 |

**请求参数**:

| 参数名 | 类型 | 默认值 | 描述 |
|--------|------|--------|------|
| current | int | 1 | 当前页 |
| size | int | 10 | 每页大小（最大200） |

---

### 4.5 查询审计日志

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/history/process-instances/{instanceId}/audit-logs` |
| 描述 | 查询指定历史流程实例的用户操作审计日志 |

---

## 五、统计分析 API

**路径前缀**: `/api/camunda/statistics`

### 5.1 获取流程定义统计

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/statistics/process-definitions` |
| 描述 | 统计流程定义的部署数量、版本分布等信息 |

**请求参数** (`StatisticsQuery`):

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| tenantId | string | 否 | 租户 ID |
| startTime | datetime | 否 | 开始时间 |
| endTime | datetime | 否 | 结束时间 |

**响应** (`ProcessStatisticsDTO`):

```json
{
  "code": 200,
  "data": {
    "totalDeployments": 50,
    "totalDefinitions": 25,
    "activeDefinitions": 20,
    "suspendedDefinitions": 5
  }
}
```

---

### 5.2 获取流程实例统计

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/statistics/instances` |
| 描述 | 统计运行中、已完成、已终止的流程实例数量 |

**响应** (`InstanceStatisticsDTO`):

```json
{
  "code": 200,
  "data": {
    "runningInstances": 100,
    "completedInstances": 500,
    "terminatedInstances": 10
  }
}
```

---

### 5.3 获取任务统计

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/statistics/tasks` |
| 描述 | 统计待办、已完成、逾期任务数量 |

**响应** (`TaskStatisticsDTO`):

```json
{
  "code": 200,
  "data": {
    "pendingTasks": 150,
    "completedTasks": 800,
    "overdueTasks": 5
  }
}
```

---

### 5.4 获取工作流运行状态概览

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/statistics/overview` |
| 描述 | 获取流程、实例、任务的综合统计概览 |

---

## 六、表单模板管理 API

**路径前缀**: `/api/camunda/form-templates`

### 6.1 分页查询表单模板

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/form-templates` |
| 描述 | 支持租户、类型、状态过滤的表单模板分页查询 |

**请求参数** (`FormTemplatePageQuery`):

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| current | int | 否 | 当前页 |
| size | int | 否 | 每页大小 |
| tenantId | string | 否 | 租户 ID |
| type | string | 否 | 表单类型 |
| status | int | 否 | 状态 |
| keyword | string | 否 | 关键词（名称、描述模糊搜索） |

---

### 6.2 获取表单模板详情

| 属性 | 值 |
|------|-----|
| 路径 | `GET /api/camunda/form-templates/{templateId}` |
| 描述 | 根据模板 ID 获取表单模板的详细信息 |

---

### 6.3 创建表单模板

| 属性 | 值 |
|------|-----|
| 路径 | `POST /api/camunda/form-templates` |
| 描述 | 创建新的表单模板 |

**请求体** (`FormTemplateCreateRequest`):

```json
{
  "name": "请假申请表单",
  "description": "用于员工请假流程",
  "type": "FORM_JS",
  "content": "{...表单定义JSON...}",
  "tenantId": "tenant-001"
}
```

---

### 6.4 更新表单模板

| 属性 | 值 |
|------|-----|
| 路径 | `PUT /api/camunda/form-templates/{templateId}` |
| 描述 | 更新表单模板信息（自动增加版本号） |

**请求体** (`FormTemplateUpdateRequest`):

```json
{
  "name": "请假申请表单V2",
  "description": "更新后的请假表单",
  "content": "{...新的表单定义JSON...}"
}
```

---

### 6.5 删除表单模板

| 属性 | 值 |
|------|-----|
| 路径 | `DELETE /api/camunda/form-templates/{templateId}` |
| 描述 | 删除指定的表单模板（软删除） |

---

## 附录

### 统一响应格式

所有接口统一使用以下响应格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

### 错误码说明

| 错误码 | 描述 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 分页参数说明

| 参数 | 类型 | 默认值 | 最大值 | 描述 |
|------|------|--------|--------|------|
| current | int | 1 | - | 当前页码 |
| size | int | 10 | 200 | 每页记录数 |

---

**文档版本**: 1.0.0  
**更新日期**: 2025-12-11  
**维护者**: BaseBackend Team
