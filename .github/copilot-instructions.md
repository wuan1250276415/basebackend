# Copilot 指南 — BaseBackend 仓库要点

此文件为 AI 编码代理的快速参考。

## 技术栈

- **语言**: Java 25
- **框架**: Spring Boot 4.0.3 + Spring Cloud 2025.1.1 + Spring Cloud Alibaba 2025.1.0.0
- **ORM**: MyBatis-Plus 3.5.16
- **缓存**: Redis + Redisson
- **消息队列**: RocketMQ 5.2.0
- **注册/配置**: Nacos 2.x
- **构建**: Maven 多模块（父 POM 在根 `pom.xml`）

## 项目结构（24 个顶层模块）

```
微服务: gateway / user-api / system-api / file-service / notification-service / observability-service
公共:   basebackend-common/ (16 个子模块: core/util/context/security/lock/idempotent/datascope/ratelimit/export/event/masking/audit/tree/storage/starter)
基础设施: jwt / database(5) / cache(3) / logging(4) / security / observability(5) / messaging / nacos
扩展:   ai / search / websocket / workflow
调度:   scheduler-parent/ (core/camunda)
通信:   api-model / service-client
工具:   code-generator / backup
```

## 快速命令

```bash
# 编译
mvn clean compile -DskipTests

# 运行测试（内存受限时）
mvn test -DforkCount=0

# 构建单个服务
mvn clean package -pl basebackend-gateway -am -DskipTests -B

# Docker 启动基础设施
docker-compose -f docker/compose/base/docker-compose.base.yml up -d
docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d nacos
```

## 关键约定

- **统一响应**: `Result<T>` / `PageResult<T>`（在 `common-core`）
- **实体基类**: `BaseEntity`（在 `database-core`）— id, createTime, updateTime, createBy, updateBy, deleted
- **AutoConfiguration**: 基础设施 `matchIfMissing=true`；高级/可选功能 `matchIfMissing=false`
- **所有 Bean**: 使用 `@ConditionalOnMissingBean` 支持业务层覆盖
- **Record**: 纯数据 DTO 用 Record；有继承/Builder/基类的保留 class
- **依赖**: 存储/DB 驱动、重型框架标记 `<optional>true</optional>`
- **服务通信**: `@HttpExchange` 声明式客户端（已移除 Feign）
- **JSON**: Jackson（已移除 fastjson2）
- **工具库**: hutool 按模块引入（已移除 hutool-all）
- **虚拟线程**: 已启用，避免 `synchronized` 和固定大小线程池

## 扩展模块启用方式

```yaml
basebackend.ai.enabled: true           # AI 基础设施
basebackend.search.enabled: true       # 全文搜索
basebackend.workflow.enabled: true     # 工作流引擎
basebackend.websocket.enabled: true    # WebSocket
```

## AI 代码生成约束

- 遵循 Java 25 语法（Record、模式匹配、增强 Switch、文本块）
- 新增模块须在根 `pom.xml` 的 `<modules>` 注册
- 修改公共模块后运行 `mvn clean compile -DskipTests` 验证
- 测试使用 JUnit 5 + AssertJ + Mockito，`@DisplayName` 用中文
- 不要引入 Spring AI（不稳定）、fastjson2、hutool-all、Feign
