# Repository Guidelines

## 项目结构与模块
- 根目录为 Maven 多模块工程，统一由 `pom.xml` 管理版本与插件。
- 核心业务模块位于 `basebackend-*` 目录（如 `basebackend-gateway`、`basebackend-security`、`basebackend-file-service`）；共用代码放在 `basebackend-common`、`basebackend-database`、`basebackend-cache`、`basebackend-logging` 等基础包。
- 脚本在 `bin/` 下：`start/` 启动、`test/` 验证、`maintenance/` 维护与配置上传。
- Docker 相关位于 `docker/compose/`，配置模板在 `config/`，文档在 `docs/`，CI 配置在 `.github/`。

## 构建、测试与本地运行
- `mvn clean package -DskipTests`：完整打包所有模块，跳过测试，常用于快速校验依赖。
- `mvn clean verify`：含单元测试与集成校验，提交前应执行，保证主干稳定。
- `./bin/start/start-microservices.sh`（Linux/macOS）或 `bin\\start\\start-all.bat`（Windows）：本地一次性启动全部微服务。
- `./bin/test/verify-services.sh` 或 `bin\\test\\health-check.bat`：基本健康检查；如依赖 Nacos/Redis/MySQL，请先用 `docker-compose -f docker/compose/base/docker-compose.base.yml up -d` 启动基础设施。

## 编码风格与命名
- 语言要求：Java 17，Spring Boot 3；保持类、接口使用驼峰，包名小写点分隔；常量全大写下划线。
- 缩进 4 空格，避免 Tab；行宽建议 ≤120。
- 格式化/静态检查：遵循 `checkstyle.xml`、`spotbugs.xml`，提交前执行 `mvn fmt:format`（若已配置 fmt 插件）与 `mvn spotbugs:check`/`mvn checkstyle:check`。
- 资源文件（YAML/SQL）保持 UTF-8，无 BOM；配置示例放入 `config/env/`，不要提交敏感值。

## 测试准则
- 测试框架：JUnit 5，Mock 选用 Mockito；命名模式 `*Test`，方法名表意（given_when_then）。
- 编写新功能时同时补充单元测试，覆盖核心逻辑与异常分支；公共库变更需在对应模块新增/更新测试。
- 变更前后至少运行受影响模块的 `mvn -pl <module> test`；如调整跨模块依赖，建议运行顶层 `mvn verify`。

## 提交与 Pull Request
- Commit 信息保持简洁动词句式（例：`feat: add order query API`，`fix: handle null tenant id`），对多模块变更可在消息中注明模块名。
- PR 描述需说明变更目的、主要改动、测试结果；如涉及接口或配置更新，附带示例请求/响应或配置片段。
- 关联缺陷或需求时在描述中引用 Issue 编号；如有界面或接口行为变化，请附截图或 curl 示例。

## 安全与配置
- 不要提交真实凭据、私钥或生产端点；使用 `.env`/`config/env/*.template` 维护占位符。
- 上传 Nacos 配置请使用 `./bin/maintenance/upload-nacos-configs.sh`，避免手工遗漏；如需本地覆盖，明确标注环境后缀（如 `-dev`）。
