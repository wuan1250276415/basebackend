# Docker Compose 部署指南

本目录包含 Base Backend 项目的 Docker Compose 配置文件，用于快速部署开发和测试环境。

## 目录结构

```
docker/compose/
├── base/                           # 基础设施
│   └── docker-compose.base.yml    # MySQL, Redis
├── middleware/                     # 中间件
│   ├── docker-compose.middleware.yml  # Nacos, RocketMQ
│   └── broker.conf                # RocketMQ Broker 配置
├── services/                       # 业务服务 (待添加)
├── env/                           # 环境配置
│   ├── .env.dev                   # 开发环境配置
│   └── .env.example               # 配置模板
├── start-all.sh                   # 启动所有服务
├── stop-all.sh                    # 停止所有服务
└── README.md                      # 本文档
```

## 快速开始

### 1. 配置环境变量

```bash
# 复制配置模板
cp env/.env.example env/.env.dev

# 编辑配置文件，修改密码等敏感信息
vim env/.env.dev
```

### 2. 启动服务

```bash
# 启动所有服务（使用默认开发环境配置）
./start-all.sh

# 或指定环境配置文件
./start-all.sh env/.env.dev
```

### 3. 验证服务

```bash
# 检查服务状态
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev ps
docker-compose -f middleware/docker-compose.middleware.yml --env-file env/.env.dev ps

# 查看日志
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev logs -f mysql
docker-compose -f middleware/docker-compose.middleware.yml --env-file env/.env.dev logs -f nacos
```

### 4. 访问服务

| 服务 | 地址 | 默认账号/密码 |
|-----|------|---------|
| MySQL | localhost:3306 | basebackend/basebackend123 |
| Redis | localhost:6379 | 密码: redis2025 |
| Nacos Console | http://localhost:8848/nacos | nacos/nacos |
| RocketMQ Console | http://localhost:8180 | (无需登录) |

## 分步启动

如果需要分步启动服务：

### 仅启动基础设施

```bash
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev up -d
```

### 仅启动中间件

```bash
docker-compose -f middleware/docker-compose.middleware.yml --env-file env/.env.dev up -d
```

## 停止服务

```bash
# 停止所有服务
./stop-all.sh

# 停止并删除数据卷（警告：会删除所有数据）
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev down -v
docker-compose -f middleware/docker-compose.middleware.yml --env-file env/.env.dev down -v
```

## 环境配置说明

### MySQL 配置

```bash
MYSQL_ROOT_PASSWORD=root123456      # Root 密码
MYSQL_DATABASE=basebackend          # 数据库名
MYSQL_USER=basebackend              # 用户名
MYSQL_PASSWORD=basebackend123       # 用户密码
MYSQL_PORT=3306                     # 端口
```

### Redis 配置

```bash
REDIS_PORT=6379                     # 端口
REDIS_PASSWORD=redis2025            # 密码（默认：redis2025）
```

### Nacos 配置

```bash
NACOS_PORT=8848                     # HTTP 端口
NACOS_GRPC_PORT=9848                # gRPC 端口
NACOS_AUTH_ENABLE=true              # 启用认证
NACOS_AUTH_TOKEN=SecretKey...       # 认证密钥（至少32字符）
NACOS_NAMESPACE=dev                 # 命名空间
```

### RocketMQ 配置

```bash
ROCKETMQ_NAMESRV_PORT=9876          # NameServer 端口
ROCKETMQ_BROKER_PORT=10911          # Broker 端口
ROCKETMQ_VIP_PORT=10909             # VIP 通道端口
ROCKETMQ_CONSOLE_PORT=8180          # 控制台端口
```

## 常见问题

### Q1: MySQL 连接失败

**问题**: 应用无法连接到 MySQL

**解决方案**:
```bash
# 检查 MySQL 是否完全启动
docker logs basebackend-mysql

# 等待 MySQL 完全启动（约30秒）
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev ps
```

### Q2: Nacos 注册失败

**问题**: 服务无法注册到 Nacos

