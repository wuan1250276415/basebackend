# Camunda 7 工作流引擎集成说明

## 📋 集成概览

本项目已成功集成 **Camunda 7.21.0** 工作流引擎，提供完整的 BPMN 2.0 流程编排能力。

### ✅ 已完成的功能

1. **核心配置**
   - ✅ Camunda BPM Engine 配置
   - ✅ MySQL 数据库集成
   - ✅ 管理员用户自动创建
   - ✅ 流程自动部署机制

2. **管理服务**
   - ✅ 流程定义管理服务（ProcessDefinitionService）
   - ✅ 流程实例管理服务（ProcessInstanceService）
   - ✅ 任务管理服务（TaskManagementService）

3. **REST API**
   - ✅ 流程定义 API（查询、部署、挂起、激活）
   - ✅ 流程实例 API（启动、查询、变量管理）
   - ✅ 任务 API（认领、完成、委派、转办）

4. **示例流程**
   - ✅ 订单审批流程（order-approval-process.bpmn）
   - ✅ 数据同步流程（data-sync-process.bpmn）
   - ✅ 微服务编排流程（microservice-orchestration.bpmn）

5. **JavaDelegate 实现**
   - ✅ 邮件发送委托（SendEmailDelegate）
   - ✅ 数据同步委托（DataSyncDelegate）
   - ✅ 订单审批委托（OrderApprovalDelegate）
   - ✅ 微服务调用委托（MicroserviceCallDelegate）

6. **管理界面**
   - ✅ Camunda Cockpit（流程监控）
   - ✅ Camunda Tasklist（任务管理）
   - ✅ Camunda Admin（系统管理）

---

## 🏗️ 项目结构

```
basebackend-scheduler/
├── src/main/java/com/basebackend/scheduler/
│   ├── SchedulerApplication.java           # 启动类
│   └── camunda/
│       ├── config/                          # Camunda 配置
│       │   ├── CamundaConfig.java
│       │   ├── CamundaProperties.java
│       │   └── CamundaAdminInitializer.java
│       ├── service/                         # 业务服务
│       │   ├── ProcessDefinitionService.java
│       │   ├── ProcessInstanceService.java
│       │   └── TaskManagementService.java
│       ├── controller/                      # REST 控制器
│       │   ├── ProcessDefinitionController.java
│       │   ├── ProcessInstanceController.java
│       │   └── TaskController.java
│       ├── dto/                             # 数据传输对象
│       │   ├── ProcessDefinitionDTO.java
│       │   ├── ProcessInstanceDTO.java
│       │   └── TaskDTO.java
│       └── delegate/                        # 任务委托
│           ├── SendEmailDelegate.java
│           ├── DataSyncDelegate.java
│           ├── OrderApprovalDelegate.java
│           └── MicroserviceCallDelegate.java
├── src/main/resources/
│   ├── processes/                           # BPMN 流程定义
│   │   ├── order-approval-process.bpmn
│   │   ├── data-sync-process.bpmn
│   │   └── microservice-orchestration.bpmn
│   ├── db/migration/                        # 数据库迁移
│   │   └── V2.0__camunda_workflow_init.sql
│   ├── application-scheduler.yml            # Scheduler 配置
│   └── application-camunda.yml              # Camunda 详细配置
└── pom.xml                                  # Maven 依赖
```

---

## 🚀 快速开始

### 1. 启动服务

```bash
# 构建项目
mvn clean install -DskipTests

# 启动 scheduler 服务
cd basebackend-scheduler
mvn spring-boot:run
```

### 2. 访问管理界面

- **地址**: http://localhost:8085/scheduler/camunda/app/
- **用户名**: admin
- **密码**: admin

### 3. 测试 API

```bash
# 查询流程定义
curl http://localhost:8085/scheduler/api/workflow/definitions

# 启动流程实例
curl -X POST http://localhost:8085/scheduler/api/workflow/instances/start \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "order-approval-process",
    "businessKey": "ORDER-001",
    "variables": {
      "orderId": "ORDER-001",
      "amount": 8000,
      "approver": "admin"
    }
  }'
```

---

## 📚 文档

- **使用指南**: [docs/CAMUNDA-GUIDE.md](../docs/CAMUNDA-GUIDE.md)
- **快速开始**: [docs/CAMUNDA-QUICKSTART.md](../docs/CAMUNDA-QUICKSTART.md)

---

## 🔧 配置说明

### application-scheduler.yml

