[< 返回首页](Home) | [< 上一页: 配置参考](配置参考)

---

# Docker 部署

---

## 概述

BaseBackend 提供完整的 Docker Compose 编排文件，按功能分组在 `docker/compose/` 目录下，支持一键部署所有基础设施和微服务。

---

## 目录结构

```
docker/
├── compose/
│   ├── base/               # 基础设施（MySQL + Redis）
│   │   └── docker-compose.yml
│   ├── middleware/          # 中间件（Nacos + RocketMQ + Seata + MinIO）
│   │   └── docker-compose.yml
│   ├── observability/       # 可观测性（Prometheus + Loki + Tempo + Grafana）
│   │   └── docker-compose.yml
│   └── services/            # 微服务
│       └── docker-compose.yml
└── seata-server/            # Seata Server 独立配置
    ├── docker-compose.yml
    └── config/
        └── application.yml
```

---

## 一键部署

### 完整环境启动

```bash
# 第一步：启动基础设施
cd docker/compose/base
docker-compose up -d

# 第二步：等待 MySQL 就绪（约 30 秒）
sleep 30

# 第三步：启动中间件
cd ../middleware
docker-compose up -d

# 第四步：启动可观测性（可选）
cd ../observability
docker-compose up -d

# 第五步：启动微服务
cd ../services
docker-compose up -d
```

### 快速验证

```bash
# 检查所有容器状态
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

# 检查各服务健康
curl http://localhost:8180/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # User API
curl http://localhost:8082/actuator/health   # System API
```

---

## 基础设施 (`docker/compose/base/`)

### MySQL 8

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: basebackend-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123456}
      MYSQL_DATABASE: basebackend
      MYSQL_CHARSET: utf8mb4
      MYSQL_COLLATION: utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init:/docker-entrypoint-initdb.d
    command: >
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --default-authentication-plugin=mysql_native_password
      --max-connections=500
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    container_name: basebackend-redis
    ports:
      - "6379:6379"
    command: redis-server --requirepass ${REDIS_PASSWORD:-}
    volumes:
      - redis-data:/data
    restart: unless-stopped

volumes:
  mysql-data:
  redis-data:
```

---

## 中间件 (`docker/compose/middleware/`)

### Nacos

```yaml
services:
  nacos:
    image: nacos/nacos-server:v2.3.0
    container_name: basebackend-nacos
    ports:
      - "8848:8848"
      - "9848:9848"
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "true"
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123456}
    restart: unless-stopped
```

### RocketMQ

```yaml
services:
  rocketmq-namesrv:
    image: apache/rocketmq:5.2.0
    container_name: basebackend-rocketmq-namesrv
    ports:
      - "9876:9876"
    command: sh mqnamesrv
    restart: unless-stopped

  rocketmq-broker:
    image: apache/rocketmq:5.2.0
    container_name: basebackend-rocketmq-broker
    ports:
      - "10911:10911"
    environment:
      NAMESRV_ADDR: rocketmq-namesrv:9876
    command: sh mqbroker -n rocketmq-namesrv:9876
    depends_on:
      - rocketmq-namesrv
    restart: unless-stopped

  rocketmq-dashboard:
    image: apacherocketmq/rocketmq-dashboard:latest
    container_name: basebackend-rocketmq-dashboard
    ports:
      - "8180:8080"
    environment:
      JAVA_OPTS: "-Drocketmq.namesrv.addr=rocketmq-namesrv:9876"
    depends_on:
      - rocketmq-namesrv
    restart: unless-stopped
```

### MinIO

```yaml
services:
  minio:
    image: minio/minio
    container_name: basebackend-minio
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ROOT_USER:-admin}
      MINIO_ROOT_PASSWORD: ${MINIO_ROOT_PASSWORD:-admin123456}
    command: server /data --console-address ":9001"
    volumes:
      - minio-data:/data
    restart: unless-stopped
```

---

## 可观测性 (`docker/compose/observability/`)

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    container_name: basebackend-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    restart: unless-stopped

  loki:
    image: grafana/loki:latest
    container_name: basebackend-loki
    ports:
      - "3100:3100"
    restart: unless-stopped

  tempo:
    image: grafana/tempo:latest
    container_name: basebackend-tempo
    ports:
      - "3200:3200"
      - "4318:4318"    # OTLP HTTP
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: basebackend-grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    volumes:
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus
      - loki
      - tempo
    restart: unless-stopped
```

---

## 环境变量说明

创建 `.env` 文件配置环境变量：

```bash
# docker/compose/base/.env
MYSQL_ROOT_PASSWORD=root123456
REDIS_PASSWORD=

# docker/compose/middleware/.env
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=admin123456
```

---

## 常用 Docker 命令

```bash
# 查看所有容器
docker ps -a

# 查看容器日志
docker logs -f basebackend-mysql
docker logs -f basebackend-redis

# 进入 MySQL 容器
docker exec -it basebackend-mysql mysql -u root -p

# 进入 Redis 容器
docker exec -it basebackend-redis redis-cli

# 停止所有服务
cd docker/compose/base && docker-compose down
cd ../middleware && docker-compose down
cd ../observability && docker-compose down

# 清理所有数据（危险操作）
docker-compose down -v    # 删除 volumes
```

---

## 端口汇总

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Nacos | 8848, 9848 | 注册/配置中心 |
| RocketMQ NameServer | 9876 | 消息队列 |
| RocketMQ Broker | 10911 | 消息队列 |
| Seata Server | 8091 | 分布式事务 |
| MinIO API | 9000 | 对象存储 |
| MinIO Console | 9001 | 对象存储控制台 |
| Prometheus | 9090 | 监控 |
| Grafana | 3000 | 监控仪表盘 |
| Loki | 3100 | 日志聚合 |
| Tempo | 3200, 4318 | 链路追踪 |
| Gateway | 8180 | API 网关 |
| User API | 8081 | 用户服务 |
| System API | 8082 | 系统管理 |

---

| [< 上一页: 配置参考](配置参考) | [下一页: 生产环境配置 >](生产环境配置) |
|---|---|
