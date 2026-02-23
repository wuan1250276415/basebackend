[根目录](../../CLAUDE.md) > **basebackend-feature-toggle**

# basebackend-feature-toggle

## 模块职责

Feature Toggle / A/B 测试库。支持 Unleash 和 Flagsmith 两种后端，提供功能开关、渐进式发布、A/B 测试分流能力。

## 对外接口

- Feature Toggle 注解与API
- A/B 测试分配器
- 多后端适配(Unleash/Flagsmith)

## 关键依赖

- Unleash Client 9.2.2
- Flagsmith Client 7.2.0

## 测试与质量

3个测试: BaseBackendFeatureToggleApplicationTest, ABTestAssignerPerformanceTest, HashAlgorithmPerformanceTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
