# Base Backend 架构重构执行计划

> **创建时间**: 2025-11-17
> **重构目标**: 解决模块依赖混乱、优化架构层次、统一中间件版本、完善部署配置
> **预计耗时**: 2-3周（分阶段执行）
> **风险等级**: 中（涉及依赖结构调整）

---

## 一、当前架构问题诊断

### 1.1 关键架构问题（必须解决）

#### ❌ **P0: 严重的循环依赖**
```
basebackend-security → basebackend-web → spring-boot-starter-security
```
- **影响**: 无法独立部署security模块，可能导致Spring上下文加载失败
- **位置**: `basebackend-security/pom.xml:28-33`

#### ❌ **P0: 版本管理混乱**
- RocketMQ同时使用 2.3.0 和 5.2.0 两个版本
- 30+ 依赖硬编码版本分散在各模块中
- **影响**: 版本冲突、升级困难、潜在的运行时错误

#### ❌ **P0: basebackend-admin-api 职责过重**
- 依赖16个其他模块（几乎所有基础设施）
- **影响**: 启动缓慢、内存占用大、无法独立扩展

### 1.2 重要架构问题（建议解决）

#### ⚠️ **P1: 模块职责不清**
- `basebackend-web` 包含安全功能
- `basebackend-common` 曾包含Web过滤器
- `basebackend-backup` 依赖 `scheduler`（应该反向）

#### ⚠️ **P1: 未声明的模块**
- 14个模块目录存在但未在 pom.xml 中声明
- 可能是未完成的拆分或废弃代码

### 1.3 次要问题（长期优化）

#### ℹ️ **P2: 配置管理分散**
- Docker Compose文件分散在多个目录
- 环境配置未分层（dev/test/prod）

---

## 二、架构重构方案

### 2.1 目标架构设计

```
┌────────────────────────────────────────────────────────┐
│                  微服务架构分层                           │
└────────────────────────────────────────────────────────┘

Layer 0: 基础工具层 (Foundation)
┌──────────────────────────────────────────────────────┐
│  basebackend-common                                  │
│  纯工具类、常量、基础异常、模型基类                       │
│  依赖: 无                                             │
└──────────────────────────────────────────────────────┘

Layer 1: 核心框架层 (Core Framework)
┌──────────────┬──────────────┬──────────────┐
│     jwt      │   database   │    cache     │
├──────────────┼──────────────┼──────────────┤
│   logging    │ transaction  │  messaging   │
├──────────────┴──────────────┴──────────────┤
│           observability                     │
└──────────────────────────────────────────────┘
依赖: Layer 0

Layer 2: 基础设施层 (Infrastructure)
┌──────────────┬──────────────┬──────────────┐
│     web      │   security   │   nacos      │
├──────────────┼──────────────┼──────────────┤
│  feign-api   │ file-service │   backup     │
└──────────────┴──────────────┴──────────────┘
依赖: Layer 0, Layer 1

Layer 3: 系统服务层 (System Services)
┌──────────────┬──────────────┬──────────────┐
│   gateway    │  scheduler   │code-generator│
└──────────────┴──────────────┴──────────────┘
依赖: Layer 0, Layer 1, Layer 2

Layer 4: 业务服务层 (Business Services)
┌──────────────┬──────────────┬──────────────┐
│  user-api    │  system-api  │  file-api    │
├──────────────┼──────────────┼──────────────┤
│  auth-api    │  notify-api  │  report-api  │
└──────────────┴──────────────┴──────────────┘
依赖: Layer 0, Layer 1, Layer 2
```

### 2.2 模块依赖规则

```yaml
依赖规则:
  - 只能依赖同层或更低层的模块
  - 同层模块间尽量避免相互依赖
  - 业务模块不应该被基础模块依赖
  - 每个模块最多依赖5个其他模块（common除外）
```

---

## 三、执行计划（分阶段）

### 阶段一：基础准备和版本统一（第1-2天）

#### Step 1.1: 创建项目备份
```bash
# 创建备份分支
git checkout -b backup/before-refactoring
git add -A
git commit -m "backup: 架构重构前的完整备份"
git push origin backup/before-refactoring

# 创建重构分支
git checkout -b refactor/architecture-optimization
```

#### Step 1.2: 统一版本管理

