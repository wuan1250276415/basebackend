[根目录](../../CLAUDE.md) > **basebackend-scheduler-parent**

# basebackend-scheduler-parent

## 模块职责

任务调度与工作流服务聚合模块。基于 Camunda BPM 7.21.0 工作流引擎，提供 BPMN 流程管理、任务调度、监控与性能优化。

## 子模块结构

| 子模块 | 职责 |
|--------|------|
| scheduler-camunda | 单一模块：包含调度核心基础设施（重试/幂等/熔断）+ Camunda 集成层（流程定义/实例/任务/历史/事件/表单模板/监控） |

## 包结构

### scheduler-camunda

**核心基础设施（原 scheduler-core）：**
- `com.basebackend.scheduler.core` — 重试、幂等、熔断基础设施
- `com.basebackend.scheduler.core.config` — 调度核心配置
- `com.basebackend.scheduler.core.circuitbreaker` — 熔断器服务
- `com.basebackend.scheduler.enums` — 枚举（ExecuteType/JobStatus/JobType/TimeExpressionType）
- `com.basebackend.scheduler.exception` — 调度异常定义
- `com.basebackend.scheduler.processor` — 任务处理器（缓存清理/数据库备份/健康检查）
- `com.basebackend.scheduler.util` — 日期工具

**Camunda 集成层：**
- `com.basebackend.scheduler.camunda.controller` — REST API 控制器（9个）
- `com.basebackend.scheduler.camunda.service` — 业务逻辑层（14个接口+实现）
- `com.basebackend.scheduler.camunda.dto` — 数据传输对象（继承模式：Detail extends Summary）
- `com.basebackend.scheduler.camunda.entity` — 数据库实体
- `com.basebackend.scheduler.camunda.mapper` — MyBatis Mapper
- `com.basebackend.scheduler.camunda.config` — Camunda 引擎配置
- `com.basebackend.scheduler.camunda.delegate` — 流程委托（邮件/微服务/审批/Saga补偿）
- `com.basebackend.scheduler.camunda.listener` — 全局BPMN解析/任务候选人监听器
- `com.basebackend.scheduler.camunda.exception` — 全局异常处理
- `com.basebackend.scheduler.monitoring` — 监控拦截器、健康检查、工作流指标
- `com.basebackend.scheduler.config` — 多数据源、Swagger、分页配置
- `com.basebackend.scheduler.exception` — 异常映射工具

## DTO 继承关系

| 子类（Detail） | 父类（Summary） |
|----------------|-----------------|
| ProcessDefinitionDetailDTO | ProcessDefinitionDTO |
| ProcessInstanceDetailDTO | ProcessInstanceDTO |
| HistoricProcessInstanceDetailDTO | HistoricProcessInstanceDTO |
| JobDetailDTO | JobDTO |
| TaskDetailDTO | TaskSummaryDTO |
| ProcessInstanceHistoryQuery | ProcessInstancePageQuery |

## 数据模型

迁移脚本: `V2.1` ~ `V3.3`
- Camunda初始化、表单模板、性能优化索引、元数据表、任务抄送、审计日志

## 测试与质量

- RetryTemplateTest(7), IdempotentCacheTest(8), CircuitBreakerServiceTest(8), TaskContextTest
- CamundaMetricsConfigurationTest(2), ProcessInstanceControllerTest(14, skipped)

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-23 | 方案C重构收尾 | DTO继承合并(6对)、清理performance包(8类)、合并scheduler-core到scheduler-camunda(单模块)、删除scheduler-old/scheduler-backup |
| 2026-02-23 | 模块重构 | 5模块合并为2模块(core+camunda)，删除死代码/表单引擎/冗余DTO，清理前后端未使用API |
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
