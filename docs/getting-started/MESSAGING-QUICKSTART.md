# 消息与集成功能 - 快速开始

## 🚀 5分钟快速开始

### 1. 启动RabbitMQ

```bash
cd docker/messaging
./start.sh
```

等待服务启动完成后，访问管理界面验证：http://localhost:15672
- 用户名：admin
- 密码：admin123

### 2. 初始化数据库

```bash
# 连接到MySQL
mysql -u root -p

# 执行建表脚本
source /path/to/basebackend/basebackend-admin-api/src/main/resources/db/migration/V1.3__create_messaging_tables.sql
```

### 3. 配置应用

编辑 `basebackend-admin-api/src/main/resources/application.yml`：

```yaml
spring:
  profiles:
    active: dev,messaging  # 添加 messaging 配置
```

### 4. 启动后端服务

```bash
cd basebackend-admin-api
mvn spring-boot:run
```

### 5. 启动前端服务

```bash
cd basebackend-admin-web
npm install
npm run dev
```

### 6. 访问功能页面

打开浏览器访问：http://localhost:3000

导航到以下页面：
- 消息监控：`/integration/message-monitor`
- Webhook配置：`/integration/webhook-config`
- 事件日志：`/integration/event-log`
- 死信处理：`/integration/dead-letter`

## 📝 快速示例

### 发送普通消息

```java
@Autowired
private MessageProducer messageProducer;

public void sendMessage() {
    Message<String> message = Message.<String>builder()
        .topic("user.notification")
        .routingKey("email")
        .payload("Hello, World!")
        .build();

    messageProducer.send(message);
}
```

### 发送延迟消息

```java
// 延迟60秒发送
messageProducer.sendDelay(message, 60000);
```

### 配置Webhook

1. 访问 `/integration/webhook-config`
2. 点击"新增"
3. 填写：
   - 名称：Test Webhook
   - URL：https://your-domain.com/webhook
   - 事件类型：user.created,user.updated
   - 启用签名：是
   - 签名密钥：your-secret-key
4. 保存

### 发布事件

```java
@Autowired
private EventPublisher eventPublisher;

public void publishUserCreatedEvent(User user) {
    eventPublisher.publishEvent(
        "user.created",
        user,
        "admin-api"
    );
}
```

Webhook会自动收到以下请求：

```json
POST https://your-domain.com/webhook
Headers:
  X-Webhook-Signature: <HMAC-SHA256签名>
  X-Webhook-Timestamp: <时间戳>
  Content-Type: application/json

Body:
{
  "eventId": "abc123",
  "eventType": "user.created",
  "data": {
    "id": 1,
    "username": "admin"
  },
  "timestamp": "2025-10-21T00:00:00",
  "source": "admin-api"
}
```

## 🔧 常用API

### 消息监控

```bash
# 获取消息统计
curl http://localhost:8082/api/messaging/monitor/statistics

# 获取队列监控
curl http://localhost:8082/api/messaging/monitor/queue
```

### Webhook管理

```bash
# 查询Webhook列表
curl http://localhost:8082/api/messaging/webhook/page?page=1&size=20

# 创建Webhook
curl -X POST http://localhost:8082/api/messaging/webhook \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Webhook",
    "url": "https://example.com/webhook",
    "eventTypes": "user.created",
    "signatureEnabled": true,
    "secret": "your-secret"
  }'
```

### 事件发布

```bash
# 发布事件
curl -X POST http://localhost:8082/api/messaging/event/publish \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "user.created",
    "data": {"userId": 1, "username": "admin"},
    "source": "admin-api"
  }'
```

### 死信处理

```bash
# 查询死信
curl http://localhost:8082/api/messaging/dead-letter/page?page=1&size=20

# 重新投递
curl -X POST http://localhost:8082/api/messaging/dead-letter/1/redeliver

# 批量重投
curl -X POST http://localhost:8082/api/messaging/dead-letter/batch-redeliver \
  -H "Content-Type: application/json" \
  -d '[1,2,3]'
```

## ⚙️ 配置说明

### application-messaging.yml

```yaml
messaging:
  rabbitmq:
    enabled: true                    # 是否启用RabbitMQ
    delay-plugin-enabled: true       # 是否启用延迟消息

  retry:
    max-attempts: 3                  # 最大重试次数
    initial-interval: 1000           # 初始重试间隔(ms)
    multiplier: 2.0                  # 重试间隔倍数

  dead-letter:
    enabled: true                    # 是否启用死信队列

  transaction:
    enabled: true                    # 是否启用事务消息
    check-interval: 60               # 补偿检查间隔(秒)

  idempotency:
    enabled: true                    # 是否启用幂等性
    expire-time: 3600                # 缓存过期时间(秒)
```

## 📊 监控指标

### RabbitMQ管理界面

访问：http://localhost:15672

关键指标：
- **队列深度**：消息积压数量
- **消息速率**：每秒处理消息数
- **消费者数量**：活跃消费者
- **内存使用**：服务器内存

### 应用监控

访问：http://localhost:3000/integration/message-monitor

展示指标：
- 消息总数
- 待发送/已发送/已消费
- 失败/死信数量
- 成功率

## 🐛 故障排查

### 问题1：消息发送失败

**症状**：日志显示"Failed to send message"

**解决方案**：
1. 检查RabbitMQ是否启动：`docker ps | grep rabbitmq`
2. 检查网络连接：`telnet localhost 5672`
3. 查看RabbitMQ日志：`docker logs basebackend-rabbitmq`

### 问题2：Webhook调用失败

**症状**：事件日志显示调用失败

**解决方案**：
1. 检查目标URL是否可访问
2. 查看错误信息：在事件日志详情中查看
3. 检查签名验证：确认密钥配置正确

### 问题3：死信过多

**症状**：死信队列消息堆积

**解决方案**：
1. 查看死信详情：访问 `/integration/dead-letter`
2. 检查错误原因
3. 修复问题后批量重投

## 📚 更多资源

- [完整实现文档](./MESSAGING-IMPLEMENTATION.md)
- [RabbitMQ部署指南](./docker/messaging/README.md)
- [API文档](http://localhost:8082/doc.html)

## 💡 最佳实践

### 1. 消息设计

- 使用有意义的主题名称：`domain.action`
- 消息体保持精简
- 使用JSON格式

### 2. 错误处理

- 合理设置重试次数
- 记录详细错误日志
- 及时处理死信

### 3. 性能优化

- 调整消费者并发数
- 使用批量操作
- 监控队列深度

### 4. 安全性

- 启用Webhook签名验证
- 使用HTTPS传输
- 定期更换密钥

## ✅ 功能清单

- [x] 消息队列（RabbitMQ）
- [x] 延迟消息
- [x] 事务消息
- [x] 消息重试
- [x] 死信队列
- [x] 幂等性保障
- [x] 顺序消息
- [x] Webhook框架
- [x] 签名验证
- [x] 事件订阅
- [x] 调用日志
- [x] 消息监控
- [x] 前端管理页面

## 🎉 开始使用吧！

现在你已经掌握了所有必要的知识，可以开始使用消息与集成功能了。

祝你使用愉快！ 🚀