**修改文件**: `pom.xml`

在 `<properties>` 部分添加所有版本定义：

```xml
<!-- 添加到第38-94行的properties中 -->
<knife4j.version>4.3.0</knife4j.version>
<springdoc-openapi.version>2.2.0</springdoc-openapi.version>
<logstash-logback.version>7.4</logstash-logback.version>
<loki-logback.version>1.5.1</loki-logback.version>
<minio.version>8.5.7</minio.version>
<thumbnailator.version>0.4.20</thumbnailator.version>
<context-propagation.version>1.0.5</context-propagation.version>
<okhttp3.version>4.12.0</okhttp3.version>
<google-java-format.version>1.19.2</google-java-format.version>
<commons-compress.version>1.25.0</commons-compress.version>
<postgresql.version>42.7.1</postgresql.version>
<ojdbc8.version>21.11.0.0</ojdbc8.version>
<freemarker.version>2.3.32</freemarker.version>
<velocity.version>2.3</velocity.version>
<thymeleaf.version>3.1.2.RELEASE</thymeleaf.version>
<openapi-generator.version>7.5.0</openapi-generator.version>
<shardingsphere.version>5.4.1</shardingsphere.version>
<micrometer-jvm-extras.version>0.2.2</micrometer-jvm-extras.version>
<oshi-core.version>6.4.6</oshi-core.version>
<camunda-spin.version>1.23.0</camunda-spin.version>
```

在 `<dependencyManagement>` 部分添加依赖声明（第95-400行后）：

```xml
<!-- 统一管理所有第三方依赖版本 -->
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
    <version>${knife4j.version}</version>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>${springdoc-openapi.version}</version>
</dependency>
<!-- ... 其他依赖 ... -->
```

#### Step 1.3: 修复RocketMQ版本冲突

统一使用 RocketMQ 5.2.0：

```xml
<!-- pom.xml -->
<rocketmq.version>5.2.0</rocketmq.version>
<rocketmq-spring.version>2.3.0</rocketmq-spring.version>

<dependencyManagement>
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
        <version>${rocketmq-spring.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-client</artifactId>
        <version>${rocketmq.version}</version>
    </dependency>
</dependencyManagement>
```

---

### 阶段二：解决循环依赖（第3-4天）

#### Step 2.1: 重构 basebackend-web

**文件**: `basebackend-web/pom.xml`

移除安全相关依赖：
```xml
<!-- 删除以下依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

**文件**: 移动以下类到 basebackend-security：
- `WebSecurityConfig.java` → `basebackend-security/src/main/java/com/basebackend/security/config/`
- `CsrfCookieFilter.java` → `basebackend-security/src/main/java/com/basebackend/security/filter/`
- `OriginValidationFilter.java` → `basebackend-security/src/main/java/com/basebackend/security/filter/`

#### Step 2.2: 重构 basebackend-security

**文件**: `basebackend-security/pom.xml`

移除对 basebackend-web 的依赖：
```xml
<!-- 删除 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-web</artifactId>
</dependency>

<!-- 添加必要的Spring Web依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <scope>provided</scope> <!-- 由使用方提供 -->
</dependency>
```

#### Step 2.3: 重构 basebackend-backup

**文件**: `basebackend-backup/pom.xml`

移除对 scheduler 的依赖：
```xml
<!-- 删除第35-40行 -->
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-scheduler</artifactId>
    <version>${project.version}</version>
</dependency>
```

创建内部调度器：
```java
// basebackend-backup/src/main/java/com/basebackend/backup/scheduler/BackupScheduler.java
@Component
@ConditionalOnProperty(name = "backup.scheduler.enabled", havingValue = "true")
public class BackupScheduler {
    @Scheduled(cron = "${backup.scheduler.cron:0 0 2 * * ?}")
    public void executeBackup() {
        // 备份逻辑
    }
}
```

---

### 阶段三：拆分 admin-api（第5-7天）

#### Step 3.1: 创建新的服务模块

```bash
# 创建用户服务
mkdir -p basebackend-user-api/src/main/java/com/basebackend/user
mkdir -p basebackend-user-api/src/main/resources

# 创建系统服务
mkdir -p basebackend-system-api/src/main/java/com/basebackend/system
mkdir -p basebackend-system-api/src/main/resources

