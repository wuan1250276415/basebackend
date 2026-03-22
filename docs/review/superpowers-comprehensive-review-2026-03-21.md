# BaseBackend Comprehensive Review

审查日期：2026-03-21  
审查方式：基于 `superpowers` 工作流进行仓库级静态审查，并结合本地构建/测试验证  
适用范围：`/home/wuan/backProjects/basebackend`

## 审查结论

本次审查没有发现“整个仓库无法构建或测试失败”的阻断问题，但发现了 5 个高价值风险点，其中 1 个为当前交付形态级缺陷，2 个为安全/权限边界缺陷，1 个为运维能力真实性缺陷，1 个为治理文档失真问题。

最重要的结论有三点：

1. `basebackend-file-service` 当前不能作为 README 所宣称的“可部署微服务”直接启动。
2. `basebackend-file-service` 的认证上下文链路存在割裂，本地认证过滤器与业务代码读取的上下文来源不是同一套。
3. 文件服务部分高风险接口缺少服务层授权校验，日志/分享信息等接口还存在过度暴露问题。

## Findings

### 1. Critical: `basebackend-file-service` 被描述为可部署微服务，但当前产物不是可启动 Spring Boot 应用

**证据**

- README 将文件服务列为“微服务（可部署）”，见 `README.md:118-124`。
- `basebackend-file-service/pom.xml` 只有普通 `jar` 打包和依赖声明，没有 `spring-boot-maven-plugin` 的 `repackage` 配置，见 `basebackend-file-service/pom.xml:13-18`、`basebackend-file-service/pom.xml:19-207`。
- 模块源码内不存在 `@SpringBootApplication` 或 `main` 入口。
- 实际执行 `mvn -q -pl basebackend-file-service -DskipTests package` 后，生成的是普通 JAR。
- 实际执行 `java -jar basebackend-file-service/target/basebackend-file-service-1.0.0-SNAPSHOT.jar` 返回：`没有主清单属性`。

**影响**

- 当前模块无法像标准 Spring Boot 微服务那样被直接启动、编排或发布。
- README、模块职责说明与真实交付物不一致，会误导部署、联调和运维流程。
- 下游如果按“服务”理解它，集成时会在最晚阶段才暴露出不可运行的问题。

**建议**

- 如果它本来就是微服务：补齐应用启动类、Boot 重打包配置、最小可运行配置和健康检查。
- 如果它本来只是组件库：从 README 的“可部署微服务”列表中移除，并明确其真实角色。

### 2. High: 文件服务认证上下文链路割裂，本地认证过滤器无法给控制器/业务层提供其正在读取的用户上下文

**证据**

- 文件服务自定义认证过滤器会把认证结果写入 `com.basebackend.file.security.UserContextHolder`，见 `basebackend-file-service/src/main/java/com/basebackend/file/security/AuthenticationFilter.java:67-86`。
- 该过滤器内部明确调用的是本模块私有上下文持有器，见 `basebackend-file-service/src/main/java/com/basebackend/file/security/AuthenticationFilter.java:74-75`。
- 但 `FileController` 导入并读取的是 `com.basebackend.common.context.UserContextHolder`，见 `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:4`，并在大量接口中调用 `getUserId()` 或 `requireUserId()`，见 `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:72-90`、`basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:127-145`、`basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:164-202`。
- `common-starter` 的 `UserContextInterceptor` 只会在 Spring Security 已建立 `Authentication` 时写入通用上下文，见 `basebackend-common/basebackend-common-starter/src/main/java/com/basebackend/common/starter/interceptor/UserContextInterceptor.java:33-47`。而 `basebackend-file-service` 自身并未引入 `spring-boot-starter-security` 或 `basebackend-security`，只有 `spring-security-crypto`，见 `basebackend-file-service/pom.xml:81-85`。

**影响**

- 从源码逻辑上看，文件服务的“本地 JWT 认证”与“业务层拿到当前用户”不是同一条链。
- 一旦该模块被补齐启动入口并直接对外运行，控制器大概率会把已认证请求当成匿名请求处理，表现为：
  - 读取 `null` 用户 ID；
  - `requireUserId()` 直接抛异常；
  - 审计、限流、权限逻辑使用错误的主体信息。

**说明**

这是基于源码装配关系做出的高置信度推断。由于该模块当前本身不可启动，本次没有做完整运行态复现，但代码路径已经足以证明两套上下文系统没有接通。

**建议**

- 统一只保留一套上下文来源。
- 如果要沿用通用上下文，文件服务本地认证完成后应显式写入 `com.basebackend.common.context.UserContextHolder` 或接入统一 Spring Security 认证对象。
- 如果要保留文件服务私有上下文，则控制器/业务层不能再读取通用 `UserContextHolder`。

### 3. High: 文件服务若作为服务边界独立暴露，版本/权限/分享接口存在授权缺口和过度返回

**证据**

