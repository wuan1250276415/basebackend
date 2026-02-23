# BaseBackend — 企业级微服务基础架构

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.1.5-brightgreen?style=flat-square&logo=springboot" />
  <img src="https://img.shields.io/badge/Spring%20Cloud-2022.0.4-brightgreen?style=flat-square&logo=spring" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
</p>

> 一个开箱即用的 Java 微服务基础架构，提供 **安全认证、数据库、缓存、消息队列、API 网关、工作流调度、可观测性** 等完整的企业级基础设施，让你专注于业务开发。

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
| 📤 **数据导出** | CSV + EasyExcel 导出导入，异步大数据量导出 |
| 📨 **可靠事件** | DomainEvent 持久化 + 指数退避重试 + 事务提交后发布 |
| 🔄 **分布式事务** | Seata AT 模式，自动补偿 |
| 📡 **消息队列** | RocketMQ 5.2.0，同步/异步/事务消息 |
| 🌐 **API 网关** | 12 个过滤器链、灰度发布、动态路由、签名验证 |
| ⏰ **工作流调度** | Camunda BPM，流程定义/任务管理/历史查询 |
| 📁 **文件存储** | Local / MinIO / S3 / OSS 四种 Provider |
| 🔭 **可观测性** | OpenTelemetry + Prometheus + Loki + Tempo 全链路 |
| 🏢 **多租户** | 数据库级租户隔离，自动注入租户条件 |
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
| **前端** | React + TypeScript + Ant Design 5 | 18 |
| **ORM** | MyBatis Plus | 3.5.x |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis + Redisson | 7.x / 3.x |
| **消息队列** | RocketMQ | 5.2.0 |
| **注册/配置** | Nacos | 2.x |
| **分布式事务** | Seata (AT 模式) | 1.7 |
| **工作流** | Camunda BPM | 7.x |
| **网关** | Spring Cloud Gateway | - |
| **文件存储** | MinIO | - |
| **可观测性** | OpenTelemetry + Prometheus + Loki + Tempo | - |
| **CI/CD** | GitHub Actions + Docker | - |

---

## 📁 项目结构

```
basebackend/
│
├── 🚀 微服务 (可部署)
│   ├── basebackend-gateway/              # API 网关 (端口 8180)
│   ├── basebackend-user-api/             # 用户服务 (认证/用户/角色/资料)
│   ├── basebackend-system-api/           # 系统管理 (部门/字典/日志/权限/监控)
│   ├── basebackend-notification-service/ # 通知服务
│   ├── basebackend-observability-service/# 可观测性服务
│   └── basebackend-file-service/         # 文件服务
│
├── 📦 公共模块 (basebackend-common/)
│   ├── common-core/         # 核心模型 (Result, PageResult, BusinessException)
│   ├── common-util/         # 工具类
│   ├── common-context/      # 用户/租户上下文
│   ├── common-security/     # 公共安全工具
│   ├── common-storage/      # 文件存储 SPI (Local/MinIO/S3/OSS)
│   ├── common-lock/         # 🔒 分布式锁 (@DistributedLock)
│   ├── common-idempotent/   # 🛡️ 幂等性 (@Idempotent)
│   ├── common-datascope/    # 📊 数据权限 (@DataScope)
│   ├── common-ratelimit/    # 🚦 多算法限流 (@RateLimit)
│   ├── common-export/       # 📤 导出 (CSV + EasyExcel + 异步)
│   ├── common-event/        # 📨 可靠领域事件
│   └── common-starter/      # 自动配置 Starter
│
├── 🔧 基础设施库
│   ├── basebackend-jwt/          # JWT 认证 (双Token/黑名单/密钥轮换/多设备/审计)
│   ├── basebackend-database/     # 数据库 (动态数据源/多租户/审计/加密/故障转移)
│   ├── basebackend-cache/        # 缓存 (多级缓存/热Key/布隆过滤器/分布式数据结构)
│   ├── basebackend-security/     # 安全 (RBAC/动态权限/安全事件)
│   ├── basebackend-messaging/    # 消息队列 (RocketMQ)
│   ├── basebackend-transaction/  # 分布式事务 (Seata AT)
│   ├── basebackend-observability/# 可观测性 (OpenTelemetry)
│   ├── basebackend-logging/      # 日志
│   ├── basebackend-nacos/        # 注册/配置中心
│   ├── basebackend-feign-api/    # Feign 客户端
│   ├── basebackend-feature-toggle/# 特性开关 (Unleash/Flagsmith)
│   └── basebackend-web/          # Web 基础配置
│
├── ⏰ 调度系统
│   └── basebackend-scheduler-parent/
│       └── scheduler-camunda/    # Camunda BPM 工作流调度
│
├── 🛠️ 工具
│   ├── basebackend-code-generator/ # 代码生成器
│   ├── basebackend-backup/         # 数据备份
│   └── basebackend-admin-web/      # React 前端管理界面
│
├── 🐳 docker/                 # Docker Compose 部署
├── ⚙️ config/                 # Nacos 配置文件
├── 📖 wiki/                   # 项目 Wiki 文档
└── 📚 docs/                   # 其他文档
```

