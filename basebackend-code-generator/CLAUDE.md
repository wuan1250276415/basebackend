[根目录](../../CLAUDE.md) > **basebackend-code-generator**

# basebackend-code-generator

## 模块职责

代码生成器。基于 FreeMarker / Velocity 模板引擎，根据数据库表结构自动生成 Entity、Mapper、Service、Controller 等代码。

## 数据模型

迁移: `V2.0__create_generator_tables.sql`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
