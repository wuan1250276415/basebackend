# BaseBackend Demo API

演示API模块，用于测试基础架构各个模块的功能。

## 功能模块

本演示API集成了以下模块：
- Common: 通用工具和模型
- Security: JWT认证
- Database: 数据库访问
- Cache: Redis缓存
- Message Service: RocketMQ消息队列
- File Service: 文件服务
- Logging: 日志
- Observability: 可观测性

## 快速开始

### 前置条件

确保以下服务已启动：
1. **MySQL** (端口 3306) - 测试数据库功能时需要
2. **Redis** (端口 6379) - 测试缓存功能时需要
3. **RocketMQ** (端口 9876) - 可选，测试消息队列功能时需要

### 初始化数据库

```bash
# 使用脚本初始化数据库
cd basebackend-demo-api
./init-database.sh
```

或手动执行SQL：
```bash
mysql -h localhost -P 3306 -u root -p < src/main/resources/db/schema.sql
# 密码: mysql_S4deB5
```

脚本会自动创建：
- 数据库 `basebackend_demo`
- 用户表 `demo_user`（包含5个测试用户）
- 文章表 `demo_article`（包含10篇测试文章）

### 启动应用

```bash
# 编译项目
mvn clean install

# 启动Demo API
cd basebackend-demo-api
mvn spring-boot:run
```

应用将在 `http://localhost:8081` 启动

## API 文档

### 1. 健康检查 API

#### 健康检查
```bash
GET http://localhost:8081/api/health
```

#### Hello World
```bash
GET http://localhost:8081/api/hello?name=张三
```

#### Echo 测试
```bash
POST http://localhost:8081/api/echo
Content-Type: application/json

{
  "message": "测试消息",
  "timestamp": 1234567890
}
```

#### 系统信息
```bash
GET http://localhost:8081/api/system/info
```

### 2. 认证 API (JWT)

#### 登录获取Token
```bash
POST http://localhost:8081/api/auth/login?username=admin&password=123456
```

返回示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin"
  }
}
```

#### 验证Token
```bash
GET http://localhost:8081/api/auth/validate
Authorization: Bearer {token}
```

#### 刷新Token
```bash
POST http://localhost:8081/api/auth/refresh
Authorization: Bearer {token}
```

### 3. 缓存 API (Redis)

#### 设置缓存
```bash
# 永久缓存
POST http://localhost:8081/api/cache/set?key=test_key&value=test_value

# 带过期时间的缓存（60秒）
POST http://localhost:8081/api/cache/set?key=test_key&value=test_value&ttl=60
```

#### 获取缓存
```bash
GET http://localhost:8081/api/cache/get?key=test_key
```

#### 删除缓存
```bash
DELETE http://localhost:8081/api/cache/delete?key=test_key
```

#### 设置Hash缓存
```bash
POST http://localhost:8081/api/cache/hash/set?key=user_info
Content-Type: application/json

{
  "name": "张三",
  "age": 25,
  "city": "北京"
}
```

#### 获取Hash缓存
```bash
GET http://localhost:8081/api/cache/hash/get?key=user_info
```

#### 检查缓存是否存在
```bash
GET http://localhost:8081/api/cache/exists?key=test_key
```

### 4. 数据库 API (MySQL + MyBatis Plus)

#### 获取所有用户
```bash
GET http://localhost:8081/api/users
```

#### 根据ID获取用户
```bash
GET http://localhost:8081/api/users/1
```

#### 分页查询用户
```bash
GET http://localhost:8081/api/users/page?current=1&size=10
```

#### 根据用户名查询
```bash
GET http://localhost:8081/api/users/username/admin
```

#### 搜索用户
```bash
GET http://localhost:8081/api/users/search?keyword=test
```

#### 创建用户
```bash
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "nickname": "新用户",
  "email": "newuser@example.com",
  "phone": "13900139000",
  "status": 1
}
```

#### 更新用户
```bash
PUT http://localhost:8081/api/users/1
Content-Type: application/json

{
  "nickname": "更新后的昵称",
  "email": "updated@example.com"
}
```

#### 删除用户
```bash
DELETE http://localhost:8081/api/users/1
```

#### 获取用户统计
```bash
GET http://localhost:8081/api/users/stats
```

#### 获取所有文章
```bash
GET http://localhost:8081/api/articles
```

#### 根据ID获取文章
```bash
GET http://localhost:8081/api/articles/1
```

#### 分页查询文章
```bash
# 基础分页
GET http://localhost:8081/api/articles/page?current=1&size=10

# 按分类筛选
GET http://localhost:8081/api/articles/page?current=1&size=10&category=技术教程

# 按状态筛选（0-草稿，1-已发布）
GET http://localhost:8081/api/articles/page?current=1&size=10&status=1
```

#### 根据作者查询文章
```bash
GET http://localhost:8081/api/articles/author/1
```

#### 搜索文章
```bash
GET http://localhost:8081/api/articles/search?keyword=Spring
```

#### 创建文章
```bash
POST http://localhost:8081/api/articles
Content-Type: application/json

