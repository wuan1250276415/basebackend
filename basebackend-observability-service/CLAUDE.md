[根目录](../../CLAUDE.md) > **basebackend-observability-service**

# basebackend-observability-service

## 模块职责

可观测性微服务。提供日志查询、指标查询、链路追踪查询、告警管理等运维能力的独立服务端点。

## 入口与启动

- 独立微服务，含Dockerfile
- Nacos服务注册

## 数据模型

- `src/main/resources/sql/schema.sql`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
