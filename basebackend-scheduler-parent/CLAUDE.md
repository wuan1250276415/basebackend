[根目录](../../CLAUDE.md) > **basebackend-scheduler-parent**

# basebackend-scheduler-parent

## 模块职责

任务调度与工作流服务聚合模块。基于 PowerJob 4.3.8 任务调度 + Camunda BPM 7.21.0 工作流引擎。

## 子模块结构

| 子模块 | 职责 |
|--------|------|
| scheduler-core | 调度核心：重试/幂等/熔断、拓扑排序、工作流引擎、DAG执行器、任务处理器（缓存清理/健康检查/数据库备份）、增强指标采集 |
| scheduler-camunda | Camunda 集成层：流程定义/实例/任务/历史/事件/表单模板控制器、流程统计、监控指标、性能优化、健康检查 |

## 包结构

### scheduler-core
- `com.basebackend.scheduler.core` — 重试、幂等、熔断基础设施
- `com.basebackend.scheduler.config` — PowerJob 配置
- `com.basebackend.scheduler.model` — 任务上下文模型
- `com.basebackend.scheduler.util` — 日期工具
- `com.basebackend.scheduler.processor` — 任务处理器（缓存/数据库/健康检查）
- `com.basebackend.scheduler.metrics` — 增强指标采集器
- `com.basebackend.scheduler.workflow` — 工作流领域模型（定义/节点/边/实例/日志）
- `com.basebackend.scheduler.engine` — DAG 工作流执行器
- `com.basebackend.scheduler.persistence` — 工作流实例持久化（MyBatis Plus）

### scheduler-camunda
- `com.basebackend.scheduler.camunda.controller` — REST API 控制器（9个）
- `com.basebackend.scheduler.camunda.service` — 业务逻辑层
- `com.basebackend.scheduler.camunda.dto` — 数据传输对象
- `com.basebackend.scheduler.camunda.entity` — 数据库实体
- `com.basebackend.scheduler.camunda.mapper` — MyBatis Mapper
- `com.basebackend.scheduler.camunda.config` — Camunda 引擎配置
- `com.basebackend.scheduler.camunda.exception` — 全局异常处理
- `com.basebackend.scheduler.monitoring` — 监控拦截器、健康检查、工作流指标
- `com.basebackend.scheduler.performance` — 性能优化（异步/批量/缓存/分页/数据库）

## 数据模型

迁移脚本: scheduler-core `V1.0`（工作流实例表）, scheduler-camunda `V2.1` ~ `V3.3`
- 工作流实例表、Camunda初始化、表单模板、性能优化索引、元数据表、任务抄送、审计日志

## 测试与质量

scheduler-core: RetryTemplateTest(7), IdempotentCacheTest(8), CircuitBreakerServiceTest(8)
scheduler-camunda: CamundaMetricsConfigurationTest(2), ProcessInstanceControllerTest(14, skipped)
遗留测试在 `basebackend-scheduler-old` 和 `basebackend-scheduler-backup`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-23 | 模块重构 | 5模块合并为2模块(core+camunda)，删除死代码/表单引擎/冗余DTO，清理前后端未使用API |
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
