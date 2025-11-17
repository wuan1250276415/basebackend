# Nacos 配置文件说明

> 本目录包含了项目的 Nacos 配置中心配置文件

## 📁 目录结构

```
nacos-configs/
├── dev/                        # 开发环境配置
│   ├── common-config.yml       # 通用配置（日志、文件上传等）
│   ├── mysql-config.yml        # MySQL/Druid/MyBatis Plus 配置
│   ├── redis-config.yml        # Redis 配置
│   ├── rocketmq-config.yml     # RocketMQ 消息队列配置
│   ├── observability-config.yml # 可观测性配置（Actuator、Metrics、Prometheus）
│   ├── security-config.yml     # 安全配置（JWT、密码策略等）
│   └── seata-config.yml        # Seata 分布式事务配置
├── import-nacos-configs.sh     # Linux/Mac 导入脚本
└── import-nacos-configs.ps1    # Windows PowerShell 导入脚本
```

## 🚀 快速开始

### 1. 启动 Nacos 服务器

确保 Nacos 服务已启动并可访问：

```bash
# 单机模式启动（Linux/Mac）
sh nacos/bin/startup.sh -m standalone

# Windows
nacos\bin\startup.cmd -m standalone
```

访问 Nacos 控制台：http://localhost:8848/nacos （默认用户名/密码：nacos/nacos）

### 2. 导入配置到 Nacos

#### Windows 用户（PowerShell）

```powershell
# 在项目根目录执行
cd nacos-configs
.\import-nacos-configs.ps1

# 自定义参数
.\import-nacos-configs.ps1 -NacosServer "192.168.66.126:8848" -Namespace "public"
```

#### Linux/Mac 用户（Bash）

```bash
# 在项目根目录执行
cd nacos-configs
bash import-nacos-configs.sh

# 自定义参数
NACOS_SERVER=192.168.66.126:8848 NAMESPACE=public bash import-nacos-configs.sh
```

### 3. 验证配置导入

1. 访问 Nacos 控制台：http://localhost:8848/nacos
2. 进入 **配置管理** → **配置列表**
3. 选择命名空间 `public` (或你指定的命名空间)
4. 确认以下 7 个配置文件已成功导入：
   - ✅ common-config.yml
   - ✅ mysql-config.yml
   - ✅ redis-config.yml
   - ✅ rocketmq-config.yml
   - ✅ observability-config.yml
   - ✅ security-config.yml
   - ✅ seata-config.yml

### 4. 重启应用程序

重启各微服务模块，应用将自动从 Nacos 加载配置：

```bash
# 示例：启动 admin-api
cd basebackend-admin-api
mvn spring-boot:run
```

查看启动日志，确认成功加载 Nacos 配置：

```
Located property source: CompositePropertySource {name='NACOS',
  propertySources=[NacosPropertySource {name='common-config.yml'},
                   NacosPropertySource {name='mysql-config.yml'}, ...]}
```

## 📋 配置文件详解

### 1. common-config.yml

**用途**：所有微服务共享的通用配置

**包含内容：**
- 日志配置（级别、格式）
- 文件上传配置（大小限制）

### 2. mysql-config.yml

**用途**：数据库相关配置

**包含内容：**
- Druid 数据源配置（连接池、监控）
- MyBatis Plus 配置（ID生成策略、逻辑删除）
- Flyway 数据库迁移配置

**环境变量：**
- `MYSQL_HOST`: MySQL 主机地址（默认：localhost）
- `MYSQL_PORT`: MySQL 端口（默认：3306）
- `MYSQL_DATABASE`: 数据库名（默认：basebackend）
- `MYSQL_USERNAME`: 用户名（默认：root）
- `MYSQL_PASSWORD`: 密码（默认：root123456）

### 3. redis-config.yml

**用途**：Redis 缓存配置

**包含内容：**
- Redis 连接配置
- Lettuce 连接池配置
- 缓存配置（过期时间、前缀）

**环境变量：**
- `REDIS_HOST`: Redis 主机（默认：localhost）
- `REDIS_PORT`: Redis 端口（默认：6379）
- `REDIS_PASSWORD`: Redis 密码
- `REDIS_DATABASE`: 数据库索引（默认：0）

### 4. rocketmq-config.yml

**用途**：RocketMQ 消息队列配置

**包含内容：**
- RocketMQ 生产者/消费者配置
- 消息重试、死信队列配置
- 事务消息配置
- 幂等性配置

**环境变量：**
- `ROCKETMQ_NAME_SERVER`: RocketMQ 服务器地址（默认：localhost:9876）

### 5. observability-config.yml

**用途**：可观测性配置

**包含内容：**
- Actuator 健康检查端点
- Micrometer Metrics 指标配置
- Prometheus 导出配置
- 链路追踪配置

### 6. security-config.yml

**用途**：安全配置

**包含内容：**
- JWT 配置（密钥、过期时间）
- 管理后台密码策略
- 登录安全配置（失败次数、锁定时间）
- Web 安全基线配置

**环境变量：**
- `JWT_SECRET`: JWT 密钥（建议生产环境使用强密钥）