---

## 💡 使用示例

### 分布式锁

```java
@DistributedLock(key = "#order.id", waitTime = 5, leaseTime = 30)
public void processOrder(Order order) {
    // 自动加锁/解锁，支持 SpEL 表达式
}
```

### 幂等性 — 防重复提交

```java
@Idempotent(strategy = IdempotentStrategy.PARAM, timeout = 5)
@PostMapping("/submit")
public Result<?> submitOrder(@RequestBody OrderDTO dto) {
    // 相同参数 5 秒内重复请求自动拦截
}
```

### 数据权限

```java
@DataScope(type = DataScopeType.DEPT_AND_BELOW)
public List<User> listUsers(UserQuery query) {
    // SQL 自动追加部门过滤条件
}
```

### 多算法限流

```java
@RateLimit(key = "#userId", limit = 100, window = 60, algorithm = RateLimitAlgorithm.TOKEN_BUCKET)
public Result<?> getData(@PathVariable Long userId) {
    // 每用户每分钟最多 100 次请求
}
```

### 双 Token 认证

```java
@Autowired
private JwtUtil jwtUtil;

// 登录 → 签发双 Token
String accessToken = jwtUtil.generateAccessToken(userId, claims);   // 30 分钟
String refreshToken = jwtUtil.generateRefreshToken(userId);         // 7 天

// 刷新 → 用 Refresh Token 换新的 Access Token
String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);

// 退出 → 吊销 Token
jwtUtil.revokeToken(accessToken);
```

### 数据导出

```java
@Autowired
private ExportManager exportManager;

// 同步导出 Excel
ExportResult result = exportManager.export(userList, UserDTO.class, ExportFormat.EXCEL);

// 异步大数据量导出
String taskId = asyncExportService.exportAsync(() -> fetchBigData(), UserDTO.class, ExportFormat.EXCEL);
ExportTaskStatus status = asyncExportService.getExportStatus(taskId);
```

### 可靠事件发布

```java
@Autowired
private ReliableDomainEventPublisher publisher;

// 事务提交后自动发布，失败指数退避重试
publisher.publish(new OrderCreatedEvent(orderId, userId));
```

### 敏感数据加密

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;

    @Sensitive(SensitiveType.PHONE)  // 自动 AES 加密存储，读取时解密
    private String phone;

    @Sensitive(SensitiveType.ID_CARD)
    private String idCard;
}
```

---

## 🌐 API 网关过滤器链

请求经过 Gateway 的完整过滤器链：

```
请求 → AuthenticationFilter       # JWT 认证
     → HeaderSanitizationFilter   # 请求头清洗
     → SecurityHeadersFilter      # 安全响应头
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

## 🐳 Docker 部署

```bash
# 一键启动全部基础设施 + 微服务
docker/compose/start-all.sh

# 或分层启动
docker-compose -f docker/compose/base/docker-compose.base.yml up -d           # MySQL + Redis
docker-compose -f docker/compose/middleware/docker-compose.middleware.yml up -d # Nacos + RocketMQ + Seata
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
| RocketMQ Console | 8080 |
| Prometheus | 9090 |
| Grafana | 3000 |

---

## 📊 可观测性

- **指标**: Prometheus 采集 → Grafana 可视化
- **日志**: Loki 聚合 → Grafana 查询
- **链路追踪**: OpenTelemetry → Tempo → Grafana
- **健康检查**: `/actuator/health`
- **指标端点**: `/actuator/prometheus`

---

## 📖 文档

| 文档 | 链接 |
|------|------|
| 📖 **完整 Wiki** | [GitHub Wiki](https://github.com/wuan1250276415/basebackend/wiki) |
| 🚀 快速启动 | [快速启动指南](https://github.com/wuan1250276415/basebackend/wiki/快速启动指南) |
| 🏗️ 架构设计 | [整体架构](https://github.com/wuan1250276415/basebackend/wiki/整体架构) |
| 🔐 JWT 认证 | [JWT 认证体系](https://github.com/wuan1250276415/basebackend/wiki/JWT认证体系) |
| 📦 Common 模块 | [Common 公共模块详解](https://github.com/wuan1250276415/basebackend/wiki/Common公共模块详解) |
| 📝 注解速查 | [常用注解速查](https://github.com/wuan1250276415/basebackend/wiki/常用注解速查) |
| ⚙️ 配置参考 | [配置参考](https://github.com/wuan1250276415/basebackend/wiki/配置参考) |
| 🆕 新增微服务 | [新增微服务指南](https://github.com/wuan1250276415/basebackend/wiki/新增微服务指南) |

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
