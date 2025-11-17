# API网关快速入门

## 5分钟快速开始

本指南帮助您快速启动和测试API网关的核心功能。

## 前置条件

确保以下服务已启动：
- Nacos（端口8848）
- Redis（端口6379）
- 至少一个后端服务（如admin-api）

## 步骤1: 启动网关

```bash
cd /home/wuan/IdeaProjects/basebackend

# 启动网关
java -jar basebackend-gateway/target/basebackend-gateway-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=gateway
```

## 步骤2: 验证网关运行

```bash
# 检查健康状态
curl http://localhost:8080/actuator/health

# 预期输出
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

## 步骤3: 测试路由功能

```bash
# 通过网关访问admin-api服务
curl http://localhost:8080/admin-api/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

## 步骤4: 测试限流功能

### 4.1 测试IP限流

```bash
# 快速发送30个请求（超过IP限流阈值20）
for i in {1..30}; do
  curl http://localhost:8080/admin-api/api/users
done

# 预期输出（前20个成功，后10个被限流）
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "success": false,
  "timestamp": 1698765432000
}
```

### 4.2 测试接口限流

```bash
# 快速发送15个登录请求（超过登录接口限流阈值10）
for i in {1..15}; do
  curl http://localhost:8080/admin-api/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}'
done
```

## 步骤5: 测试灰度路由

### 5.1 准备灰度环境

在Nacos中注册两个版本的服务实例：

**稳定版本服务实例**：
```yaml
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          version: v1.0.0
```

**灰度版本服务实例**：
```yaml
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          version: v2.0.0
```

### 5.2 测试Header灰度

```bash
# 访问稳定版本
curl http://localhost:8080/basebackend-demo-api/api/users

# 访问灰度版本
curl -H "X-Gray-Flag: true" \
  http://localhost:8080/basebackend-demo-api/api/users
```

### 5.3 测试权重灰度

```bash
# 发送100个请求，观察灰度版本流量占比
for i in {1..100}; do
  curl http://localhost:8080/admin-api/api/users
done

# 预期：约10%的请求路由到v2.0.0版本
```

## 步骤6: 测试重试机制

### 6.1 模拟服务故障

```bash
# 临时停止后端服务，模拟502错误
# 网关会自动重试3次

curl -v http://localhost:8080/admin-api/api/users

# 观察日志，可以看到重试记录
```

### 6.2 查看重试日志

```bash
# 网关日志会显示
2025-10-21 11:00:01.123 DEBUG [gateway] - Retry attempt 1 for /admin-api/api/users
2025-10-21 11:00:01.133 DEBUG [gateway] - Retry attempt 2 for /admin-api/api/users
2025-10-21 11:00:01.153 DEBUG [gateway] - Retry attempt 3 for /admin-api/api/users
```

## 步骤7: 测试动态路由

### 7.1 查看当前路由

```bash
curl http://localhost:8080/actuator/gateway/routes | jq
```

### 7.2 添加新路由

```bash
curl -X POST http://localhost:8080/actuator/gateway/routes \
  -H "Content-Type: application/json" \
  -d '{
    "id": "test-route",
    "uri": "lb://test-service",
    "predicates": [
      {
        "name": "Path",
        "args": {"pattern": "/test/**"}
      }
    ],
    "filters": [
      {
        "name": "StripPrefix",
        "args": {"parts": "1"}
      }
    ],
    "order": 0
  }'

# 预期输出
{
  "success": true,
  "message": "success",
  "routeId": "test-route"
}
```

### 7.3 验证新路由

```bash
# 查看路由是否添加成功
curl http://localhost:8080/actuator/gateway/routes | jq '.[] | select(.id=="test-route")'
```

### 7.4 删除路由

```bash
curl -X DELETE http://localhost:8080/actuator/gateway/routes/test-route

# 预期输出
{
  "success": true,
  "message": "success",
  "routeId": "test-route"
}
```

## 步骤8: 集成Sentinel控制台

### 8.1 启动Sentinel控制台

```bash
# 下载Sentinel控制台（如果未下载）
wget https://github.com/alibaba/Sentinel/releases/download/1.8.6/sentinel-dashboard-1.8.6.jar

# 启动控制台
java -jar sentinel-dashboard-1.8.6.jar --server.port=8858
```

### 8.2 访问控制台

打开浏览器访问：http://localhost:8858

- 用户名：sentinel
- 密码：sentinel

### 8.3 查看网关监控

在控制台中可以看到：
- 实时QPS监控
- 限流规则配置
- 熔断规则配置
- 流量详情

## 常用配置修改

### 修改限流阈值

编辑 `RateLimitRuleManager.java`:

```java
// 修改IP限流阈值为50
GatewayFlowRule ipLimitRule = new GatewayFlowRule("admin-api")
    .setCount(50)  // 改为50
    .setIntervalSec(1)
    .setParamItem(new GatewayParamFlowItem()
        .setParseStrategy(SentinelGatewayConstants.PARAM_PARSE_STRATEGY_CLIENT_IP));
```

### 修改灰度权重

编辑 `application-gateway.yml`:

```yaml
gateway:
  gray:
    rules:
      - serviceName: admin-api
        weight: 20  # 改为20%流量到灰度版本
```

### 修改重试次数

编辑 `application-gateway.yml`:

```yaml
filters:
  - name: Retry
    args:
      retries: 5  # 改为最多重试5次
```

## 故障排查

### 问题1: 网关启动失败

```bash
# 检查端口是否被占用
netstat -tlnp | grep 8080

# 检查Nacos是否启动
curl http://localhost:8848/nacos/

# 检查Redis是否启动
redis-cli ping
```

### 问题2: 路由不通

```bash
# 检查后端服务是否注册到Nacos
curl http://localhost:8848/nacos/v1/ns/instance/list?serviceName=admin-api

# 检查路由配置
curl http://localhost:8080/actuator/gateway/routes | jq
```

### 问题3: 限流不生效

```bash
# 检查Sentinel规则是否加载
curl http://localhost:8080/actuator/sentinel

# 查看网关日志
tail -f logs/gateway.log | grep Sentinel
```

## 下一步

- 阅读完整文档：[GATEWAY-FEATURES.md](./GATEWAY-FEATURES.md)
- 配置自定义限流规则
- 集成JWT鉴权
- 配置灰度发布策略
- 接入监控告警系统

## 快速命令参考

```bash
# 启动网关
java -jar basebackend-gateway/target/basebackend-gateway-1.0.0-SNAPSHOT.jar --spring.profiles.active=gateway

# 健康检查
curl http://localhost:8080/actuator/health

# 查看路由
curl http://localhost:8080/actuator/gateway/routes

# 刷新路由
curl -X POST http://localhost:8080/actuator/gateway/refresh

# 查看限流规则
curl http://localhost:8080/actuator/sentinel
```

## 性能测试

使用Apache Bench进行压力测试：

```bash
# 测试网关吞吐量
ab -n 10000 -c 100 http://localhost:8080/admin-api/api/health

# 测试限流效果
ab -n 1000 -c 50 http://localhost:8080/admin-api/api/users

# 观察限流拦截率
# 预期：超过限流阈值的请求返回429
```

## 总结

完成以上步骤后，您已经成功：

✅ 启动并验证网关运行
✅ 测试路由转发功能
✅ 验证多维度限流
✅ 体验灰度路由
✅ 测试重试机制
✅ 使用动态路由API
✅ 集成Sentinel监控

现在您可以开始在生产环境中使用API网关了！