# 创建认证服务
mkdir -p basebackend-auth-api/src/main/java/com/basebackend/auth
mkdir -p basebackend-auth-api/src/main/resources
```

#### Step 3.2: 创建各服务的 pom.xml

**basebackend-user-api/pom.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <parent>
        <groupId>com.basebackend</groupId>
        <artifactId>basebackend</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>basebackend-user-api</artifactId>
    <name>BaseBackend User API</name>

    <dependencies>
        <!-- 基础依赖 -->
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-common</artifactId>
        </dependency>
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-database</artifactId>
        </dependency>
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.basebackend</groupId>
            <artifactId>basebackend-observability</artifactId>
        </dependency>

        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### Step 3.3: 迁移代码

从 `basebackend-admin-api` 迁移相关代码：

**用户服务** (basebackend-user-api):
- controller: UserController, ProfileController, RoleController
- service: UserService, ProfileService, RoleService
- mapper: UserMapper, ProfileMapper, RoleMapper
- entity: User, Profile, Role

**系统服务** (basebackend-system-api):
- controller: DictController, MenuController, DeptController
- service: DictService, MenuService, DeptService
- mapper: DictMapper, MenuMapper, DeptMapper
- entity: Dict, Menu, Dept

**认证服务** (basebackend-auth-api):
- controller: AuthController, LoginController
- service: AuthService, LoginService, TokenService
- 配置: SecurityConfig, JwtConfig

---

### 阶段四：整理Docker和中间件配置（第8-9天）

#### Step 4.1: 创建统一的Docker Compose结构

```bash
# 创建Docker配置目录结构
mkdir -p docker/compose/base        # 基础设施
mkdir -p docker/compose/middleware  # 中间件
mkdir -p docker/compose/services    # 业务服务
mkdir -p docker/compose/env          # 环境配置
```

#### Step 4.2: 基础设施 Docker Compose

**文件**: `docker/compose/base/docker-compose.base.yml`

```yaml
version: '3.8'

networks:
  basebackend-network:
    driver: bridge
    name: basebackend-network

volumes:
  mysql-data:
  redis-data:
  nacos-data:
  rocketmq-data:

services:
  mysql:
    image: mysql:8.0.33
    container_name: basebackend-mysql
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123456}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-basebackend}
      MYSQL_USER: ${MYSQL_USER:-basebackend}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-basebackend123}
      TZ: Asia/Shanghai
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d
    networks:
      - basebackend-network
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --max_connections=1000
      - --max_allowed_packet=64M
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7.2-alpine
    container_name: basebackend-redis
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis-data:/data
      - ./redis.conf:/usr/local/etc/redis/redis.conf
    networks:
      - basebackend-network
    command: redis-server /usr/local/etc/redis/redis.conf
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

#### Step 4.3: 中间件 Docker Compose

**文件**: `docker/compose/middleware/docker-compose.middleware.yml`

