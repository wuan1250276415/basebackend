[根目录](../../CLAUDE.md) > **basebackend-logging**

# basebackend-logging

## 模块职责

结构化日志库。提供 Logback 结构化输出、审计日志模型、日志脱敏、ELK/Loki 集成配置。

## 对外接口

- 审计日志模型: `AuditLogEntry`
- Logback配置模板: `logback-spring.xml`, `logback-structured.xml`
- Docker监控集成: `docker-compose.monitoring.yml`

## 测试与质量

1个测试: AuditLogEntryTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