**解决方案**:
```bash
# 检查 Nacos 是否完全启动
docker logs basebackend-nacos

# 验证 Nacos 健康状态
curl http://localhost:8848/nacos/v1/console/health/readiness

# 检查网络连通性
docker exec basebackend-gateway ping nacos
```

### Q3: 端口冲突

**问题**: 端口已被占用

**解决方案**:
```bash
# 修改 env/.env.dev 中的端口配置
# 例如：MYSQL_PORT=3307
```

### Q4: 内存不足

**问题**: Docker 内存不足

**解决方案**:
- 调整 Docker Desktop 的内存配置（推荐至少 8GB）
- 或减少服务副本数
- 或调整 JVM 内存配置（在 docker-compose 文件中）

## 数据持久化

所有数据都存储在 Docker 卷中：

```bash
# 查看数据卷
docker volume ls | grep basebackend

# 备份数据卷
docker run --rm -v mysql-data:/data -v $(pwd):/backup alpine tar czf /backup/mysql-backup.tar.gz /data

# 恢复数据卷
docker run --rm -v mysql-data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql-backup.tar.gz -C /
```

## 生产环境部署

⚠️ **警告**: 本配置仅用于开发和测试环境，生产环境部署请参考：
- [Kubernetes 部署指南](../../docs/deployment/kubernetes/deployment.md)
- [生产环境最佳实践](../../docs/deployment/docker/production.md)

## 故障排查

### 查看所有容器状态

```bash
docker ps -a | grep basebackend
```

### 查看容器日志

```bash
# 实时查看日志
docker logs -f basebackend-mysql
docker logs -f basebackend-nacos
docker logs -f basebackend-rocketmq-namesrv

# 查看最近100行日志
docker logs --tail 100 basebackend-mysql
```

### 进入容器调试

```bash
# 进入 MySQL 容器
docker exec -it basebackend-mysql mysql -uroot -p

# 进入 Redis 容器（需要密码认证）
docker exec -it basebackend-redis redis-cli -a redis2025

# 测试 Redis 连接
docker exec basebackend-redis redis-cli -a redis2025 ping

# 进入 Nacos 容器
docker exec -it basebackend-nacos bash
```

### 重启服务

```bash
# 重启单个服务
docker-compose -f base/docker-compose.base.yml --env-file env/.env.dev restart mysql

# 重启所有服务
./stop-all.sh && ./start-all.sh
```

## 网络配置

所有服务都连接到 `basebackend-network` 网络，可以通过服务名相互访问：

```yaml
# 应用配置示例
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/basebackend  # 使用服务名 'mysql'
  redis:
    host: redis  # 使用服务名 'redis'
  cloud:
    nacos:
      server-addr: nacos:8848  # 使用服务名 'nacos'
```

## Redis连接说明

### 从宿主机连接

```bash
# 使用redis-cli连接（需要密码）
redis-cli -h localhost -p 6379 -a redis2025

# 测试连接
redis-cli -h localhost -p 6379 -a redis2025 ping
# 应返回: PONG

# 运行测试脚本
cd base
./test-redis-connection.sh  # Linux/Mac
test-redis-connection.bat   # Windows
```

### 从应用连接

**Spring Boot配置：**

```yaml
spring:
  redis:
    host: redis  # 容器内使用服务名
    # host: localhost  # 宿主机使用localhost
    port: 6379
    password: redis2025
    timeout: 3000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

### 常见问题

**问题1: 连接被拒绝**
```bash
# 检查Redis是否运行
docker ps | grep redis

# 查看Redis日志
docker logs basebackend-redis
```

**问题2: 认证失败**
```bash
# 确认密码正确
docker exec basebackend-redis redis-cli -a redis2025 ping

# 查看Redis配置
docker exec basebackend-redis redis-cli CONFIG GET requirepass
```

详细故障排查请参考：[Redis故障排查指南](base/REDIS_TROUBLESHOOTING.md)

## 更新日志

- 2025-11-19: 修复Redis密码配置错误，添加Redis连接测试脚本
- 2025-11-17: 初始版本，包含基础设施和中间件配置