```yaml
version: '3.8'

services:
  nacos:
    image: nacos/nacos-server:v2.2.3
    container_name: basebackend-nacos
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_USER: ${MYSQL_USER:-basebackend}
      MYSQL_SERVICE_PASSWORD: ${MYSQL_PASSWORD:-basebackend123}
      NACOS_AUTH_ENABLE: ${NACOS_AUTH_ENABLE:-true}
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN:-SecretKey012345678901234567890123456789012345678901234567890123456789}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_IDENTITY_KEY:-nacos}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_IDENTITY_VALUE:-nacos}
    ports:
      - "${NACOS_PORT:-8848}:8848"
      - "${NACOS_GRPC_PORT:-9848}:9848"
    volumes:
      - nacos-data:/home/nacos/data
    networks:
      - basebackend-network
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/v1/console/health/readiness"]
      interval: 10s
      timeout: 5s
      retries: 10

  rocketmq-namesrv:
    image: apache/rocketmq:5.2.0
    container_name: basebackend-rocketmq-namesrv
    ports:
      - "${ROCKETMQ_NAMESRV_PORT:-9876}:9876"
    volumes:
      - rocketmq-data:/home/rocketmq/store
    networks:
      - basebackend-network
    command: sh mqnamesrv
    healthcheck:
      test: ["CMD", "sh", "-c", "netstat -an | grep 9876"]
      interval: 10s
      timeout: 5s
      retries: 5

  rocketmq-broker:
    image: apache/rocketmq:5.2.0
    container_name: basebackend-rocketmq-broker
    ports:
      - "${ROCKETMQ_BROKER_PORT:-10911}:10911"
      - "${ROCKETMQ_VIP_PORT:-10909}:10909"
    volumes:
      - rocketmq-data:/home/rocketmq/store
      - ./broker.conf:/home/rocketmq/broker.conf
    networks:
      - basebackend-network
    environment:
      NAMESRV_ADDR: rocketmq-namesrv:9876
      MAX_HEAP_SIZE: 512M
      HEAP_NEWSIZE: 256M
    command: sh mqbroker -c /home/rocketmq/broker.conf
    depends_on:
      rocketmq-namesrv:
        condition: service_healthy

  rocketmq-console:
    image: apacherocketmq/rocketmq-console:2.0.0
    container_name: basebackend-rocketmq-console
    ports:
      - "${ROCKETMQ_CONSOLE_PORT:-8180}:8080"
    environment:
      JAVA_OPTS: "-Drocketmq.config.namesrvAddr=rocketmq-namesrv:9876 -Drocketmq.config.isVIPChannel=false"
    networks:
      - basebackend-network
    depends_on:
      - rocketmq-namesrv
      - rocketmq-broker

  seata-server:
    image: seataio/seata-server:1.7.1
    container_name: basebackend-seata
    ports:
      - "${SEATA_PORT:-8091}:8091"
      - "${SEATA_CONSOLE_PORT:-7091}:7091"
    environment:
      SEATA_PORT: 8091
      STORE_MODE: db
      SEATA_CONFIG_NAME: file:/root/seata-config/registry
    volumes:
      - ./seata-config:/root/seata-config
    networks:
      - basebackend-network
    depends_on:
      mysql:
        condition: service_healthy
      nacos:
        condition: service_healthy
```

#### Step 4.4: 服务编排 Docker Compose

**文件**: `docker/compose/services/docker-compose.services.yml`

```yaml
version: '3.8'

services:
  gateway:
    image: basebackend/gateway:${VERSION:-latest}
    container_name: basebackend-gateway
    build:
      context: ../../../basebackend-gateway
      dockerfile: Dockerfile
    ports:
      - "${GATEWAY_PORT:-8080}:8080"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
    networks:
      - basebackend-network
    depends_on:
      nacos:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  user-api:
    image: basebackend/user-api:${VERSION:-latest}
    container_name: basebackend-user-api
    build:
      context: ../../../basebackend-user-api
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
    networks:
      - basebackend-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      nacos:
        condition: service_healthy
    deploy:
      replicas: ${USER_API_REPLICAS:-2}
      restart_policy:
        condition: on-failure
        delay: 5s
        max_attempts: 3

  system-api:
    image: basebackend/system-api:${VERSION:-latest}
    container_name: basebackend-system-api
    build:
      context: ../../../basebackend-system-api
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
    networks:
      - basebackend-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      nacos:
        condition: service_healthy

  auth-api:
    image: basebackend/auth-api:${VERSION:-latest}
    container_name: basebackend-auth-api
    build:
      context: ../../../basebackend-auth-api
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
    networks:
      - basebackend-network
    depends_on:
      redis:
        condition: service_healthy
      nacos:
        condition: service_healthy
```

#### Step 4.5: 环境配置文件

**文件**: `docker/compose/env/.env.dev`

```bash
# MySQL Configuration
MYSQL_ROOT_PASSWORD=root123456
MYSQL_DATABASE=basebackend
MYSQL_USER=basebackend
MYSQL_PASSWORD=basebackend123
MYSQL_PORT=3306

# Redis Configuration
REDIS_PORT=6379
REDIS_PASSWORD=

# Nacos Configuration
NACOS_PORT=8848
NACOS_GRPC_PORT=9848
NACOS_AUTH_ENABLE=true
NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789
NACOS_NAMESPACE=dev

# RocketMQ Configuration
ROCKETMQ_NAMESRV_PORT=9876
ROCKETMQ_BROKER_PORT=10911
ROCKETMQ_VIP_PORT=10909
ROCKETMQ_CONSOLE_PORT=8180

# Seata Configuration
SEATA_PORT=8091
SEATA_CONSOLE_PORT=7091

# Gateway Configuration
GATEWAY_PORT=8080

# Service Configuration
SPRING_PROFILES_ACTIVE=dev
VERSION=1.0.0

# Service Replicas
USER_API_REPLICAS=2
SYSTEM_API_REPLICAS=1
AUTH_API_REPLICAS=2
```

