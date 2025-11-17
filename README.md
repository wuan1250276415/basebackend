# Base Backend - 微服务基础后台架构

一个基于 Java 和 Spring Boot 的企业级微服务基础架构项目，采用 Maven 多模块管理，提供了完整的基础组件和服务模块。

## 项目概述

本项目是一个开箱即用的微服务架构后台系统，集成了常用的企业级功能模块，包括数据库操作、缓存、安全认证、日志、消息队列、文件服务和 API 网关等。

## 技术栈

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0**
- **MyBatis Plus 3.5.5** - ORM框架
- **MySQL 8.0** - 关系型数据库
- **Redis** - 缓存和分布式锁
- **Redisson 3.25.2** - Redis客户端
- **RocketMQ** - 消息队列
- **JWT** - 身份认证
- **Spring Cloud Gateway** - API网关
- **Micrometer + Prometheus** - 监控指标
- **Zipkin** - 链路追踪
- **Logback** - 日志框架

## 📁 项目结构

```
basebackend/
├── 📚 docs/                         # 所有项目文档
│   ├── getting-started/             # 快速入门指南
│   ├── guides/                      # 详细使用指南
│   ├── architecture/                # 架构设计文档
│   ├── troubleshooting/             # 故障排查文档
│   ├── implementation/              # 功能实现总结
│   ├── changelog/                   # 变更记录
│   └── README.md                    # 文档索引
│
├── 🔧 bin/                          # 所有脚本文件
│   ├── start/                       # 启动脚本
│   ├── test/                        # 测试脚本
│   ├── maintenance/                 # 运维脚本
│   └── sql/                         # SQL脚本
│
├── 🐳 docker/                       # Docker相关
│   ├── compose/                     # Docker Compose文件
│   ├── messaging/                   # 消息队列配置
│   ├── nacos/                       # Nacos配置
│   ├── observability/               # 可观测性配置
│   └── seata-server/                # Seata配置
│
├── ⚙️ config/                       # 配置文件
│   ├── nacos-configs/               # Nacos配置中心
│   └── env/                         # 环境配置模板
│
├── 🔄 .github/                      # GitHub Actions CI/CD
├── ☸️ k8s/                          # Kubernetes配置
├── 🚀 deployment/                   # 部署相关
├── 📦 rocketmq/                     # RocketMQ配置
├── 🛡️ sentinel-rules/               # Sentinel规则
│
└── 📦 业务模块/
    ├── basebackend-common/          # 公共模块
    ├── basebackend-database/        # 数据库模块
    ├── basebackend-cache/           # 缓存模块
    ├── basebackend-logging/         # 日志模块
    ├── basebackend-security/        # 安全模块
    ├── basebackend-observability/   # 可观测模块
    ├── basebackend-messaging/       # 消息服务模块
    ├── basebackend-file-service/    # 文件服务模块
    ├── basebackend-gateway/         # 网关模块
    ├── basebackend-admin-api/       # 管理后台API
    └── ...                          # 其他业务模块
```

### 模块详细说明

#### 核心基础模块

**basebackend-common** - 公共模块
- 通用数据模型（Result, PageResult）
- 全局异常处理
- 常量定义和枚举类

**basebackend-database** - 数据库模块
- MyBatis Plus配置
- 基础实体类
- 字段自动填充处理器

**basebackend-cache** - 缓存模块
- Redis配置
- 缓存服务
- Redisson分布式锁工具

**basebackend-logging** - 日志模块
- Web日志切面
- Logback配置

**basebackend-security** - 安全模块
- Spring Security配置
- JWT认证过滤器
- JWT工具类

**basebackend-observability** - 可观测模块
- 监控指标配置
- Actuator端点
- 健康检查、指标暴露

**basebackend-messaging** - 消息服务模块
- 消息生产者
- 消息消费者

**basebackend-file-service** - 文件服务模块
- 文件配置
- 文件上传下载服务
- 文件API接口

**basebackend-gateway** - 网关模块
- 网关过滤器（认证、日志）
- 路由配置

## 核心功能

### 1. 公共模块 (basebackend-common)
- 统一响应结果封装 (Result)
- 分页结果封装 (PageResult)
- 全局异常处理
- 业务异常定义
- 通用常量和枚举

### 2. 数据库模块 (basebackend-database)
- MyBatis Plus 集成和配置
- 基础实体类 (BaseEntity) 包含通用字段
- 自动填充创建时间、更新时间、创建人、更新人
- 逻辑删除支持
- 分页插件
- 乐观锁插件
- 防止全表更新与删除

### 3. 缓存模块 (basebackend-cache)
- Redis 集成 (支持各种数据类型操作)
- Redisson 分布式锁
- 读写锁、信号量、倒计时锁等高级功能

### 4. 日志模块 (basebackend-logging)
- Web请求日志切面 (自动记录请求和响应)
- 按级别分文件存储 (INFO/WARN/ERROR)
- 按天滚动日志
- 异步日志输出
- 支持多环境配置

### 5. 安全模块 (basebackend-security)
- JWT Token 生成和验证
- Spring Security 集成
- JWT 认证过滤器
- 密码加密 (BCrypt)
- 路径白名单配置

