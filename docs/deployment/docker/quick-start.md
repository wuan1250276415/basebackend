# Docker 快速部署指南

本指南帮助你在 5 分钟内使用 Docker 启动完整的 Base Backend 开发环境。

## 前置条件

- Docker 24.0+
- Docker Compose 2.20+
- 至少 8GB 可用内存
- 至少 20GB 可用磁盘空间

### 安装 Docker

**Windows/Mac**:
- 下载并安装 [Docker Desktop](https://www.docker.com/products/docker-desktop)

**Linux**:
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER
```

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/your-org/basebackend.git
cd basebackend
```

### 2. 启动基础设施

```bash
cd docker/compose
./start-all.sh
```

等待约 90 秒，所有服务将自动启动。

### 3. 验证服务

```bash
# 检查服务状态
docker ps | grep basebackend

# 应该看到以下容器:
# - basebackend-mysql
# - basebackend-redis
# - basebackend-nacos
# - basebackend-rocketmq-namesrv
# - basebackend-rocketmq-broker
# - basebackend-rocketmq-console
```

### 4. 访问服务

| 服务 | 地址 | 账号密码 |
|-----|------|---------|
| Nacos 控制台 | http://localhost:8848/nacos | nacos/nacos |
| RocketMQ 控制台 | http://localhost:8180 | 无需登录 |
| MySQL | localhost:3306 | basebackend/basebackend123 |
| Redis | localhost:6379 | 无密码 |

### 5. 导入 Nacos 配置

```bash
# 进入配置目录
cd ../../config/nacos-configs

# 导入配置 (Windows PowerShell)
.\import-nacos-configs.ps1

# 或 (Linux/Mac)
./import-nacos-configs.sh
```

### 6. 启动应用服务

```bash
# 返回项目根目录
cd ../..

# 编译项目
mvn clean install -DskipTests

# 启动 Gateway
cd basebackend-gateway
mvn spring-boot:run

# 在新终端启动 Admin API
cd basebackend-admin-api
mvn spring-boot:run
```

### 7. 验证部署

```bash
# 测试 Gateway 健康检查
curl http://localhost:8080/actuator/health

# 测试 Admin API
curl http://localhost:8081/actuator/health

# 测试登录接口
curl -X POST http://localhost:8080/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

## 停止服务

```bash
# 停止应用服务 (Ctrl+C)

# 停止基础设施
cd docker/compose
./stop-all.sh
```

## 常见问题

### Q1: 端口冲突

**错误**: `Bind for 0.0.0.0:3306 failed: port is already allocated`

**解决方案**:
```bash
# 修改端口配置
vim docker/compose/env/.env.dev

# 修改对应端口，例如:
MYSQL_PORT=3307
```

### Q2: 内存不足

**错误**: 容器频繁重启或 OOM

**解决方案**:
- 打开 Docker Desktop
- Settings → Resources → Memory
- 调整为至少 8GB

### Q3: Nacos 启动失败

**错误**: Nacos 无法连接到 MySQL

**解决方案**:
```bash
# 检查 MySQL 是否完全启动
docker logs basebackend-mysql

# 等待 MySQL 完全启动后重启 Nacos
docker restart basebackend-nacos
```

### Q4: 应用无法连接到 Nacos

**错误**: `Connection refused: nacos:8848`

**解决方案**:
```bash
# 检查 Nacos 是否完全启动
docker logs basebackend-nacos

# 验证 Nacos 健康状态
curl http://localhost:8848/nacos/v1/console/health/readiness
```

## 下一步

- 阅读 [开发指南](../../development/getting-started.md)
- 查看 [API 文档](http://localhost:8080/doc.html)
- 了解 [配置管理](../configuration/nacos.md)

## 故障排查

### 查看日志

```bash
# 查看所有容器日志
docker-compose -f docker/compose/base/docker-compose.base.yml logs

# 查看特定服务日志
docker logs -f basebackend-mysql
docker logs -f basebackend-nacos

# 查看最近 100 行日志
docker logs --tail 100 basebackend-mysql
```

### 重启服务

```bash
# 重启单个服务
docker restart basebackend-mysql

# 重启所有服务
cd docker/compose
./stop-all.sh
./start-all.sh
```

### 清理数据

```bash
# 停止并删除所有数据 (警告: 会删除所有数据!)
cd docker/compose
docker-compose -f base/docker-compose.base.yml down -v
docker-compose -f middleware/docker-compose.middleware.yml down -v
```

## 性能优化

### 调整 JVM 内存

编辑 `docker/compose/middleware/docker-compose.middleware.yml`:

```yaml
nacos:
  environment:
    JVM_XMS: 1024m  # 增加到 1GB
    JVM_XMX: 1024m
```

### 调整 MySQL 配置

编辑 `docker/compose/base/docker-compose.base.yml`:

```yaml
mysql:
  command:
    - --max_connections=2000  # 增加连接数
    - --innodb_buffer_pool_size=2G  # 增加缓冲池
```

## 生产环境部署

⚠️ **警告**: 本指南仅用于开发环境。

生产环境部署请参考:
- [Kubernetes 部署指南](../kubernetes/deployment.md)
- [生产环境最佳实践](production.md)