#### Step 4.6: 启动脚本

**文件**: `docker/compose/start-all.sh`

```bash
#!/bin/bash

# 设置环境
ENV_FILE=${1:-env/.env.dev}
echo "Using environment: $ENV_FILE"

# 检查环境文件
if [ ! -f "$ENV_FILE" ]; then
    echo "Environment file not found: $ENV_FILE"
    exit 1
fi

# 启动基础设施
echo "Starting base infrastructure..."
docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE up -d

# 等待基础设施就绪
echo "Waiting for infrastructure to be ready..."
sleep 30

# 启动中间件
echo "Starting middleware..."
docker-compose -f middleware/docker-compose.middleware.yml --env-file $ENV_FILE up -d

# 等待中间件就绪
echo "Waiting for middleware to be ready..."
sleep 60

# 启动服务
echo "Starting services..."
docker-compose -f services/docker-compose.services.yml --env-file $ENV_FILE up -d

echo "All services started successfully!"
echo "Check status with: docker-compose -f base/docker-compose.base.yml --env-file $ENV_FILE ps"
```

---

### 阶段五：创建部署文档结构（第10天）

#### Step 5.1: 创建文档目录结构

```bash
mkdir -p docs/deployment/docker
mkdir -p docs/deployment/kubernetes
mkdir -p docs/deployment/configuration
mkdir -p docs/deployment/monitoring
mkdir -p docs/architecture
mkdir -p docs/api
mkdir -p docs/development
```

#### Step 5.2: 部署主文档

**文件**: `docs/deployment/README.md`

```markdown
# Base Backend 部署指南

## 目录结构

```
docs/deployment/
├── README.md                 # 本文档
├── docker/                   # Docker部署
│   ├── quick-start.md       # Docker快速开始
│   ├── production.md        # 生产环境部署
│   └── troubleshooting.md   # 故障排查
├── kubernetes/               # K8s部署
│   ├── helm-charts.md       # Helm Chart说明
│   ├── deployment.md        # K8s部署指南
│   └── scaling.md           # 扩缩容配置
├── configuration/            # 配置管理
│   ├── nacos.md             # Nacos配置
│   ├── environment.md       # 环境变量
│   └── secrets.md           # 密钥管理
└── monitoring/               # 监控运维
    ├── prometheus.md         # Prometheus监控
    ├── grafana.md           # Grafana面板
    └── logging.md           # 日志管理
```

## 快速开始

### 1. 使用Docker Compose部署（开发环境）

```bash
cd docker/compose
./start-all.sh env/.env.dev
```

### 2. 使用Kubernetes部署（生产环境）

```bash
cd k8s
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f deployments/
kubectl apply -f services/
kubectl apply -f ingress.yaml
```

## 部署架构

[架构图]

## 环境要求

### 最小配置（开发环境）
- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB

### 推荐配置（生产环境）
- CPU: 16核
- 内存: 32GB
- 磁盘: 200GB SSD

## 服务依赖

### 基础设施
- MySQL 8.0.33+
- Redis 7.2+
- Nacos 2.2.3+

### 中间件
- RocketMQ 5.2.0+
- Seata 1.7.1+
- Sentinel 1.8.6+

### 监控组件
- Prometheus 2.45+
- Grafana 10.0+
- Loki 2.9+
```

#### Step 5.3: Docker部署文档

**文件**: `docs/deployment/docker/quick-start.md`

```markdown
# Docker 快速部署指南

## 前置条件

- Docker 24.0+
- Docker Compose 2.20+
- 至少8GB可用内存

## 部署步骤

### 1. 克隆项目

```bash
git clone https://github.com/basebackend/basebackend.git
cd basebackend
```

### 2. 配置环境变量

```bash
cd docker/compose
cp env/.env.example env/.env.dev
# 编辑 env/.env.dev 配置数据库密码等
```

### 3. 启动服务

#### 方式一：一键启动所有服务

```bash
./start-all.sh env/.env.dev
```

#### 方式二：分步启动

```bash
# 启动基础设施
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev up -d