### 6. 可观测模块 (basebackend-observability)
- Spring Boot Actuator 集成
- Prometheus 指标暴露
- Micrometer 监控
- Zipkin 链路追踪
- 健康检查端点

### 7. 消息服务模块 (basebackend-message-service)
- RocketMQ 集成
- 支持同步/异步/单向消息
- 延迟消息
- 消息标签
- 消费者示例

### 8. 文件服务模块 (basebackend-file-service)
- 文件上传、下载、删除
- 文件类型验证
- 文件大小限制
- 按日期分目录存储
- 唯一文件名生成

### 9. 网关模块 (basebackend-gateway)
- Spring Cloud Gateway 路由
- 全局认证过滤器
- 请求日志记录
- 限流配置
- CORS 跨域支持
- 负载均衡

## 📖 文档导航

完整的项目文档请访问 [docs/](docs/) 目录：

- **快速入门**: [docs/getting-started/](docs/getting-started/) - 快速上手指南
- **详细指南**: [docs/guides/](docs/guides/) - 各功能模块的详细使用说明
- **架构设计**: [docs/architecture/](docs/architecture/) - 系统架构和设计文档
- **故障排查**: [docs/troubleshooting/](docs/troubleshooting/) - 常见问题解决方案
- **实现总结**: [docs/implementation/](docs/implementation/) - 功能实现记录
- **变更记录**: [docs/changelog/](docs/changelog/) - 功能更新历史

推荐从 [docs/README.md](docs/README.md) 开始浏览完整的文档索引。

## 快速开始

### 前置要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- RocketMQ 4.9+ (可选)

### 安装步骤

1. 克隆项目
```bash
git clone <repository-url>
cd basebackend
```

2. 编译项目
```bash
mvn clean install
```

3. 配置数据库
- 创建数据库
- 在各服务的 `application.yml` 中配置数据库连接

4. 配置Redis
- 在 `application-cache.yml` 中配置Redis连接

5. 启动网关
```bash
cd basebackend-gateway
mvn spring-boot:run
```

## 配置说明

### 数据库配置
在 `basebackend-database/src/main/resources/application-database.yml` 中配置：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: your_password
```

### Redis配置
在 `basebackend-cache/src/main/resources/application-cache.yml` 中配置：
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### JWT配置
在 `basebackend-security/src/main/resources/application-security.yml` 中配置：
```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时
```

## 使用示例

### 1. 创建实体类
```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String email;
}
```

### 2. 使用缓存服务
```java
@Autowired
private RedisService redisService;

// 设置缓存
redisService.set("key", "value", 60, TimeUnit.SECONDS);

// 获取缓存
Object value = redisService.get("key");
```

### 3. 使用分布式锁
```java
@Autowired
private RedissonLockUtil lockUtil;

String lockKey = "resource:lock";
if (lockUtil.tryLock(lockKey, 10, 30, TimeUnit.SECONDS)) {
    try {
        // 执行业务逻辑
    } finally {
        lockUtil.unlock(lockKey);
    }
}
```

### 4. 发送消息
```java
@Autowired
private MessageProducer messageProducer;

messageProducer.sendSyncMessage("topic", messageObject);
```

### 5. 上传文件
```java
@Autowired
private FileService fileService;

String filePath = fileService.uploadFile(multipartFile);
```

## API端点

### 网关端口
- 默认端口: `8080`

### Actuator监控端点
- 健康检查: `/actuator/health`
- 监控指标: `/actuator/prometheus`
- 所有端点: `/actuator`

## 开发指南

### 添加新的微服务模块

1. 在父POM中添加模块声明
2. 创建模块目录和pom.xml
3. 在网关配置中添加路由规则
4. 在需要的模块中引入依赖

### 扩展数据库实体

继承 `BaseEntity` 类即可自动获得：
- id（主键）
- createTime（创建时间）
- updateTime（更新时间）
- createBy（创建人）
- updateBy（更新人）
- deleted（逻辑删除标记）

## 监控和运维

### 日志目录
- 默认日志路径: `./logs`
- 日志文件: `info.log`, `warn.log`, `error.log`

### 监控指标
- 通过 Prometheus 采集 `/actuator/prometheus` 端点
- 使用 Grafana 可视化监控指标

### 链路追踪
- Zipkin Server: `http://localhost:9411`
- 自动追踪HTTP请求链路

## 注意事项

1. **JWT密钥**: 生产环境必须修改默认密钥，建议从环境变量或配置中心获取
2. **数据库连接**: 请根据实际环境配置数据库连接信息
3. **文件上传路径**: 确保应用有权限访问配置的上传目录
4. **消息队列**: 如不使用RocketMQ，可以移除相关依赖

## 最佳实践

1. 使用 `Result` 统一包装响应结果
2. 继承 `BaseEntity` 减少重复字段定义
3. 使用全局异常处理器处理业务异常
4. 敏感配置使用环境变量或配置中心
5. 定期清理过期日志文件

## 许可证

MIT License

## 联系方式

如有问题或建议，请提交 Issue 或 Pull Request。
