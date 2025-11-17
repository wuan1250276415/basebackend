# BaseBackend 微服务部署指南

## 📋 概述

本文档详细介绍了 BaseBackend 微服务架构的完整部署流程，包括环境准备、依赖安装、服务部署、配置管理等。

---

## 🏗️ 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                    │
│                   Spring Cloud Gateway                   │
└─────────────────────┬───────────────────────────────────┘
                      │
        ┌─────────────┴─────────────┐
        │                           │
┌───────▼────────┐         ┌───────▼────────┐
│  用户服务 (8081)  │         │  权限服务 (8082)  │
│   MySQL        │         │   MySQL        │
│   Redis        │         │   Redis        │
└────────────────┘         └────────────────┘

┌─────────────────────────────────────────────────────────┐
│              业务服务集群                                │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐           │
│  │字典  │ │部门  │ │日志  │ │菜单  │ │监控  │           │
│  │8083  │ │8084  │ │8085  │ │8088  │ │8089  │           │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘           │
│                                                           │
│  ┌────────┐ ┌────────┐ ┌────────┐                        │
│  │通知    │ │个人配置│ │应用    │                        │
│  │8090    │ │8091    │ │8086    │                        │
│  └────────┘ └────────┘ └────────┘                        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              基础服务设施                                │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐            │
│  │ MySQL  │ │ Redis  │ │ Nacos  │ │Sentinel│            │
│  │ 3306   │ │ 6379   │ │ 8848   │ │ 8080   │            │
│  └────────┘ └────────┘ └────────┘ └────────┘            │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 环境要求

### 硬件要求

| 组件 | CPU | 内存 | 磁盘 | 网络 |
|------|-----|------|------|------|
| 最小配置 | 4核 | 8GB | 100GB | 1Gbps |
| 推荐配置 | 8核 | 16GB | 200GB | 1Gbps |
| 生产配置 | 16核 | 32GB | 500GB | 10Gbps |

### 软件要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | Java 运行环境 |
| Maven | 3.8+ | 项目构建工具 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.0+ | 缓存和会话存储 |
| Nacos | 2.2+ | 服务发现和配置中心 |
| Sentinel | 1.8+ | 流量控制组件 |

---

## 🚀 快速开始

### 1. 一键部署所有服务

```bash
# 克隆项目
git clone <repository-url>
cd basebackend

# 运行快速部署脚本
chmod +x scripts/quick-deploy.sh
./scripts/quick-deploy.sh
```

**快速部署脚本** (`scripts/quick-deploy.sh`)：
```bash
#!/bin/bash
set -e

echo "======================================="
echo "BaseBackend 微服务快速部署"
echo "======================================="

# 检查环境
echo "1. 检查环境..."
./scripts/check-environment.sh

# 启动依赖服务
echo "2. 启动依赖服务..."
./scripts/start-dependencies.sh

# 启动微服务
echo "3. 启动微服务..."
./scripts/start-services.sh

# 验证部署
echo "4. 验证部署..."
./scripts/verify-deployment.sh

echo "======================================="
echo "部署完成！"
echo "API Gateway: http://localhost:8080"
echo "API 文档: http://localhost:8080/swagger-ui.html"
echo "======================================="
```

---

## 📦 详细部署步骤

### 第一步：环境准备

#### 1.1 安装 JDK 17

```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install openjdk-17-jdk

# CentOS/RHEL
sudo yum install java-17-openjdk-devel

# 验证安装
java -version
javac -version
```

#### 1.2 安装 Maven

```bash
# Ubuntu/Debian
sudo apt-get install maven

# 或手动安装
wget https://dlcdn.apache.org/maven/maven-3/3.9.4/binaries/apache-maven-3.9.4-bin.tar.gz
tar -xzf apache-maven-3.9.4-bin.tar.gz
sudo mv apache-maven-3.9.4 /opt/maven
export PATH=$PATH:/opt/maven/bin

# 验证安装
mvn -version
```