# 启动中间件
docker-compose -f middleware/docker-compose.middleware.yml --env-file env/.env.dev up -d

# 启动业务服务
docker-compose -f services/docker-compose.services.yml --env-file env/.env.dev up -d
```

### 4. 验证部署

```bash
# 检查服务状态
docker ps

# 访问服务
curl http://localhost:8080/actuator/health

# 访问Nacos控制台
http://localhost:8848/nacos
用户名: nacos
密码: nacos

# 访问RocketMQ控制台
http://localhost:8180
```

## 常见问题

### Q1: MySQL连接失败
检查MySQL是否完全启动：
```bash
docker logs basebackend-mysql
```

### Q2: Nacos注册失败
确保Nacos已经完全启动并且网络连通：
```bash
docker exec basebackend-gateway ping nacos
```

### Q3: 内存不足
调整Docker Desktop的内存配置或减少服务副本数。
```

#### Step 5.4: Kubernetes部署文档

**文件**: `docs/deployment/kubernetes/deployment.md`

```markdown
# Kubernetes 部署指南

## 前置条件

- Kubernetes 1.27+
- Helm 3.12+
- kubectl 配置完成

## 部署架构

```
┌─────────────────────────────────────────────┐
│                 Ingress                      │
└─────────────────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
    ┌───▼────┐              ┌──────▼──────┐
    │Gateway │              │ Admin Web   │
    │Service │              │   (Nginx)   │
    └───┬────┘              └─────────────┘
        │
    ┌───▼─────────────────────────┐
    │     Service Mesh (Istio)    │
    └─────────────────────────────┘
        │         │         │
    ┌───▼──┐ ┌───▼──┐ ┌───▼──┐
    │User  │ │System│ │Auth  │
    │API   │ │API   │ │API   │
    └──────┘ └──────┘ └──────┘
```

## 部署步骤

### 1. 创建命名空间

```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: basebackend
  labels:
    istio-injection: enabled
```

```bash
kubectl apply -f namespace.yaml
```

### 2. 部署基础设施

```bash
# 使用Helm部署MySQL
helm repo add bitnami https://charts.bitnami.com/bitnami
helm install mysql bitnami/mysql \
  --namespace basebackend \
  --set auth.rootPassword=root123456 \
  --set auth.database=basebackend

# 部署Redis
helm install redis bitnami/redis \
  --namespace basebackend \
  --set auth.enabled=false

# 部署Nacos
kubectl apply -f nacos-deployment.yaml
```

### 3. 部署业务服务

```bash
# 应用ConfigMap
kubectl apply -f configmap.yaml -n basebackend

# 应用Secrets
kubectl create secret generic db-secret \
  --from-literal=username=basebackend \
  --from-literal=password=basebackend123 \
  -n basebackend

# 部署服务
kubectl apply -f deployments/ -n basebackend
kubectl apply -f services/ -n basebackend
```

### 4. 配置Ingress

```yaml
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: basebackend-ingress
  namespace: basebackend
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: api.basebackend.local
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway-service
            port:
              number: 8080
```

### 5. 自动扩缩容配置

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-api-hpa
  namespace: basebackend
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## 监控配置

### Prometheus监控

```bash
# 安装Prometheus Operator
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --create-namespace
```

### 配置ServiceMonitor

```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: basebackend-metrics
  namespace: basebackend
spec:
  selector:
    matchLabels:
      app: basebackend
  endpoints:
  - port: metrics
    interval: 30s
    path: /actuator/prometheus
```
```

---

### 阶段六：测试和验证（第11-12天）

#### Step 6.1: 单元测试

为每个模块编写测试：

```bash
# 运行所有测试
mvn clean test

# 运行特定模块测试
mvn clean test -pl basebackend-user-api
```

#### Step 6.2: 集成测试

创建集成测试脚本：

**文件**: `bin/test/integration-test.sh`

