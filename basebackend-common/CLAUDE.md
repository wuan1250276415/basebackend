[根目录](../../CLAUDE.md) > **basebackend-common**

# basebackend-common

## 模块职责

公共模块聚合父项目，拆分为 7 个子模块，提供企业级通用基础能力。所有业务模块均依赖此模块。

## 子模块结构

| 子模块 | artifactId | 职责 |
|--------|-----------|------|
| common-core | basebackend-common-core | 核心基类：统一响应体、异常体系、常量、基础Entity |
| common-dto | basebackend-common-dto | 跨模块共享DTO定义 |
| common-util | basebackend-common-util | 通用工具类 |
| common-context | basebackend-common-context | 上下文传递(线程/请求级) |
| common-security | basebackend-common-security | 安全基础：XSS防护、密钥管理、输入校验、脱敏 |
| common-starter | basebackend-common-starter | Spring Boot自动配置Starter |
| common-storage | basebackend-common-storage | 存储抽象层 |

## 测试与质量

- common-security: 3个测试 (SecretManagerTest, SafeStringValidatorTest, SanitizationUtilsTest)
- 其他子模块暂无独立测试

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