```yaml
camunda:
  bpm:
    enabled: true
    admin:
      id: admin
      password: ${CAMUNDA_ADMIN_PASSWORD:admin}
    history-level: full
    database:
      schema-update: true
    webapp:
      enabled: true
    rest:
      enabled: true
```

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| CAMUNDA_ADMIN_PASSWORD | 管理员密码 | admin |
| SPRING_DATASOURCE_URL | 数据库连接 | jdbc:mysql://localhost:3306/basebackend |
| SPRING_DATASOURCE_USERNAME | 数据库用户 | root |
| SPRING_DATASOURCE_PASSWORD | 数据库密码 | root |

---

## 🎯 使用场景

### 1. 长流程编排
- 订单审批流程（支持多级审批）
- 请假申请流程
- 采购审批流程

### 2. 数据处理流水线
- ETL 数据同步
- 数据清洗和转换
- 批量数据导入导出

### 3. 微服务编排
- 分布式事务协调
- Saga 模式实现
- 跨服务业务流程

### 4. 定时任务调度
- 定时报表生成
- 定时数据备份
- 定时清理任务

---

## 📊 API 端点总览

### 流程定义管理
- `GET /api/workflow/definitions` - 查询所有流程定义
- `GET /api/workflow/definitions/{id}` - 查询流程定义
- `GET /api/workflow/definitions/key/{key}` - 根据Key查询
- `POST /api/workflow/definitions` - 部署流程定义
- `PUT /api/workflow/definitions/{id}/suspend` - 挂起流程
- `PUT /api/workflow/definitions/{id}/activate` - 激活流程
- `GET /api/workflow/definitions/{id}/xml` - 获取流程XML
- `GET /api/workflow/definitions/{id}/diagram` - 获取流程图

### 流程实例管理
- `POST /api/workflow/instances/start` - 启动流程实例
- `GET /api/workflow/instances/running` - 查询运行中的实例
- `GET /api/workflow/instances/{id}` - 查询流程实例
- `GET /api/workflow/instances/business-key/{key}` - 根据业务键查询
- `PUT /api/workflow/instances/{id}/variables` - 设置变量
- `GET /api/workflow/instances/{id}/variables` - 获取变量
- `PUT /api/workflow/instances/{id}/suspend` - 挂起实例
- `PUT /api/workflow/instances/{id}/activate` - 激活实例
- `DELETE /api/workflow/instances/{id}` - 删除实例

### 任务管理
- `GET /api/workflow/tasks/pending/{assignee}` - 查询待办任务
- `GET /api/workflow/tasks/candidate/{user}` - 查询候选任务
- `GET /api/workflow/tasks/{id}` - 查询任务详情
- `POST /api/workflow/tasks/{id}/complete` - 完成任务
- `POST /api/workflow/tasks/{id}/claim` - 认领任务
- `POST /api/workflow/tasks/{id}/unclaim` - 取消认领
- `POST /api/workflow/tasks/{id}/delegate` - 委派任务
- `POST /api/workflow/tasks/{id}/assign` - 转办任务
- `PUT /api/workflow/tasks/{id}/variables` - 设置任务变量
- `GET /api/workflow/tasks/{id}/variables` - 获取任务变量

---

## 🔍 监控和观测

Camunda 已集成到项目的可观测性体系中：

1. **指标收集**: 通过 Prometheus 采集流程执行指标
2. **日志记录**: 所有流程操作记录到结构化日志
3. **链路追踪**: 支持 OpenTelemetry 分布式追踪
4. **健康检查**: `/actuator/health` 包含 Camunda 健康状态

---

## 🛠️ 开发工具

### Camunda Modeler
- **下载地址**: https://camunda.com/download/modeler/
- **用途**: 可视化设计 BPMN 流程图
- **支持**: Windows、macOS、Linux

### 推荐插件
- **VS Code BPMN Editor**: 在 VS Code 中编辑 BPMN
- **IntelliJ IDEA Camunda Plugin**: IDEA 中的 Camunda 支持

---

## ⚠️ 注意事项

1. **数据库表**: Camunda 会自动创建约 70 张表，表名以 `ACT_` 开头
2. **历史数据**: 建议定期清理历史数据以优化性能
3. **变量存储**: 避免在流程变量中存储大对象
4. **异步任务**: 耗时操作建议使用异步任务
5. **错误处理**: 为关键任务添加错误边界事件

---

## 📈 性能优化

1. **历史级别**: 根据需要调整 `history-level`（none/activity/audit/full）
2. **作业执行器**: 调整线程池大小适应并发需求
3. **缓存配置**: 启用流程定义缓存
4. **批量操作**: 使用批量 API 提高效率
5. **数据库优化**: 为 Camunda 表添加合适的索引

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request 来改进工作流引擎集成。

---

## 📄 许可证

本项目采用 MIT 许可证。