```bash
#!/bin/bash

echo "Starting integration tests..."

# 测试服务健康检查
services=("gateway:8080" "user-api:8081" "system-api:8082" "auth-api:8083")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    echo "Testing $name on port $port..."

    response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/actuator/health)

    if [ "$response" = "200" ]; then
        echo "✓ $name is healthy"
    else
        echo "✗ $name is not responding (HTTP $response)"
        exit 1
    fi
done

# 测试API端点
echo "Testing API endpoints..."

# 登录测试
login_response=$(curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"admin","password":"admin123"}')

if [[ $login_response == *"token"* ]]; then
    echo "✓ Login API works"
else
    echo "✗ Login API failed"
    exit 1
fi

echo "All integration tests passed!"
```

#### Step 6.3: 性能测试

使用JMeter或K6进行性能测试：

**文件**: `bin/test/performance-test.js` (K6脚本)

```javascript
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    stages: [
        { duration: '2m', target: 100 },  // 逐渐增加到100个用户
        { duration: '5m', target: 100 },  // 保持100个用户
        { duration: '2m', target: 200 },  // 增加到200个用户
        { duration: '5m', target: 200 },  // 保持200个用户
        { duration: '2m', target: 0 },    // 逐渐减少到0
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95%的请求在500ms内完成
        http_req_failed: ['rate<0.1'],     // 错误率小于10%
    },
};

export default function() {
    // 测试登录API
    let loginRes = http.post('http://localhost:8080/api/auth/login',
        JSON.stringify({
            username: 'admin',
            password: 'admin123'
        }), {
            headers: { 'Content-Type': 'application/json' },
        }
    );

    check(loginRes, {
        'login successful': (r) => r.status === 200,
        'token received': (r) => JSON.parse(r.body).token !== undefined,
    });

    let token = JSON.parse(loginRes.body).token;

    // 测试用户列表API
    let usersRes = http.get('http://localhost:8080/api/users', {
        headers: { 'Authorization': `Bearer ${token}` },
    });

    check(usersRes, {
        'users fetched': (r) => r.status === 200,
    });

    sleep(1);
}
```

运行性能测试：
```bash
k6 run bin/test/performance-test.js
```

---

### 阶段七：文档完善和培训（第13-14天）

#### Step 7.1: API文档生成

配置Swagger/OpenAPI：

**文件**: `basebackend-user-api/src/main/java/com/basebackend/user/config/SwaggerConfig.java`

```java
@Configuration
@EnableOpenApi
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("User API")
                .version("1.0.0")
                .description("用户服务API文档")
                .license(new License()
                    .name("Apache 2.0")
                    .url("http://www.apache.org/licenses/LICENSE-2.0")))
            .externalDocs(new ExternalDocumentation()
                .description("项目Wiki")
                .url("https://wiki.basebackend.com"));
    }

    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
            .group("user")
            .pathsToMatch("/api/users/**", "/api/roles/**", "/api/profiles/**")
            .build();
    }
}
```

#### Step 7.2: 开发指南

**文件**: `docs/development/getting-started.md`

```markdown
# 开发入门指南

## 环境准备

### 必需工具
- JDK 17+
- Maven 3.8+
- Docker Desktop
- IDE (推荐 IntelliJ IDEA)

### 可选工具
- Postman (API测试)
- DBeaver (数据库管理)
- Lens (K8s管理)

## 项目结构

```
basebackend/
├── basebackend-common/      # 公共模块
├── basebackend-user-api/    # 用户服务
├── basebackend-system-api/  # 系统服务
├── basebackend-auth-api/    # 认证服务
├── basebackend-gateway/     # API网关
└── docs/                    # 文档
```

## 开发流程

### 1. 拉取代码
```bash
git clone https://github.com/basebackend/basebackend.git
cd basebackend
```

### 2. 启动基础设施
```bash
cd docker/compose
docker-compose -f base/docker-compose.base.yml up -d
```

### 3. 导入IDEA
1. File -> Open -> 选择项目根目录
2. 等待Maven依赖下载完成
3. 配置JDK 17

### 4. 启动服务
在IDEA中依次启动：
1. NacosApplication (配置中心)
2. GatewayApplication (网关)
3. UserApiApplication (用户服务)
4. SystemApiApplication (系统服务)
5. AuthApiApplication (认证服务)

### 5. 验证
访问 http://localhost:8080/swagger-ui.html 查看API文档

## 编码规范

### 包命名
- controller: REST控制器
- service: 业务逻辑
- mapper: 数据访问
- entity: 实体类
- dto: 数据传输对象
- vo: 视图对象
- config: 配置类
- util: 工具类

### 代码风格
遵循阿里巴巴Java开发手册

### Git提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式
- refactor: 重构
- test: 测试
- chore: 构建过程或辅助工具
```

