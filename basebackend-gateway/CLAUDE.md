[根目录](../../CLAUDE.md) > **basebackend-gateway**

# basebackend-gateway

## 模块职责

Spring Cloud Gateway统一API入口。提供路由转发、限流、鉴权、负载均衡、CORS等网关功能。

## 入口与启动

- 基于Spring Cloud Gateway (WebFlux响应式)
- 依赖 common-core, common-util, jwt 模块
- Nacos服务发现集成

## 关键依赖

- Spring Cloud Gateway
- Spring Cloud Bootstrap
- basebackend-jwt (JWT验证)

## 相关文件

- Dockerfile: `Dockerfile`
- Nacos日志: `src/main/resources/nacos-logback.xml`

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