### 7. seata-config.yml

**用途**：Seata 分布式事务配置

**包含内容：**
- Seata 客户端配置（RM、TM）
- Undo Log 配置
- Seata Server 注册中心配置

**环境变量：**
- `SEATA_ENABLED`: 是否启用 Seata（默认：false）
- `SEATA_SERVER_ADDR`: Seata Server 地址（默认：localhost:8091）

## 🔧 环境变量配置

建议使用环境变量管理敏感配置。创建 `.env` 文件（不要提交到 Git）：

```bash
# .env 文件示例

# Nacos 配置
NACOS_SERVER_ADDR=192.168.66.126:8848
NACOS_NAMESPACE=public
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

# MySQL 配置
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=basebackend
MYSQL_USERNAME=root
MYSQL_PASSWORD=your-db-password

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password
REDIS_DATABASE=0

# JWT 配置
JWT_SECRET=your-super-secret-jwt-key-change-in-production

# RocketMQ 配置
ROCKETMQ_NAME_SERVER=localhost:9876

# Seata 配置
SEATA_ENABLED=false
SEATA_SERVER_ADDR=localhost:8091
```

## 📝 配置迁移说明

### 已迁移的配置

以下配置已从各模块的 `application.yml` / `application-*.yml` 迁移到 Nacos：

- ✅ 日志配置 → common-config.yml
- ✅ 文件上传配置 → common-config.yml
- ✅ 数据库配置 → mysql-config.yml
- ✅ Redis 配置 → redis-config.yml
- ✅ RocketMQ 配置 → rocketmq-config.yml
- ✅ JWT 配置 → security-config.yml
- ✅ 管理后台配置 → security-config.yml
- ✅ Actuator 配置 → observability-config.yml
- ✅ Seata 配置 → seata-config.yml

### 保留在本地的配置

以下配置仍保留在各模块的 `application.yml`：

- ✅ 服务端口（`server.port`） - 每个模块不同
- ✅ 应用名称（`spring.application.name`） - 每个模块不同
- ✅ 邮件配置（`spring.mail`） - 模块特有（如 admin-api）
- ✅ Thymeleaf 配置 - 模块特有
- ✅ Knife4j 配置 - 模块特有
- ✅ MinIO 配置 - 模块特有（如 admin-api）

### 配置清理（可选）

**⚠️ 重要：在确认服务正常运行后再执行清理**

导入 Nacos 配置并重启服务，确认一切正常后，可以从本地 `application.yml` / `application-dev.yml` 中删除已迁移的配置，避免配置冗余和混淆。

## 🔄 动态刷新配置

所有 Nacos 配置文件都已启用动态刷新（`refresh: true`）。

如需在 Java 代码中支持动态刷新，在配置类上添加 `@RefreshScope` 注解：

```java
@Component
@RefreshScope  // 支持 Nacos 配置动态刷新
@ConfigurationProperties(prefix = "security.baseline")
public class SecurityBaselineProperties {
    private List<String> allowedOrigins = new ArrayList<>();
    private boolean enforceReferer = true;
    // getters and setters...
}
```

修改 Nacos 配置后，配置类会自动刷新，无需重启应用。

## 🆘 常见问题

### Q1: 服务启动时找不到 Nacos 配置怎么办？

**A:** 检查以下几点：
1. Nacos 服务是否正常运行
2. `bootstrap.yml` 中的 `server-addr` 是否正确
3. 命名空间 ID 是否正确（默认：public）
4. Data ID 和 Group 是否匹配
5. 查看服务启动日志，确认 Nacos 配置加载情况

### Q2: 配置修改后服务没有刷新怎么办？

**A:** 确认：
1. `bootstrap.yml` 中 `refresh: true` 是否设置
2. 配置类是否添加了 `@RefreshScope` 注解
3. 检查 Nacos 日志，确认配置推送成功
4. 调用 `/actuator/refresh` 端点手动触发刷新

### Q3: 如何创建其他环境（test/prod）的配置？

**A:**
1. 复制 `dev` 目录为 `test` 或 `prod`
2. 修改配置文件中的环境相关值（如数据库地址、密码等）
3. 在 Nacos 中创建对应的命名空间（如 `test`、`prod`）
4. 执行导入脚本时指定命名空间：`bash import-nacos-configs.sh NAMESPACE=test`

### Q4: 导入脚本执行失败怎么办？

**A:** 常见原因：
1. **权限问题**：Linux/Mac 需要执行权限（`chmod +x import-nacos-configs.sh`）
2. **网络问题**：确认能访问 Nacos 服务器
3. **依赖缺失**：Bash 脚本需要 `curl` 和 `python3` 或 `perl`
4. **配置文件编码**：确保配置文件是 UTF-8 编码

## 📚 参考资料

- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Spring Cloud Alibaba Nacos Config](https://github.com/alibaba/spring-cloud-alibaba/wiki/Nacos-config)
- [项目配置迁移指南](../docs/NACOS_MIGRATION_GUIDE.md)

---

*最后更新: 2025-01-13*
*维护者: BaseBackend Team*