#### 1.3 准备部署目录

```bash
# 创建项目目录
mkdir -p /opt/basebackend
cd /opt/basebackend

# 创建日志目录
mkdir -p logs
mkdir -p logs/{user-service,auth-service,dept-service,dict-service}
```

### 第二步：部署依赖服务

#### 2.1 部署 MySQL

```bash
# 安装 MySQL
sudo apt-get install mysql-server-8.0

# 启动 MySQL
sudo systemctl start mysql
sudo systemctl enable mysql

# 创建数据库
mysql -u root -p << EOF
CREATE DATABASE IF NOT EXISTS basebackend DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_dept DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_dict DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_log DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_menu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_monitor DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_profile DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS basebackend_application DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'basebackend'@'%' IDENTIFIED BY 'basebackend123';
GRANT ALL PRIVILEGES ON basebackend.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_auth.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_dept.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_dict.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_log.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_menu.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_monitor.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_notification.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_profile.* TO 'basebackend'@'%';
GRANT ALL PRIVILEGES ON basebackend_application.* TO 'basebackend'@'%';

FLUSH PRIVILEGES;
EOF
```

#### 2.2 部署 Redis

```bash
# 安装 Redis
sudo apt-get install redis-server

# 配置 Redis
sudo cp redis.conf /etc/redis/redis.conf

# 启动 Redis
sudo systemctl start redis
sudo systemctl enable redis

# 验证 Redis
redis-cli ping
# 返回 PONG 表示正常
```

#### 2.3 部署 Nacos

```bash
# 下载 Nacos
wget https://github.com/alibaba/nacos/releases/download/2.2.3/nacos-server-2.2.3.tar.gz
tar -xzf nacos-server-2.2.3.tar.gz
cd nacos/bin

# 启动 Nacos (standalone mode)
./startup.sh -m standalone

# 验证 Nacos
curl http://localhost:8848/nacos/v1/console/health/readiness
```

#### 2.4 部署 Sentinel

```bash
# 下载 Sentinel
wget https://github.com/alibaba/Sentinel/releases/download/v1.8.6/sentinel-dashboard-1.8.6.jar

# 启动 Sentinel
java -Dserver.port=8080 -jar sentinel-dashboard-1.8.6.jar &

# 验证 Sentinel
curl http://localhost:8080
# 默认用户名密码: sentinel/sentinel
```

### 第三步：编译项目

```bash
# 克隆项目
cd /opt/basebackend
git clone <repository-url> .
git checkout dev

# 编译所有服务
mvn clean install -DskipTests

# 编译结果
# [INFO] BUILD SUCCESS
# [INFO] Total time: 120s
```

### 第四步：部署微服务

#### 4.1 启动所有服务

使用统一启动脚本：

```bash
chmod +x scripts/start-all-services.sh
./scripts/start-all-services.sh
```

**启动脚本** (`scripts/start-all-services.sh`)：
```bash
#!/bin/bash

services=(
    "basebackend-user-service:8081"
    "basebackend-auth-service:8082"
    "basebackend-dict-service:8083"
    "basebackend-dept-service:8084"
    "basebackend-log-service:8085"
    "basebackend-menu-service:8088"
    "basebackend-monitor-service:8089"
    "basebackend-notification-service:8090"
    "basebackend-profile-service:8091"
    "basebackend-application-service:8086"
)

for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"
    echo "启动服务: $service (端口: $port)"

    cd $service
    mvn spring-boot:run \
        -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
        > ../logs/${service}.log 2>&1 &

    cd ..
    PID=$!
    echo "$service PID: $PID"

    # 等待服务启动
    sleep 10

    # 检查服务状态
    if curl -f http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        echo "✅ $service 启动成功"
    else
        echo "❌ $service 启动失败"
    fi
done

echo "所有服务启动完成"
```

#### 4.2 启动 API Gateway