- 创建新版本没有校验当前用户是否具备 `WRITE` 权限，`createFileVersion()` 直接修改文件内容和版本状态，见 `basebackend-file-service/src/main/java/com/basebackend/file/service/FileVersionService.java:42-79`。
- 版本回退同样没有任何权限校验，见 `basebackend-file-service/src/main/java/com/basebackend/file/service/FileVersionService.java:85-105`。
- 查询版本历史没有权限校验，见 `basebackend-file-service/src/main/java/com/basebackend/file/service/FileVersionService.java:107-116`。
- 查询文件 ACL 列表没有权限校验，见 `basebackend-file-service/src/main/java/com/basebackend/file/service/FilePermissionService.java:77-86`。
- 控制器直接暴露这些服务能力，见：
  - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:264-267`
  - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:366-402`
  - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:408-411`
  - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:571-579`
  - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:602-615`
- `FileShare` 实体包含 `sharePassword` 字段，且 `getShareInfo()`/`createFileShare()` 直接返回该实体，见：
  - `basebackend-file-service/src/main/java/com/basebackend/file/entity/FileShare.java:28-31`
  - `basebackend-file-service/src/main/java/com/basebackend/file/service/FileShareService.java:58-79`
  - `basebackend-file-service/src/main/java/com/basebackend/file/service/FileShareService.java:85-115`
  - `basebackend-file-service/src/main/java/com/basebackend/file/controller/FileController.java:460-487`

**影响**

- 服务层自身并没有形成可靠的最小权限边界，过度依赖外部网关或上层约束。
- 只要未来路由、网关白名单、内部调用路径或管理接口稍有放宽，这些缺口就会立刻变成真实授权绕过。
- 分享接口当前会把哈希后的分享密码一并返回给客户端，这虽然不是明文泄露，但仍属于不必要的敏感字段暴露。

**建议**

- 在 `FileVersionService` 和 `FilePermissionService` 内部补齐 `READ/WRITE/SHARE` 校验，不要只把授权责任放在 Controller 或网关。
- 为分享信息定义专用 DTO，显式排除 `sharePassword` 等服务端内部字段。
- 给这些边界补集成测试，而不是只测纯业务 happy path。

### 4. Medium: 可观测性服务的日志查询 API 返回的是模拟数据，不是真实日志系统结果

**证据**

- 控制器以正式 API 形式暴露日志搜索、统计和 tail 接口，见 `basebackend-observability-service/src/main/java/com/basebackend/observability/controller/LogController.java:32-69`。
- 但实现层明确写着“返回 mock data”，见：
  - `basebackend-observability-service/src/main/java/com/basebackend/observability/service/impl/LogQueryServiceImpl.java:31-39`
  - `basebackend-observability-service/src/main/java/com/basebackend/observability/service/impl/LogQueryServiceImpl.java:65-76`
  - `basebackend-observability-service/src/main/java/com/basebackend/observability/service/impl/LogQueryServiceImpl.java:80-87`

**影响**

- 上层调用方会误以为系统已经具备真实日志检索能力。
- 运维排障时可能拿到“看起来结构正确、实际上无业务价值”的结果。
- 这类“半成品 API”比明确未实现更危险，因为它会制造错误信心。

**建议**

- 在接入 Loki / ELK 前，至少把接口标记为实验态或直接下线。
- 如果暂时保留，响应中应显式标明 `mock=true`，避免误用。

### 5. Medium: 仓库治理文档与真实工程状态已经明显偏离

**证据**

- 根 `AGENTS.md` 仍要求使用 `bin/` 目录下的启动/测试脚本，并声明技术基线为 Java 17、Spring Boot 3，见 `AGENTS.md:6-18`。
- 实际仓库根目录没有 `bin/`，本次检查仅找到：
  - `docker/compose/start-all.sh`
  - `config/nacos-configs/import-nacos-configs.sh`
- 实际父 POM 技术基线已经是 Java 25、Spring Boot 4.0.3、Spring Cloud 2025.1.1，见 `pom.xml:50-62`。
- README 也已经按 Java 25 / Boot 4 书写，见 `README.md:4-6`、`README.md:53-57`、`README.md:97-105`。

**影响**

- 新成员会同时看到两套互相冲突的“官方入口”。
- 自动化代理、脚本维护者和人工排障都会被过时指引误导。
- 这类治理失真会降低后续审查与修复动作的可信度。

**建议**

- 把 `AGENTS.md` 更新到与当前仓库一致，尤其是启动入口、测试命令、技术版本基线。
- 对已废弃目录和脚本做一次集中清理，减少“历史指令残留”。

## 构建与验证

本次实际执行并确认的命令如下：

- `mvn -q -DskipTests validate`
  - 结果：通过
- `mvn -q test`
  - 结果：通过
  - 统计：286 份 surefire 报告，1506 个测试，0 failures，0 errors，63 skipped
- `mvn -q -pl basebackend-file-service -DskipTests package`
  - 结果：通过
- `java -jar basebackend-file-service/target/basebackend-file-service-1.0.0-SNAPSHOT.jar`
  - 结果：失败，提示 JAR 中没有主清单属性

## 其他观察

- 当前测试基线是健康的，但测试输出噪音较大。
- `mvn test` 日志中多次出现 Mockito 在 JDK 25 下动态附加 agent 的警告，说明未来 JDK 进一步收紧时，测试链路可能需要显式配置 Mockito agent。
- README 徽章显示 `Tests-710+`，而本次本地统计到的 surefire 测试数为 1506，说明对外展示信息也已经滞后。

## 建议的修复优先级

### P0

- 明确 `basebackend-file-service` 的产品定位。
- 如果它是微服务，先补齐启动入口和可运行工件，再谈后续安全与权限修复。

### P1

- 统一文件服务认证上下文来源，打通“认证结果 -> 业务层用户上下文”的链路。
- 为版本创建、版本回退、版本历史、权限列表、操作日志、预览地址等接口补齐服务层权限校验。
- 停止对外返回 `FileShare.sharePassword`。

### P2

- 将 observability 的日志 API 从 mock 实现替换为真实后端，或至少显式降级为实验能力。
- 修正文档和仓库治理说明，清理过时启动脚本指引。

## 总体判断

这个仓库当前的主要问题不是“代码完全不能用”，而是“部分模块已经演化得比文档快，且有少数关键边界没有随着功能扩张同步收紧”。  
从可交付性角度看，最需要优先处理的是文件服务：它同时占据了部署形态、认证链路和授权边界三个高风险面。
