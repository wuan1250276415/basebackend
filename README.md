# BaseBackend — 企业级微服务基础架构

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?style=flat-square&logo=springboot" />
  <img src="https://img.shields.io/badge/Spring%20Cloud-2025.1.1-brightgreen?style=flat-square&logo=spring" />
  <img src="https://img.shields.io/badge/Modules-24-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/Tests-710+-green?style=flat-square" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
</p>

> 一个开箱即用的 Java 微服务基础架构，提供 **安全认证、数据库、缓存、消息队列、API 网关、AI 基础设施、全文搜索、工作流引擎、WebSocket 实时通信、可观测性** 等完整的企业级基础设施，让你专注于业务开发。

📖 **完整文档**: [GitHub Wiki](https://github.com/wuan1250276415/basebackend/wiki)

---

## ✨ 核心特性

| 特性 | 说明 |
|------|------|
| 🔐 **JWT 安全认证** | 双 Token 机制、密钥轮换、多设备管理、Token 黑名单、事件审计 |
| 🛡️ **权限控制** | RBAC 模型、@RequiresPermission / @RequiresRole / @DataScope 注解式权限 |
| 🔒 **分布式锁** | @DistributedLock 注解 + SpEL 表达式，Redis/内存双实现 |
| 🛡️ **幂等性** | @Idempotent 注解，TOKEN/PARAM/SPEL 三种策略防重复提交 |
| 📊 **数据权限** | @DataScope 注解 + MyBatis 拦截器，5 种数据范围自动过滤 |
| ⚡ **多级缓存** | L1 本地 + L2 Redis、热 Key 检测、布隆过滤器、缓存预热 |
| 🚦 **多算法限流** | @RateLimit 注解，滑动窗口/令牌桶/固定窗口，Redis/内存双实现 |
| 🤖 **AI 基础设施** | OpenAI/DeepSeek/通义千问 多模型适配、会话管理、RAG、Token 追踪 |
| 🔍 **全文搜索** | Elasticsearch/OpenSearch 集成、DSL 查询构建器、中文分词、索引管理 |
| ⚙️ **工作流引擎** | 流程定义/审批节点/条件分支/超时处理/任务查询/审批历史 |
| 💬 **WebSocket** | 实时通信、频道管理、用户多连接、心跳检测、消息路由 |
| 🏢 **多租户** | 数据库级租户隔离、租户解析策略、租户感知缓存、生命周期管理 |
| 🌐 **API 网关** | 12 个过滤器链、灰度发布、黑白名单、流量统计、动态跨域 |
| 📨 **消息队列** | RocketMQ 5.2.0，同步/异步/事务消息 |
| 📤 **数据导出** | CSV + EasyExcel 导出导入，异步大数据量导出 |
| 📁 **文件存储** | Local / MinIO / S3 / OSS 四种 Provider |
| 🔭 **可观测性** | OpenTelemetry + Prometheus + Loki + Tempo 全链路 |
| 🔑 **敏感数据加密** | @Sensitive 注解字段级 AES 加密，透明读写 |

---

## 🚀 快速开始

### 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 25+ |
| Maven | 3.8+ |
| MySQL | 8.0+ |
| Redis | 7.0+ |
| Docker | 20.10+ |

### 5 分钟启动

```bash
# 1. 克隆项目
git clone https://github.com/wuan1250276415/basebackend.git
cd basebackend

# 2. 启动基础设施
docker-compose -f docker/compose/base/docker-compose.base.yml up -d
docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d nacos

# 3. 上传 Nacos 配置
./bin/maintenance/upload-nacos-configs.sh

# 4. 编译项目
mvn clean package -DskipTests

# 5. 启动微服务
./bin/start/start-microservices.sh

# 6. 验证
./bin/test/verify-services.sh
```

> 💡 详细步骤请查看 [快速启动指南](https://github.com/wuan1250276415/basebackend/wiki/快速启动指南)

---

## 🏗️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **语言** | Java | 25 |
| **框架** | Spring Boot | 4.0.3 |
| **微服务** | Spring Cloud | 2025.1.1 |
| **微服务扩展** | Spring Cloud Alibaba | 2025.1.0.0 |
| **ORM** | MyBatis Plus | 3.5.16 |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis + Redisson | 7.x / 3.x |
| **消息队列** | RocketMQ | 5.2.0 |
| **注册/配置** | Nacos | 2.x |
| **网关** | Spring Cloud Gateway | 2025.1.1 |
| **文件存储** | MinIO / S3 / OSS | - |
| **可观测性** | OpenTelemetry + Prometheus + Loki + Tempo | - |
| **CI/CD** | GitHub Actions + Docker | - |

---

## 📁 项目结构

```
basebackend/                                    24 个顶层模块 · 68 个子模块 · 1,291 个 Java 文件
│
├── 🚀 微服务 (可部署)
│   ├── basebackend-gateway/                    # API 网关 — 12 过滤器 + 灰度 + 黑白名单 + 流量统计
│   ├── basebackend-user-api/                   # 用户服务 — 认证/用户/角色/资料
│   ├── basebackend-system-api/                 # 系统管理 — 部门/字典/日志/权限/监控
│   ├── basebackend-notification-service/       # 通知服务 — 邮件/短信/站内信
│   ├── basebackend-observability-service/      # 可观测性服务
│   └── basebackend-file-service/               # 文件服务 — Local/MinIO/S3/OSS
│
├── 📦 公共模块 (basebackend-common/)
│   ├── common-core/                            # 核心模型 (Result, PageResult, BusinessException)
│   ├── common-util/                            # 工具类 (JsonUtils, AssertUtils, IdGenerator)
│   ├── common-context/                         # 用户/租户上下文
│   ├── common-security/                        # 公共安全工具
│   ├── common-storage/                         # 文件存储 SPI
│   ├── common-lock/                            # 🔒 分布式锁 (@DistributedLock)
│   ├── common-idempotent/                      # 🛡️ 幂等性 (@Idempotent)
│   ├── common-datascope/                       # 📊 数据权限 (@DataScope)
│   ├── common-ratelimit/                       # 🚦 多算法限流 (@RateLimit)
│   ├── common-export/                          # 📤 导出 (CSV + EasyExcel + 异步)
│   ├── common-event/                           # 📨 可靠领域事件 (持久化 + 指数退避重试)
│   ├── common-masking/                         # 🎭 数据脱敏
│   ├── common-audit/                           # 📋 操作审计
│   ├── common-tree/                            # 🌳 树结构构建
│   └── common-starter/                         # 自动配置 Starter
│
├── 🔧 基础设施库
│   ├── basebackend-jwt/                        # JWT 认证 (双Token/黑名单/密钥轮换/多设备/审计)
│   ├── basebackend-database/                   # 数据库 (动态数据源/多租户/审计/加密/迁移)
│   │   ├── database-core/                      # MyBatis-Plus 核心
│   │   ├── database-security/                  # 字段加密 (@Sensitive)
│   │   ├── database-multitenant/               # 🏢 多租户 (租户解析/缓存/生命周期)
│   │   ├── database-audit/                     # 数据审计
│   │   └── database-migration/                 # Flyway 迁移
│   ├── basebackend-cache/                      # 缓存
│   │   ├── cache-core/                         # Redis 基础
│   │   ├── cache-advanced/                     # 多级缓存/热Key/布隆过滤器
│   │   └── cache-starter/                      # 自动配置
│   ├── basebackend-security/                   # 安全 (RBAC/动态权限/安全事件)
│   ├── basebackend-messaging/                  # 消息队列 (RocketMQ + Webhook)
│   ├── basebackend-observability/              # 可观测性 (OpenTelemetry)
│   │   ├── observability-core/                 # Metrics/Tracing 核心
│   │   ├── observability-alert/                # 告警规则引擎
│   │   ├── observability-slo/                  # SLO/SLI 定义
│   │   ├── observability-dashboard/            # 仪表盘配置
│   │   └── observability-starter/              # 自动配置
│   ├── basebackend-logging/                    # 日志
│   │   ├── logging-core/                       # 日志核心
│   │   ├── logging-monitoring/                 # 日志监控
│   │   ├── logging-access/                     # 访问日志
│   │   └── logging-starter/                    # 自动配置
│   └── basebackend-nacos/                      # 注册/配置中心聚合
│
├── 🤖 扩展模块
│   ├── basebackend-ai/                         # AI 基础设施 — OpenAI/DeepSeek/通义千问
│   ├── basebackend-search/                     # 全文搜索 — Elasticsearch/OpenSearch
│   ├── basebackend-workflow/                   # 工作流引擎 — 审批流/条件分支/超时处理
│   └── basebackend-websocket/                  # WebSocket — 实时通信/频道/消息路由
│
├── ⏰ 调度系统
│   └── basebackend-scheduler-parent/
│       ├── scheduler-core/                     # 调度核心 (重试/熔断/幂等)
│       └── scheduler-camunda/                  # Camunda BPM 集成
│
├── 🔗 服务通信
│   ├── basebackend-api-model/                  # API 数据模型
│   └── basebackend-service-client/             # @HttpExchange 声明式客户端
│
├── 🛠️ 工具
│   ├── basebackend-code-generator/             # 代码生成器
│   └── basebackend-backup/                     # 数据备份 (全量/增量/压缩/加密)
│
├── 🐳 docker/                                  # Docker Compose 部署
├── ⚙️ config/                                  # Nacos 配置文件
├── 📖 wiki/                                    # 项目 Wiki 文档 (27 页)
└── 📚 docs/                                    # 其他文档
```

---

## 💡 使用示例

### 🤖 AI 对话 — 多模型适配

```java
@Autowired
private OpenAiClient aiClient;

// 单轮对话
ChatResponse response = aiClient.chat("分析这段代码的性能瓶颈...");

// 流式输出 (SSE)
aiClient.streamChat(messages, chunk -> {
    System.out.print(chunk.content());
});

// 使用 @AIGenerate 注解自动生成
@AIGenerate(prompt = "为以下内容生成摘要: {{content}}")
public String generateSummary(String content) { return null; }
```

### 🔍 全文搜索 — DSL 查询构建器

```java
@Autowired
private SearchClient searchClient;

SearchQuery query = SearchQuery.builder("articles")
    .must(Condition.match("title", "Java虚拟线程"))
    .filter(Condition.term("status", "published"))
    .filter(Condition.range("createTime", "2024-01-01", null))
    .highlight("title", "content")
    .sortBy("createTime", SortOrder.DESC)
    .page(1, 20)
    .build();

SearchResult<Article> result = searchClient.search(query, Article.class);
```

### ⚙️ 工作流 — 审批流程

```java
@Autowired
private ProcessEngine processEngine;

// 定义流程
ProcessDefinition def = ProcessDefinition.builder("leave", "请假审批")
    .startNode("start", "提交申请")
    .conditionNode("days_check", "天数判断", List.of(
        ConditionBranch.of("days <= 3", "leader"),
        ConditionBranch.of("days > 3", "hr")
    ))
    .approvalNode("leader", "主管审批", "ROLE_LEADER")
    .approvalNode("hr", "HR审批", "ROLE_HR")
    .endNode("end", "审批完成")
    .transition("start", "days_check")
    .transition("leader", "end")
    .transition("hr", "end")
    .build();

processEngine.deploy(def);

// 启动流程
ProcessInstance inst = processEngine.startProcess("leave", "张三", Map.of("days", 5));

// 审批
processEngine.approve(inst.getInstanceId(), "HR王经理", "同意");
```

### 💬 WebSocket — 实时通信

```yaml
basebackend:
  websocket:
    enabled: true
    endpoint: /ws
    allowed-origins: "*"
    heartbeat-interval: 30s
```

```java
// 客户端连接
ws://localhost:8080/ws?userId=user123

// 发送消息 (JSON)
{ "type": "CHANNEL_MESSAGE", "channel": "room-1", "content": "Hello!" }

// 私信
{ "type": "PRIVATE", "to": "user456", "content": "Hi!" }
```

### 🔒 分布式锁

```java
@DistributedLock(key = "#order.id", waitTime = 5, leaseTime = 30)
public void processOrder(Order order) {
    // 自动加锁/解锁，支持 SpEL 表达式
}
```

### 🛡️ 幂等性 — 防重复提交

```java
@Idempotent(strategy = IdempotentStrategy.PARAM, timeout = 5)
@PostMapping("/submit")
public Result<?> submitOrder(@RequestBody OrderDTO dto) {
    // 相同参数 5 秒内重复请求自动拦截
}
```

### 📊 数据权限

```java
@DataScope(type = DataScopeType.DEPT_AND_BELOW)
public List<User> listUsers(UserQuery query) {
    // SQL 自动追加部门过滤条件
}
```

### 🔐 双 Token 认证

```java
// 登录 → 签发双 Token
String accessToken = jwtUtil.generateAccessToken(userId, claims);   // 30 分钟
String refreshToken = jwtUtil.generateRefreshToken(userId);         // 7 天

// 刷新 → 用 Refresh Token 换新的 Access Token
String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
```

### 🏢 多租户

```java
// 租户解析策略 — Header/Domain/Path 自动解析
// SQL 自动注入 tenant_id 过滤条件

TenantContextHolder.setTenantId("tenant-001");
userMapper.selectList(null); // → SELECT * FROM user WHERE tenant_id = 'tenant-001'
```

---

## 🌐 API 网关过滤器链

```
请求 → AuthenticationFilter       # JWT 认证
     → HeaderSanitizationFilter   # 请求头清洗
     → SecurityHeadersFilter      # 安全响应头
     → BlacklistFilter            # 🆕 IP/路径黑名单
     → SignatureVerifyFilter      # 接口签名验证
     → RequestSizeLimitFilter     # 请求体大小限制
     → IdempotencyFilter          # 幂等性检查
     → ApiVersionFilter           # API 版本路由
     → SlowRequestFilter          # 慢请求检测
     → TraceIdFilter              # 链路追踪 ID
     → AccessLogFilter            # 访问日志
     → ResponseCacheFilter        # 响应缓存
     → 后端微服务
```

---

## 📊 项目统计

| 指标 | 数量 |
|------|------|
| 顶层模块 | 24 |
| 子模块总计 | 68 |
| Java 源文件 | 1,291 |
| 测试文件 | 169 |
| 单元测试 | 710+ |
| 代码行数 | ~191K |

---

## 🐳 Docker 部署

```bash
# 一键启动全部基础设施 + 微服务
docker/compose/start-all.sh

# 或分层启动
docker-compose -f docker/compose/base/docker-compose.base.yml up -d           # MySQL + Redis
docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d # Nacos + RocketMQ
docker-compose -f docker/compose/observability/docker-compose.yml up -d        # Prometheus + Loki + Tempo + Grafana
docker-compose -f docker/compose/services/docker-compose.yml up -d             # 全部微服务
```

### 端口规划

| 服务 | 端口 |
|------|------|
| API Gateway | 8180 |
| User API | 8081 |
| System API | 8082 |
| Nacos | 8848 |
| MySQL | 3306 |
| Redis | 6379 |
| Prometheus | 9090 |
| Grafana | 3000 |

---

## 📖 文档

| 文档 | 链接 |
|------|------|
| 📖 **完整 Wiki** | [GitHub Wiki](https://github.com/wuan1250276415/basebackend/wiki) |
| 🚀 快速启动 | [快速启动指南](https://github.com/wuan1250276415/basebackend/wiki/快速启动指南) |
| 🏗️ 架构设计 | [整体架构](https://github.com/wuan1250276415/basebackend/wiki/整体架构) |
| 🔐 JWT 认证 | [JWT 认证体系](https://github.com/wuan1250276415/basebackend/wiki/JWT认证体系) |
| 🤖 AI 基础设施 | [AI 模块](https://github.com/wuan1250276415/basebackend/wiki/AI模块) |
| 🔍 全文搜索 | [搜索模块](https://github.com/wuan1250276415/basebackend/wiki/搜索模块) |
| ⚙️ 工作流引擎 | [工作流模块](https://github.com/wuan1250276415/basebackend/wiki/工作流模块) |
| 💬 WebSocket | [WebSocket 模块](https://github.com/wuan1250276415/basebackend/wiki/WebSocket模块) |
| 📝 注解速查 | [常用注解速查](https://github.com/wuan1250276415/basebackend/wiki/常用注解速查) |
| ⚙️ 配置参考 | [配置参考](https://github.com/wuan1250276415/basebackend/wiki/配置参考) |

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feat/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加新功能'`)
4. 推送分支 (`git push origin feat/amazing-feature`)
5. 发起 Pull Request

---

## 📄 License

[MIT License](LICENSE)