```bash
cd basebackend-gateway
mvn spring-boot:run \
    -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m" \
    > ../logs/gateway.log 2>&1 &

echo "Gateway 启动，PID: $!"
```

### 第五步：验证部署

#### 5.1 检查服务状态

```bash
#!/bin/bash

echo "======================================="
echo "服务状态检查"
echo "======================================="

services=(
    "Gateway:8080"
    "User-Service:8081"
    "Auth-Service:8082"
    "Dict-Service:8083"
    "Dept-Service:8084"
    "Log-Service:8085"
    "Menu-Service:8088"
    "Monitor-Service:8089"
    "Notification-Service:8090"
    "Profile-Service:8091"
    "Application-Service:8086"
)

for service_info in "${services[@]}"; do
    IFS=':' read -r service port <<< "$service_info"

    if curl -f http://localhost:${port}/actuator/health > /dev/null 2>&1; then
        echo "✅ $service (端口: $port) - 运行正常"
    else
        echo "❌ $service (端口: $port) - 未响应"
    fi
done

echo "======================================="
```

#### 5.2 测试 API 接口

```bash
#!/bin/bash

echo "======================================="
echo "API 接口测试"
echo "======================================="

# 测试用户服务
echo "1. 测试用户服务..."
curl -X GET http://localhost:8081/api/users \
    -H "Content-Type: application/json" \
    -w "HTTP状态码: %{http_code}\n"

# 测试权限服务
echo "2. 测试权限服务..."
curl -X GET http://localhost:8082/api/auth/roles \
    -H "Content-Type: application/json" \
    -w "HTTP状态码: %{http_code}\n"

# 测试字典服务
echo "3. 测试字典服务..."
curl -X GET http://localhost:8083/api/dict/types \
    -H "Content-Type: application/json" \
    -w "HTTP状态码: %{http_code}\n"

echo "======================================="
```

### 第六步：导入配置

#### 6.1 导入 Nacos 配置

```bash
# 用户服务配置
cp nacos-configs/basebackend-user-service.yml /path/to/nacos/config/
cp nacos-configs/import-nacos-config.sh basebackend-user-service/src/main/resources/config/
cd basebackend-user-service/src/main/resources/config/
chmod +x import-nacos-config.sh
./import-nacos-config.sh

# 权限服务配置
cp nacos-configs/basebackend-auth-service.yml /path/to/nacos/config/
cp nacos-configs/import-nacos-config.sh basebackend-auth-service/src/main/resources/config/
cd basebackend-auth-service/src/main/resources/config/
chmod +x import-nacos-config.sh
./import-nacos-config.sh

# 部门服务配置
cp nacos-configs/basebackend-dept-service.yml /path/to/nacos/config/

# 字典服务配置
cp nacos-configs/basebackend-dict-service.yml /path/to/nacos/config/
```

#### 6.2 导入数据库数据

```bash
# 用户表
mysql -u root -p basebackend < basebackend-user-service/src/main/resources/db/migration/V1__Create_user_tables.sql

# 权限表
mysql -u root -p basebackend_auth < basebackend-auth-service/src/main/resources/db/migration/V1__Create_auth_tables.sql

# 部门表
mysql -u root -p basebackend_dept < basebackend-dept-service/src/main/resources/db/migration/V1__Create_dept_tables.sql

# 字典表
mysql -u root -p basebackend_dict < basebackend-dict-service/src/main/resources/db/migration/V1__Create_dict_tables.sql
```

---

## 🔧 配置管理

### 环境变量配置

创建 `.env` 文件：

```bash
# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_USERNAME=basebackend
export DB_PASSWORD=basebackend123

# Redis 配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# Nacos 配置
export NACOS_SERVER_ADDR=localhost:8848
export NACOS_NAMESPACE=basebackend
export NACOS_GROUP=DEFAULT_GROUP

# JWT 配置
export JWT_SECRET=BaseBackendSecretKey2023
export JWT_EXPIRATION=86400

# Sentinel 配置
export SENTINEL_DASHBOARD=localhost:8080
```