---

## 四、重构检查清单

### 依赖重构检查

- [ ] 所有版本号统一在父POM管理
- [ ] 解决basebackend-security和basebackend-web的循环依赖
- [ ] basebackend-backup不再依赖scheduler
- [ ] 每个模块依赖不超过5个其他模块
- [ ] 所有硬编码版本号已移除

### 模块拆分检查

- [ ] basebackend-admin-api拆分为独立服务
- [ ] 用户服务(user-api)独立部署
- [ ] 系统服务(system-api)独立部署
- [ ] 认证服务(auth-api)独立部署
- [ ] 各服务可独立启动和测试

### Docker配置检查

- [ ] 基础设施Docker Compose配置完整
- [ ] 中间件版本统一
- [ ] 服务编排配置正确
- [ ] 环境变量管理规范
- [ ] 健康检查配置完整

### 文档完整性检查

- [ ] 部署文档结构清晰
- [ ] Docker部署指南完整
- [ ] Kubernetes部署指南完整
- [ ] API文档自动生成
- [ ] 开发指南易于理解

### 测试覆盖检查

- [ ] 单元测试覆盖率>70%
- [ ] 集成测试通过
- [ ] 性能测试达标
- [ ] 端到端测试通过

---

## 五、风险和缓解措施

### 风险1: 服务拆分后的数据一致性

**缓解措施**:
- 使用Seata保证分布式事务
- 实施最终一致性方案
- 添加数据同步机制

### 风险2: 性能下降

**缓解措施**:
- 添加缓存层
- 优化数据库查询
- 实施服务熔断和限流

### 风险3: 配置管理复杂度增加

**缓解措施**:
- 使用Nacos配置中心
- 实施配置版本管理
- 自动化配置验证

---

## 六、后续优化建议

### 短期（1-2个月）
1. 添加服务网格(Istio)
2. 实施灰度发布
3. 完善监控告警

### 中期（3-6个月）
1. 实施CQRS模式
2. 添加事件驱动架构
3. 优化数据库分片

### 长期（6个月以上）
1. 迁移到云原生架构
2. 实施多租户隔离
3. 添加AI运维能力

---

## 七、总结

本重构计划旨在解决当前项目的架构问题，通过：

1. **依赖优化**: 解决循环依赖，统一版本管理
2. **模块拆分**: 将单体服务拆分为微服务
3. **配置规范**: 统一Docker和K8s部署配置
4. **文档完善**: 建立完整的文档体系
5. **测试保障**: 确保重构后的质量

预期效果：
- 提高系统可维护性
- 增强服务可扩展性
- 降低部署复杂度
- 提升开发效率

---

**文档版本**: v1.0
**最后更新**: 2025-11-17
**负责人**: Architecture Team
**审核人**: Tech Lead

## 附录

### A. 命令速查

```bash
# Maven命令
mvn clean install -DskipTests  # 编译打包
mvn dependency:tree            # 查看依赖树
mvn versions:display-dependency-updates  # 检查版本更新

# Docker命令
docker-compose up -d           # 启动服务
docker-compose logs -f service # 查看日志
docker-compose down -v         # 停止并清理

# Kubernetes命令
kubectl get pods -n basebackend        # 查看Pod
kubectl logs -f pod-name -n basebackend # 查看日志
kubectl scale deployment user-api --replicas=3 -n basebackend # 扩缩容
```

### B. 故障排查指南

| 问题 | 可能原因 | 解决方案 |
|-----|---------|---------|
| 服务无法启动 | 端口占用 | 检查并释放端口 |
| 注册中心连接失败 | Nacos未启动 | 启动Nacos服务 |
| 数据库连接失败 | 配置错误 | 检查数据库配置 |
| 服务调用超时 | 网络问题 | 检查网络连通性 |
| 内存溢出 | JVM配置不当 | 调整JVM参数 |

### C. 联系方式

- 架构组: architecture@basebackend.com
- DevOps: devops@basebackend.com
- 紧急联系: 13800138000

---

祝重构顺利！🚀