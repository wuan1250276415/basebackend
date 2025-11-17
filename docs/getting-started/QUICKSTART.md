# BaseBackend Demo API - 快速启动指南

## 项目概述

本项目已成功创建，包含以下模块：

### 核心模块
- **basebackend-common**: 通用工具和模型
- **basebackend-security**: JWT认证
- **basebackend-database**: 数据库访问（MyBatis Plus）
- **basebackend-cache**: Redis缓存（Redisson）
- **basebackend-message-service**: RocketMQ消息队列
- **basebackend-file-service**: 文件服务
- **basebackend-logging**: 日志
- **basebackend-observability**: 可观测性
- **basebackend-gateway**: API网关（WebFlux）
- **basebackend-demo-api**: 演示API（测试所有模块）

## 快速启动

### 1. 最小启动（只需Redis）

如果只想测试基本功能，只需启动 Redis：

```bash
# 启动Redis（Docker方式）
docker run -d -p 6379:6379 redis

# 或者使用本地Redis
redis-server
```

然后启动Demo API：

```bash
cd basebackend-demo-api
mvn spring-boot:run
```

访问: http://localhost:8081/api/health

### 2. 完整启动（所有服务）

#### 启动MySQL

```bash
# Docker方式
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=mysql_S4deB5 \
  -e MYSQL_DATABASE=basebackend_demo \
  mysql:8.0

# 创建数据库
mysql -h localhost -P 3306 -u root -p
# 密码: mysql_S4deB5
CREATE DATABASE IF NOT EXISTS basebackend_demo;
```

#### 启动Redis

```bash
# Docker方式
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis \
  redis-server --requirepass redis_ZJGE42
```

#### 启动RocketMQ（可选）

```bash
# Docker方式
docker run -d \
  --name rocketmq-namesrv \
  -p 9876:9876 \
  apache/rocketmq:latest \
  sh mqnamesrv

docker run -d \
  --name rocketmq-broker \
  --link rocketmq-namesrv:namesrv \
  -p 10911:10911 \
  -p 10909:10909 \
  -e "NAMESRV_ADDR=namesrv:9876" \
  apache/rocketmq:latest \
  sh mqbroker -n namesrv:9876
```

如果不想启动RocketMQ，消息相关的API会自动禁用，不影响其他功能。

### 3. 启动Demo API

```bash
cd /home/wuan/IdeaProjects/basebackend

# 编译整个项目
mvn clean install -DskipTests

# 启动Demo API
cd basebackend-demo-api
mvn spring-boot:run
```

## 测试API

### 方式1: 使用测试脚本

```bash
cd /home/wuan/IdeaProjects/basebackend/basebackend-demo-api
./test-api.sh
```

### 方式2: 使用curl命令

#### 1. 健康检查
```bash
curl http://localhost:8081/api/health
```

#### 2. JWT认证测试
```bash
# 登录获取Token
curl -X POST 'http://localhost:8081/api/auth/login?username=admin&password=123456'

# 验证Token（替换{token}为实际token）
curl -H 'Authorization: Bearer {token}' http://localhost:8081/api/auth/validate
```

#### 3. 缓存测试
```bash
# 设置缓存
curl -X POST 'http://localhost:8081/api/cache/set?key=test&value=hello&ttl=60'

# 获取缓存
curl 'http://localhost:8081/api/cache/get?key=test'

# Hash缓存
curl -X POST 'http://localhost:8081/api/cache/hash/set?key=user' \
  -H 'Content-Type: application/json' \
  -d '{"name":"张三","age":25}'

curl 'http://localhost:8081/api/cache/hash/get?key=user'
```

#### 4. 消息队列测试（需要RocketMQ）
```bash
# 发送消息
curl -X POST 'http://localhost:8081/api/message/sync?topic=demo' \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"001","amount":100}'
```

### 方式3: 使用浏览器

直接访问：
- 健康检查: http://localhost:8081/api/health
- Hello: http://localhost:8081/api/hello?name=张三
- 系统信息: http://localhost:8081/api/system/info

## 可用的API端点

### 健康检查
- GET `/api/health` - 健康检查
- GET `/api/hello` - Hello World
- GET `/api/system/info` - 系统信息

### 认证 (JWT)
- POST `/api/auth/login` - 登录
- GET `/api/auth/validate` - 验证Token
- POST `/api/auth/refresh` - 刷新Token

### 缓存 (Redis)
- POST `/api/cache/set` - 设置缓存
- GET `/api/cache/get` - 获取缓存
- DELETE `/api/cache/delete` - 删除缓存
- POST `/api/cache/hash/set` - 设置Hash
- GET `/api/cache/hash/get` - 获取Hash
- GET `/api/cache/exists` - 检查缓存

### 消息队列 (RocketMQ - 可选)
- POST `/api/message/sync` - 同步消息
- POST `/api/message/async` - 异步消息
- POST `/api/message/oneway` - 单向消息
- POST `/api/message/delay` - 延迟消息
- POST `/api/message/tagged` - 带标签消息
- POST `/api/message/batch` - 批量消息

## 监控端点

- http://localhost:8081/actuator/health
- http://localhost:8081/actuator/metrics
- http://localhost:8081/actuator/prometheus

## 默认配置

### 端口
- Demo API: 8081
- Gateway: 8080

### 数据库
- Host: localhost:3306
- Database: basebackend_demo
- Username: root
- Password: mysql_S4deB5

### Redis
- Host: localhost:6379
- Password: redis_ZJGE42

### RocketMQ
- NameServer: localhost:9876

## 常见问题

### Q: RocketMQ未启动导致启动失败？
A: RocketMQ是可选的。代码已经做了优雅降级，如果RocketMQ未启动，消息相关的Controller会自动禁用，不影响其他功能。

### Q: Redis连接失败？
A: 检查Redis是否启动，并确认密码配置正确（application.yml中配置为`redis_ZJGE42`）

### Q: 数据库连接失败？
A: 如果暂时不需要测试数据库功能，可以在application.yml中注释掉数据源配置

## 下一步

1. 根据实际需求添加业务逻辑
2. 实现数据库CRUD操作演示
3. 添加文件上传下载演示
4. 集成Swagger/OpenAPI文档
5. 添加单元测试和集成测试

## 项目结构

```
basebackend/
├── basebackend-common/          # 通用模块
├── basebackend-security/        # 安全模块（JWT）
├── basebackend-database/        # 数据库模块
├── basebackend-cache/           # 缓存模块（Redis）
├── basebackend-message-service/ # 消息队列模块
├── basebackend-file-service/    # 文件服务模块
├── basebackend-logging/         # 日志模块
├── basebackend-observability/   # 可观测性模块
├── basebackend-gateway/         # API网关
└── basebackend-demo-api/        # 演示API ⭐
    ├── src/main/java/
    │   └── com/basebackend/demo/
    │       ├── DemoApiApplication.java
    │       └── controller/
    │           ├── HealthDemoController.java
    │           ├── AuthDemoController.java
    │           ├── CacheDemoController.java
    │           └── MessageDemoController.java
    ├── src/main/resources/
    │   └── application.yml
    ├── test-api.sh             # 测试脚本
    └── README.md               # 详细文档
```

祝您使用愉快！
