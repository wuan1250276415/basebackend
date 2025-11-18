# BaseBackend 部署指南

> **版本**: v1.0  
> **最后更新**: 2025-11-18  
> **适用环境**: 开发、测试、生产

---

## 📋 目录

1. [环境要求](#环境要求)
2. [快速开始](#快速开始)
3. [Docker部署](#docker部署)
4. [Kubernetes部署](#kubernetes部署)
5. [配置说明](#配置说明)
6. [服务启动顺序](#服务启动顺序)
7. [健康检查](#健康检查)
8. [故障排查](#故障排查)

---

## 环境要求

### 基础环境

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 17+ | 推荐使用OpenJDK 17 |
| Maven | 3.8+ | 用于构建项目 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存和会话存储 |
| Nacos | 2.2+ | 服务注册和配置中心 |
| RocketMQ | 4.9+ | 消息队列（可选） |

### 可观测性栈（可选）

| 组件 | 版本 | 说明 |
|------|------|------|
| Prometheus | latest | 指标收集 |
| Grafana | latest | 可视化 |
| Jaeger | latest | 分布式追踪 |
| Loki | latest | 日志聚合 |

### 硬件要求

**开发环境**:
- CPU: 4核
- 内存: 8GB
- 磁盘: 50GB

**生产环境**:
- CPU: 8核+
- 内存: 16GB+
- 磁盘: 200GB+

---

## 快速开始

### 1. 克隆代码

```bash
git clone https://github.com/your-org/basebackend.git
cd basebackend
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE basebackend CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入表结构
mysql -u root -p basebackend < sql/schema.sql

# 导入初始数据
mysql -u root -p basebackend < sql/data.sql

# 导入可观测性服务表
mysql -u root -p basebackend < basebackend-observability-service/src/main/resources/sql/schema.sql
```

### 3. 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量
vi .env
```

**环境变量示例**:
```bash
# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=basebackend
DB_USERNAME=root
DB_PASSWORD=your_password

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Nacos配置
NACOS_SERVER_ADDR=localhost:8848
NACOS_NAMESPACE=
NACOS_GROUP=DEFAULT_GROUP

# RocketMQ配置
ROCKETMQ_NAME_SERVER=localhost:9876

# Zipkin配置
ZIPKIN_URL=http://localhost:9411
```

### 4. 启动基础设施

```bash
# 启动基础设施（MySQL, Redis, Nacos, RocketMQ）
cd docker/compose/base
docker-compose up -d

# 等待服务启动
sleep 30

# 检查服务状态
docker-compose ps
```

### 5. 编译项目

```bash
# 返回项目根目录
cd ../../..

# 编译所有模块
mvn clean package -DskipTests

# 或者只编译微服务
mvn clean package -DskipTests -pl basebackend-gateway,basebackend-user-api,basebackend-system-api,basebackend-auth-api,basebackend-notification-service,basebackend-observability-service -am
```

### 6. 启动服务

```bash
# 使用启动脚本
./bin/start/start-microservices.sh

# 或手动启动各个服务
java -jar basebackend-gateway/target/basebackend-gateway-1.0.0-SNAPSHOT.jar &
java -jar basebackend-user-api/target/basebackend-user-api-1.0.0-SNAPSHOT.jar &
java -jar basebackend-system-api/target/basebackend-system-api-1.0.0-SNAPSHOT.jar &
java -jar basebackend-auth-api/target/basebackend-auth-api-1.0.0-SNAPSHOT.jar &
java -jar basebackend-notification-service/target/basebackend-notification-service-1.0.0-SNAPSHOT.jar &
java -jar basebackend-observability-service/target/basebackend-observability-service-1.0.0-SNAPSHOT.jar &
```

### 7. 验证部署

```bash
# 检查服务健康状态
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health

# 测试登录
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

---

## Docker部署

### 1. 构建Docker镜像

```bash
# 构建所有服务镜像
./docker/build-all.sh

# 或单独构建
docker build -t basebackend/gateway:latest -f docker/gateway/Dockerfile .
docker build -t basebackend/user-api:latest -f docker/user-api/Dockerfile .
docker build -t basebackend/system-api:latest -f docker/system-api/Dockerfile .
docker build -t basebackend/auth-api:latest -f docker/auth-api/Dockerfile .
docker build -t basebackend/notification-service:latest -f docker/notification-service/Dockerfile .
docker build -t basebackend/observability-service:latest -f docker/observability-service/Dockerfile .
```

### 2. 使用Docker Compose部署

```bash
# 启动所有服务
cd docker/compose
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

### 3. Docker Compose配置

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  gateway:
    image: basebackend/gateway:latest
    ports:
      - "8080:8080"
    environment:
      - NACOS_SERVER_ADDR=nacos:8848
      - REDIS_HOST=redis
    depends_on:
      - nacos
      - redis
    networks:
      - basebackend-network

  user-api:
    image: basebackend/user-api:latest
    ports:
      - "8081:8081"
    environment:
      - NACOS_SERVER_ADDR=nacos:8848
      - DB_HOST=mysql
      - REDIS_HOST=redis
    depends_on:
      - mysql
      - redis
      - nacos
    networks:
      - basebackend-network

  # 其他服务配置...

networks:
  basebackend-network:
    driver: bridge
```

---

## Kubernetes部署

### 1. 准备Kubernetes集群

```bash
# 检查集群状态
kubectl cluster-info
kubectl get nodes
```

### 2. 创建命名空间

```bash
kubectl create namespace basebackend
kubectl config set-context --current --namespace=basebackend
```

### 3. 创建ConfigMap和Secret

```bash
# 创建配置
kubectl create configmap basebackend-config \
  --from-file=config/application.yml

# 创建密钥
kubectl create secret generic basebackend-secret \
  --from-literal=db-password=your_password \
  --from-literal=redis-password=your_password
```

### 4. 部署服务

```bash
# 部署所有服务
kubectl apply -f k8s/

# 或单独部署
kubectl apply -f k8s/gateway-deployment.yaml
kubectl apply -f k8s/user-api-deployment.yaml
kubectl apply -f k8s/system-api-deployment.yaml
```

### 5. 暴露服务

```bash
# 创建Ingress
kubectl apply -f k8s/ingress.yaml

# 或使用LoadBalancer
kubectl expose deployment gateway --type=LoadBalancer --port=8080
```

### 6. 查看部署状态

```bash
# 查看Pod状态
kubectl get pods

# 查看服务
kubectl get services

# 查看日志
kubectl logs -f deployment/gateway
```

---

## 配置说明

### Nacos配置

**上传配置到Nacos**:
```bash
./bin/maintenance/upload-nacos-configs.sh
```

**配置文件列表**:
- common-config.yml - 公共配置
- gateway-config.yml - 网关配置
- user-api-config.yml - 用户服务配置
- system-api-config.yml - 系统服务配置
- auth-api-config.yml - 认证服务配置
- notification-service-config.yml - 通知服务配置
- observability-service-config.yml - 可观测性服务配置

### 环境配置

**开发环境** (dev):
```yaml
spring:
  profiles:
    active: dev
```

**测试环境** (test):
```yaml
spring:
  profiles:
    active: test
```

**生产环境** (prod):
```yaml
spring:
  profiles:
    active: prod
```

---

## 服务启动顺序

**推荐启动顺序**:

1. **基础设施** (必须先启动)
   - MySQL
   - Redis
   - Nacos
   - RocketMQ (可选)

2. **核心服务**
   - Auth API (认证服务)
   - User API (用户服务)
   - System API (系统服务)

3. **扩展服务**
   - Notification Service (通知服务)
   - Observability Service (可观测性服务)
   - File Service (文件服务)

4. **网关**
   - Gateway (API网关)

**等待时间**: 每个服务启动后等待10-30秒，确保注册到Nacos

---

## 健康检查

### 服务健康检查

```bash
# 网关
curl http://localhost:8080/actuator/health

# 用户服务
curl http://localhost:8081/actuator/health

# 系统服务
curl http://localhost:8082/actuator/health

# 认证服务
curl http://localhost:8083/actuator/health

# 通知服务
curl http://localhost:8086/actuator/health

# 可观测性服务
curl http://localhost:8087/actuator/health
```

### Nacos服务注册检查

```bash
# 查看已注册服务
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api
```

### 数据库连接检查

```bash
mysql -h localhost -u root -p -e "SELECT 1"
```

### Redis连接检查

```bash
redis-cli ping
```

---

## 故障排查

### 服务无法启动

**检查日志**:
```bash
tail -f logs/basebackend-user-api.log
```

**常见问题**:
1. 端口被占用 - 修改端口或停止占用进程
2. 数据库连接失败 - 检查数据库配置和网络
3. Nacos连接失败 - 确认Nacos已启动

### 服务无法注册到Nacos

**检查Nacos配置**:
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

**检查网络连接**:
```bash
telnet localhost 8848
```

### 网关无法路由

**检查路由配置**:
```bash
curl http://localhost:8080/actuator/gateway/routes
```

**检查服务发现**:
```bash
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api
```

---

## 性能调优

### JVM参数

**生产环境推荐**:
```bash
java -Xms2g -Xmx4g \
  -XX:MetaspaceSize=256m \
  -XX:MaxMetaspaceSize=512m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/heapdump.hprof \
  -jar app.jar
```

### 数据库连接池

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
```

### Redis连接池

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

---

## 监控部署

### 启动可观测性栈

```bash
cd docker/compose/observability
./start-observability.sh
```

**访问地址**:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Jaeger: http://localhost:16686

---

## 备份和恢复

### 数据库备份

```bash
# 备份
mysqldump -u root -p basebackend > backup_$(date +%Y%m%d).sql

# 恢复
mysql -u root -p basebackend < backup_20251118.sql
```

### 配置备份

```bash
# 备份Nacos配置
./bin/maintenance/backup-nacos-configs.sh
```

---

## 相关文档

- [API文档](./API_DOCUMENTATION.md)
- [运维手册](./OPERATIONS_GUIDE.md)
- [性能优化](./PERFORMANCE_OPTIMIZATION.md)
- [故障排查](./TROUBLESHOOTING.md)

---

**文档维护**: 运维团队  
**最后更新**: 2025-11-18
