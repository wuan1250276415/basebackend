# 优化后快速启动指南

本指南帮助您在完成优化后快速启动和验证微服务系统。

## 前置条件

### 必需软件
- ✅ JDK 17+
- ✅ Maven 3.8+
- ✅ Docker & Docker Compose
- ✅ MySQL 8.0+
- ✅ Redis 6.0+

### 环境变量
```bash
# JWT密钥（可选，有默认值）
export JWT_SECRET="basebackend-secret-key-for-jwt-token-generation-minimum-256-bits"

# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=basebackend_admin
export DB_USERNAME=root
export DB_PASSWORD=your_password

# Redis配置
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# Nacos配置
export NACOS_SERVER=localhost:8848
```

## 快速启动步骤

### 步骤1: 启动基础设施（5分钟）

```bash
# 1. 启动MySQL和Redis
cd docker/compose/base
docker-compose up -d

# 2. 等待服务就绪
docker-compose ps

# 3. 启动Nacos
cd ../middleware
docker-compose up -d nacos

# 4. 验证Nacos启动
# 访问: http://localhost:8848/nacos
# 默认账号: nacos/nacos
```

### 步骤2: 初始化数据库（2分钟）

```bash
# 1. 连接MySQL
mysql -h localhost -u root -p

# 2. 创建数据库
CREATE DATABASE IF NOT EXISTS basebackend_admin 
  DEFAULT CHARACTER SET utf8mb4 
  DEFAULT COLLATE utf8mb4_unicode_ci;

# 3. 执行初始化脚本
USE basebackend_admin;
SOURCE basebackend-admin-api/src/main/resources/db/schema.sql;

# 4. 插入测试数据（可选）
SOURCE basebackend-admin-api/src/main/resources/db/data.sql;
```

### 步骤3: 编译项目（1分钟）

```bash
# 编译所有微服务
mvn clean package -DskipTests

# 或只编译三个核心微服务
mvn clean package -pl basebackend-user-api,basebackend-system-api,basebackend-auth-api -am -DskipTests
```

### 步骤4: 启动微服务（2分钟）

#### 方式1: 使用启动脚本（推荐）
```bash
# Windows
bin\start\start-microservices.bat

# Linux/Mac
./bin/start/start-microservices.sh
```

#### 方式2: 手动启动
```bash
# 终端1: 启动User API
cd basebackend-user-api
mvn spring-boot:run

# 终端2: 启动System API
cd basebackend-system-api
mvn spring-boot:run

# 终端3: 启动Auth API
cd basebackend-auth-api
mvn spring-boot:run
```

#### 方式3: 使用JAR包启动
```bash
# 启动User API
java -jar basebackend-user-api/target/basebackend-user-api-1.0.0-SNAPSHOT.jar &

# 启动System API
java -jar basebackend-system-api/target/basebackend-system-api-1.0.0-SNAPSHOT.jar &

# 启动Auth API
java -jar basebackend-auth-api/target/basebackend-auth-api-1.0.0-SNAPSHOT.jar &
```

### 步骤5: 验证服务（3分钟）

#### 1. 检查服务注册
```bash
# 查看Nacos服务列表
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-user-api"
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-system-api"
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=basebackend-auth-api"
```

#### 2. 测试健康检查
```bash
# User API健康检查
curl http://localhost:8081/actuator/health

# System API健康检查
curl http://localhost:8082/actuator/health

# Auth API健康检查
curl http://localhost:8083/actuator/health
```

#### 3. 测试认证接口
```bash
# 用户登录
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# 响应示例:
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
#   "expiresIn": 3600,
#   "userInfo": {
#     "userId": 1,
#     "username": "admin"
#   }
# }
```

#### 4. 测试用户接口
```bash
# 获取用户列表（需要Token）
TOKEN="your_access_token_here"

curl -X GET "http://localhost:8081/api/user/users?current=1&size=10" \
  -H "Authorization: Bearer $TOKEN"
```

#### 5. 测试系统接口
```bash
# 获取部门树
curl -X GET http://localhost:8082/api/system/depts/tree \
  -H "Authorization: Bearer $TOKEN"

# 获取菜单树
curl -X GET http://localhost:8082/api/system/menus/tree \
  -H "Authorization: Bearer $TOKEN"

# 获取字典数据
curl -X GET "http://localhost:8082/api/system/dicts/data?dictType=user_status" \
  -H "Authorization: Bearer $TOKEN"
```