加载环境变量：

```bash
# 临时生效
source .env

# 永久生效 (添加到 ~/.bashrc)
echo "source /opt/basebackend/.env" >> ~/.bashrc
source ~/.bashrc
```

---

## 📊 监控与告警

### 1. 启用监控

所有服务已内置 Actuator 监控：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

### 2. 查看健康状态

```bash
# 查看所有服务健康状态
curl http://localhost:<port>/actuator/health

# 查看 Prometheus 指标
curl http://localhost:<port>/actuator/prometheus
```

### 3. 日志查看

```bash
# 查看服务日志
tail -f logs/user-service.log
tail -f logs/auth-service.log
tail -f logs/gateway.log

# 查看错误日志
grep ERROR logs/*.log

# 查看慢查询日志
grep "Slow query" logs/*.log
```

---

## 🔄 服务管理

### 启动服务

```bash
# 启动单个服务
cd basebackend-user-service
mvn spring-boot:run &

# 批量启动所有服务
./scripts/start-all-services.sh
```

### 停止服务

```bash
# 停止单个服务
pkill -f "spring-boot:run"

# 或使用端口停止
lsof -ti:8081 | xargs kill -9

# 停止所有服务
./scripts/stop-all-services.sh
```

### 重启服务

```bash
# 重启单个服务
./scripts/restart-service.sh user-service

# 重启所有服务
./scripts/restart-all-services.sh
```

---

## 🚨 故障排查

### 常见问题

#### 1. 服务启动失败

**问题**: 服务无法启动或启动后立即停止

**排查步骤**:
```bash
# 1. 检查端口是否被占用
lsof -i :8081

# 2. 检查日志
tail -f logs/user-service.log

# 3. 检查数据库连接
mysql -u basebackend -p -h localhost basebackend

# 4. 检查 Redis 连接
redis-cli ping

# 5. 检查 Nacos 连接
curl http://localhost:8848/nacos/v1/console/health/readiness
```

**解决方案**:
- 释放被占用的端口
- 检查数据库、Redis、Nacos 服务是否正常
- 检查配置文件中的连接参数是否正确

#### 2. API 调用失败

**问题**: API 返回 4xx 或 5xx 错误

**排查步骤**:
```bash
# 1. 检查服务状态
curl http://localhost:8081/actuator/health

# 2. 检查网关路由
curl http://localhost:8080/api/users

# 3. 查看网关日志
tail -f logs/gateway.log

# 4. 查看服务日志
tail -f logs/user-service.log
```

**解决方案**:
- 检查服务是否正常运行
- 检查网关配置是否正确
- 检查认证 Token 是否有效

#### 3. 性能问题

**问题**: API 响应缓慢

**排查步骤**:
```bash
# 1. 查看性能指标
curl http://localhost:8081/actuator/prometheus

# 2. 查看慢查询日志
grep "Slow query" logs/user-service.log

# 3. 查看数据库慢查询
mysql -u root -p -e "SELECT * FROM mysql.slow_log ORDER BY start_time DESC LIMIT 10;"

# 4. 查看系统资源
top
free -h
```

**解决方案**:
- 优化数据库查询
- 调整 JVM 参数
- 增加缓存
- 升级硬件资源

---

## 📚 参考资料

- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud 官方文档](https://docs.spring.io/spring-cloud/docs/current/reference/html/)
- [MySQL 官方文档](https://dev.mysql.com/doc/)
- [Redis 官方文档](https://redis.io/documentation/)
- [Nacos 官方文档](https://nacos.io/en-us/docs/what-is-nacos.html)
- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/docs/overview.html)

---

## 📞 技术支持

如有问题，请联系：

- **邮箱**: support@basebackend.com
- **QQ 群**: 123456789
- **文档**: https://docs.basebackend.com

---

**编制**: 浮浮酱 🐱（猫娘工程师）
**日期**: 2025-11-15
**版本**: v1.0.0