{
  "title": "新文章标题",
  "content": "文章内容...",
  "summary": "文章摘要",
  "authorId": 1,
  "category": "技术教程",
  "tags": "Java,Spring Boot",
  "status": 0
}
```

#### 更新文章
```bash
PUT http://localhost:8081/api/articles/1
Content-Type: application/json

{
  "title": "更新后的标题",
  "content": "更新后的内容"
}
```

#### 发布文章
```bash
POST http://localhost:8081/api/articles/1/publish
```

#### 删除文章
```bash
DELETE http://localhost:8081/api/articles/1
```

#### 获取文章统计
```bash
GET http://localhost:8081/api/articles/stats
```

#### 获取热门文章
```bash
GET http://localhost:8081/api/articles/hot?limit=5
```

### 5. 消息队列 API (RocketMQ - 可选)

#### 发送同步消息
```bash
POST http://localhost:8081/api/message/sync?topic=demo-topic
Content-Type: application/json

{
  "type": "order",
  "orderId": "20231018001",
  "amount": 100.00
}
```

#### 发送异步消息
```bash
POST http://localhost:8081/api/message/async?topic=demo-topic
Content-Type: application/json

{
  "type": "notification",
  "userId": "12345",
  "message": "您有新的订单"
}
```

#### 发送单向消息
```bash
POST http://localhost:8081/api/message/oneway?topic=demo-topic
Content-Type: application/json

{
  "type": "log",
  "content": "系统日志消息"
}
```

#### 发送延迟消息
```bash
POST http://localhost:8081/api/message/delay?topic=demo-topic&delayLevel=3
Content-Type: application/json

{
  "type": "reminder",
  "content": "这是一条延迟消息"
}
```

延迟级别说明：
- 1: 1秒
- 2: 5秒
- 3: 10秒
- 4: 30秒
- 5: 1分钟
- ...更多级别请参考RocketMQ文档

#### 发送带标签的消息
```bash
POST http://localhost:8081/api/message/tagged?topic=demo-topic&tag=important
Content-Type: application/json

{
  "type": "alert",
  "level": "high",
  "message": "重要告警消息"
}
```

#### 批量发送消息
```bash
POST http://localhost:8081/api/message/batch?topic=demo-topic
```

## 测试流程

### 完整测试流程示例

1. **测试健康检查**
```bash
curl http://localhost:8081/api/health
```

2. **测试JWT认证**
```bash
# 登录获取token
TOKEN=$(curl -X POST 'http://localhost:8081/api/auth/login?username=admin&password=123456' | jq -r '.data.token')

# 验证token
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/auth/validate
```

3. **测试缓存功能**
```bash
# 设置缓存
curl -X POST 'http://localhost:8081/api/cache/set?key=test&value=hello&ttl=60'

# 获取缓存
curl 'http://localhost:8081/api/cache/get?key=test'

# 检查缓存
curl 'http://localhost:8081/api/cache/exists?key=test'
```

4. **测试消息队列**
```bash
# 发送同步消息
curl -X POST http://localhost:8081/api/message/sync?topic=demo-topic \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"001","amount":100}'

# 发送异步消息
curl -X POST http://localhost:8081/api/message/async?topic=demo-topic \
  -H 'Content-Type: application/json' \
  -d '{"userId":"12345","message":"测试消息"}'
```

## 常见问题

### 1. Redis连接失败
确保Redis服务已启动：
```bash
redis-server
# 或
docker run -d -p 6379:6379 redis
```

### 2. RocketMQ连接失败
确保RocketMQ服务已启动：
```bash
# 启动NameServer
sh bin/mqnamesrv

# 启动Broker
sh bin/mqbroker -n localhost:9876
```

### 3. 数据库连接失败
修改 `application.yml` 中的数据库配置，或者注释掉数据库相关配置（如果不需要测试数据库功能）

## 性能测试

使用 Apache Bench 进行简单的性能测试：

```bash
# 测试健康检查接口
ab -n 1000 -c 10 http://localhost:8081/api/health

# 测试缓存读取
ab -n 1000 -c 10 http://localhost:8081/api/cache/get?key=test
```

## 监控和指标

访问 Actuator 端点查看应用状态：

- 健康检查: http://localhost:8081/actuator/health
- 应用信息: http://localhost:8081/actuator/info
- 指标数据: http://localhost:8081/actuator/metrics
- Prometheus: http://localhost:8081/actuator/prometheus

## 开发建议

1. 根据实际需求修改 `application.yml` 配置
2. 添加更多业务相关的演示API
3. 集成Swagger文档（可选）
4. 添加单元测试和集成测试
5. 配置日志输出到文件

## 许可证

本项目采用 MIT 许可证
