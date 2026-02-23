[根目录](../../CLAUDE.md) > **basebackend-cache**

# basebackend-cache

## 模块职责

企业级多级缓存库。支持注解驱动的本地+Redis多级缓存、分布式锁、缓存预热、缓存键生成器、指标监控、容错降级。

## 入口与启动

- 库模块，通过Spring Boot自动配置加载
- 注解入口: `@Cacheable`, `@CachePut`, `@CacheEvict`

## 对外接口

- 注解驱动缓存: `com.basebackend.cache.annotation.*`
- 分布式锁API
- 缓存预热API
- 缓存键生成器
- 缓存指标服务

## 关键依赖

- Redisson 3.24.3
- Caffeine (本地缓存)

## 测试与质量

11个测试类，覆盖:
- 配置: CachePropertiesTest, CacheMetricsConfigTest, MultiLevelCacheConfigTest
- 序列化: SerializerRoundTripPropertyTest, SerializerErrorHandlingPropertyTest
- 指标: CacheMetricsServiceTest
- 管理: CacheEvictionManagerTest
- 工具: CacheKeyGeneratorTest
- 集成: MultiLevelCacheIntegrationTest, DistributedLockIntegrationTest, CacheWarmingRealIntegrationTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