## API文档访问

启动服务后，可以通过以下地址访问API文档：

- **User API**: http://localhost:8081/doc.html
- **System API**: http://localhost:8082/doc.html
- **Auth API**: http://localhost:8083/doc.html

## 常见问题排查

### 问题1: 服务无法启动
```bash
# 检查端口占用
netstat -ano | findstr "8081"
netstat -ano | findstr "8082"
netstat -ano | findstr "8083"

# 检查日志
tail -f logs/info.log
tail -f logs/error.log
```

### 问题2: 无法连接数据库
```bash
# 检查MySQL是否运行
docker ps | grep mysql

# 检查数据库连接
mysql -h localhost -u root -p -e "SELECT 1"

# 检查数据库是否存在
mysql -h localhost -u root -p -e "SHOW DATABASES LIKE 'basebackend%'"
```

### 问题3: 无法连接Redis
```bash
# 检查Redis是否运行
docker ps | grep redis

# 测试Redis连接
redis-cli ping
```

### 问题4: 服务未注册到Nacos
```bash
# 检查Nacos是否运行
curl http://localhost:8848/nacos/v1/console/health/readiness

# 检查服务配置
# 查看 bootstrap.yml 中的 nacos 配置
cat basebackend-user-api/src/main/resources/bootstrap.yml
```

### 问题5: JWT Token验证失败
```bash
# 检查JWT密钥配置
echo $JWT_SECRET

# 检查Redis中的Token
redis-cli
> KEYS auth:token:*
> GET auth:token:your_token_here
```

## 性能测试

### 基准测试
```bash
# 安装Apache Bench
# Windows: 下载Apache HTTP Server
# Linux: sudo apt-get install apache2-utils
# Mac: brew install ab

# 测试登录接口
ab -n 1000 -c 10 -p login.json -T application/json \
  http://localhost:8083/api/auth/login

# login.json内容:
# {"username":"admin","password":"admin123"}
```

### 压力测试
```bash
# 使用JMeter进行压力测试
# 1. 下载JMeter
# 2. 创建测试计划
# 3. 添加HTTP请求
# 4. 运行测试
```

## 监控和日志

### 查看日志
```bash
# 实时查看日志
tail -f logs/info.log

# 查看错误日志
tail -f logs/error.log

# 查看特定服务日志
tail -f logs/basebackend-user-api.log
```

### 查看指标
```bash
# Prometheus指标
curl http://localhost:8081/actuator/prometheus
curl http://localhost:8082/actuator/prometheus
curl http://localhost:8083/actuator/prometheus

# 应用指标
curl http://localhost:8081/actuator/metrics
```

## 停止服务

### 停止微服务
```bash
# 如果使用脚本启动
./bin/stop/stop-microservices.sh

# 如果手动启动，找到进程并停止
ps aux | grep basebackend
kill -9 <PID>
```

### 停止基础设施
```bash
# 停止所有Docker容器
cd docker/compose/base
docker-compose down

cd ../middleware
docker-compose down
```

## 下一步

完成快速启动后，您可以：

1. **开发新功能** - 基于现有的Service实现添加新的业务逻辑
2. **集成测试** - 编写集成测试验证服务间调用
3. **性能优化** - 添加缓存、优化查询、调整JVM参数
4. **监控告警** - 配置Prometheus、Grafana、日志收集
5. **部署上线** - 使用Docker Compose或Kubernetes部署

## 相关文档

- [优化完成报告](./OPTIMIZATION_COMPLETION_REPORT.md)
- [优化待办清单](./OPTIMIZATION_TODO.md)
- [微服务指南](./MICROSERVICES_GUIDE.md)
- [部署文档](./deployment/README.md)

## 技术支持

如遇到问题，请查看：
1. 项目日志文件 `logs/`
2. 错误日志 `logs/error.log`
3. 服务健康检查 `/actuator/health`
4. Nacos控制台 http://localhost:8848/nacos

---

**文档版本**: v1.0  
**更新时间**: 2025-11-17  
**适用版本**: BaseBackend 1.0.0-SNAPSHOT
