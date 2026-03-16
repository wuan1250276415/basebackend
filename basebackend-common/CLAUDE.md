[根目录](../../CLAUDE.md) > **basebackend-common**

# basebackend-common

## 模块职责

公共模块聚合父项目，当前维护 15 个子模块，提供企业级通用基础能力。所有业务模块均依赖此模块。

## 子模块结构

- `basebackend-common-core`：核心模型、异常体系、基础枚举常量
- `basebackend-common-util`：通用工具类（JSON/Bean/日期/ID/IP 等）
- `basebackend-common-context`：用户/租户上下文传递与管理
- `basebackend-common-security`：安全能力（输入校验、密钥管理、基础防护）
- `basebackend-common-audit`：审计注解与审计切面
- `basebackend-common-masking`：敏感字段脱敏能力
- `basebackend-common-tree`：树结构构建与遍历
- `basebackend-common-starter`：Spring Boot 自动配置聚合模块
- `basebackend-common-storage`：统一存储抽象层
- `basebackend-common-lock`：分布式锁抽象与实现
- `basebackend-common-idempotent`：幂等控制注解与执行器
- `basebackend-common-datascope`：数据权限范围控制
- `basebackend-common-ratelimit`：限流组件（固定窗口/滑动窗口/令牌桶）
- `basebackend-common-export`：导入导出与异步任务能力
- `basebackend-common-event`：事件发布、存储与重试机制

## 测试与质量

- 各子模块均按需维护单元测试，建议通过 `mvn -f basebackend-common/pom.xml test` 统一验证。
- 新增模块（`audit/masking/tree`）已纳入聚合构建链路，避免 CI 覆盖盲区。

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
| 2026-03-06 | 更新模块清单 | 同步 15 个实际子模块并修正文档漂移 |
