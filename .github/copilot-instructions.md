# Copilot 指南 — Base Backend 仓库要点

此文件为 AI 编码代理的快速参考，聚焦于能让你立刻在此仓库中高效工作的“可发现”信息与具体示例。

- **工程类型**: Maven 多模块 Java (父 POM 在根 `pom.xml`)；前端位于 `basebackend-admin-web`（Vite/React）。
- **核心技术**: Java 17, Spring Boot, Spring Cloud, MyBatis-Plus, Redis/Redisson, RocketMQ, Nacos 配置中心。

- **启动 / 编译 快速命令**:
  - 启动基础 infra (Docker): `cd docker/compose && ./start-all.sh`
  - 导入 Nacos 配置: `cd config/nacos-configs && ./import-nacos-configs.sh`（Windows PowerShell: `./import-nacos-configs.ps1`）
  - 编译后端: `mvn clean install -DskipTests`（在仓库根目录）
  - 启动某个服务: 进入模块目录并运行 `mvn spring-boot:run`（示例: `cd basebackend-gateway && mvn spring-boot:run`）
  - 前端本地启动: `cd basebackend-admin-web && npm install && npm run dev` 或使用 `pnpm`/`yarn` 视环境而定

- **关键文件/目录（优先查看）**:
  - 根 `README.md`, `QUICKSTART.md` — 启动步骤与健康检查脚本示例
  - `pom.xml` (根) — 模块列表、Maven 属性和依赖管理（添加模块时必须修改此处）
  - `docker/compose/` — 本地依赖服务的 compose 与启动脚本
  - `config/nacos-configs/` — Nacos 配置导入脚本与示例
  - `docs/` — 架构、模块说明、开发与部署指南
  - `bin/maintenance/health-check.sh` — 常用健康检查脚本

- **项目约定（可被 AI 直接依赖的规则）**:
  - 所有服务由父 POM管理；新增服务必须在根 `pom.xml` 的 `<modules>` 中注册。
  - 统一响应封装: `Result` / `PageResult`（位于 `basebackend-common`）— API 返回应遵循此结构。
  - 实体基类: `BaseEntity`（在 `basebackend-database`）提供通用字段（id, createTime, updateTime, createBy, updateBy, deleted）。
  - 全局异常处理与常量在 `basebackend-common`，请复用已有异常类型与状态码。
  - 配置中心: 使用 Nacos；敏感配置建议从环境或 Nacos 加载（参见 `config/nacos-configs`）。

- **常见开发工作流 / 修改示例**:
  - 添加新微服务模块：
    1. 在仓库根创建模块目录并添加 `pom.xml`（继承父 POM）。
    2. 在根 `pom.xml` 的 `<modules>` 添加模块名。
    3. 在 `basebackend-gateway` 中添加路由/过滤器配置以暴露新服务（或修改网关配置文件）。
  - 变更数据库实体：继承 `BaseEntity` 并只添加业务字段，避免重复通用字段。
  - 调用 MQ：使用 `basebackend-messaging` 的生产者/消费者封装，遵循已有 topic/tag 使用方式。

- **前端注意点**:
  - 前端 SDK 生成脚本: `basebackend-admin-web` 中的 `scripts/generate-sdk.mjs`（用于从后端 OpenAPI 生成 JS/TS 客户端）。
  - 前端构建: `npm run build` 使用 Vite（`package.json` 在 `basebackend-admin-web`）。

- **调试/测试提示（基于可发现脚本）**:
  - 若依赖未启动可先用 Docker Compose 本地运行所有 infra，再运行服务。
  - 健康检查：访问网关或服务的 `/actuator/health`，或运行 `./bin/maintenance/health-check.sh`。
  - 日志位置：项目根 `logs/`，Docker 服务日志使用 `docker logs -f <container>`。

- **集成点与注意的外部依赖**:
  - Nacos: 配置中心（请优先查看 `config/nacos-configs` 导入脚本），默认端口 `8848`。
  - MySQL、Redis、RocketMQ: 在 `docker/compose` 中有示例容器配置；生产环境请替换为托管服务并调整 `application-*.yml`。

- **对 AI 的约束性建议（生成/修改代码时）**:
  - 遵循父 POM 中的依赖版本与 Java 版本（Java 17）。
  - 修改公共模型或 `Result` 时，务必同步更新所有使用处并运行编译（`mvn -DskipTests`）以捕获编译断裂。
  - 新增模块时不要直接改 gateway 路由文件之外的运行时配置，先提出变更点并征求人工审查。

如果这份说明中有遗漏或你希望补充某些模块的深度示例（例如 `basebackend-security` 或 `basebackend-file-service` 的实现细节），请告诉我要加深的子模块，我会把具体文件引用和示例代码片段补上。
