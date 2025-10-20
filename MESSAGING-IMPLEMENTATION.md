# 消息与集成功能实现文档

## 项目概述

本文档记录了BaseBackend项目中"消息与集成"功能的完整实现过程，包括消息队列（事件总线、异步解耦）、Webhook/回调框架、幂等与有序性保障等核心功能。

## 技术选型

### 消息队列
- **RabbitMQ 3.12**：成熟稳定的消息中间件，支持多种消息模式
- **Spring AMQP**：Spring官方RabbitMQ客户端

### 功能特性
- ✅ 延迟消息（基于rabbitmq_delayed_message_exchange插件）
- ✅ 事务消息（本地消息表方案）
- ✅ 消息重试（指数退避策略）
- ✅ 死信队列（DLQ处理）
- ✅ 幂等性保障（基于Redis）
- ✅ 顺序消息（单线程消费器）

### Webhook特性
- ✅ HMAC-SHA256签名验证
- ✅ 异步调用（基于消息队列）
- ✅ 重试机制（指数退避）
- ✅ 事件订阅（灵活配置）
- ✅ 调用日志（完整记录）

### 前端技术
- **React 18 + TypeScript**
- **Ant Design 5**
- **React Router 6**

## 架构设计

### 模块结构

```
basebackend-messaging/          # 消息集成基础模块
├── config/                    # 配置类
│   ├── MessagingConfig.java
│   ├── MessagingProperties.java
│   ├── RabbitMQConfig.java
│   └── WebhookConfig.java
├── model/                     # 数据模型
│   ├── Message.java
│   └── MessageStatus.java
├── producer/                  # 消息生产者
│   ├── MessageProducer.java
│   └── RabbitMQProducer.java
├── consumer/                  # 消息消费者
│   └── MessageConsumer.java
├── handler/                   # 消息处理器
│   └── MessageHandler.java
├── idempotency/              # 幂等性
│   └── IdempotencyService.java
├── transaction/              # 事务消息
│   └── TransactionalMessageService.java
├── order/                    # 顺序消息
│   └── OrderedMessageConsumer.java
├── webhook/                  # Webhook框架
│   ├── WebhookEvent.java
│   ├── WebhookConfig.java
│   ├── WebhookLog.java
│   ├── WebhookSignatureService.java
│   └── WebhookInvoker.java
├── event/                    # 事件发布
│   └── EventPublisher.java
└── exception/                # 异常类
    ├── MessagingException.java
    ├── MessageSendException.java
    └── MessageConsumeException.java
```

## 实现步骤

### Phase 1: 创建消息集成基础模块

#### 1.1 更新父POM

```xml
<!-- 在properties中添加 -->
<rabbitmq.version>3.1.5</rabbitmq.version>

<!-- 在dependencyManagement中添加 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
    <version>${rabbitmq.version}</version>
</dependency>

<!-- 在modules中添加 -->
<module>basebackend-messaging</module>
```

#### 1.2 创建核心类

**消息模型** (`Message.java`):
- 消息唯一ID
- 主题/路由键
- 消息体
- 延迟时间
- 重试次数
- 事务标记
- 顺序保障

**消息生产者** (`RabbitMQProducer.java`):
- `send()`: 普通消息发送
- `sendDelay()`: 延迟消息发送
- `sendTransactional()`: 事务消息发送
- `sendOrdered()`: 顺序消息发送

**消息处理器** (`MessageHandler.java`):
- 幂等性检查
- 并发处理锁
- 重试逻辑
- 死信处理

#### 1.3 幂等性实现

基于Redis的消息去重：
- 使用消息ID作为唯一标识
- 设置过期时间（默认3600秒）
- 分布式锁防止并发处理

#### 1.4 事务消息实现

基于本地消息表：
- 先保存消息到数据库
- 再发送到RabbitMQ
- 定时扫描超时未确认的消息
- 自动补偿机制

### Phase 2: 实现Webhook框架

#### 2.1 Webhook签名

使用HMAC-SHA256算法：
```java
String signContent = timestamp + "." + payload;
Mac mac = Mac.getInstance("HmacSHA256");
SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
mac.init(secretKeySpec);
byte[] hash = mac.doFinal(signContent.getBytes());
return Base64.getEncoder().encodeToString(hash);
```

#### 2.2 Webhook调用

**同步调用** (`invoke()`):
- 构建HTTP请求
- 添加签名头
- 发送请求
- 记录日志

**异步调用** (`invokeAsync()`):
- 发送到消息队列
- 异步处理
- 失败重试

#### 2.3 重试策略

指数退避算法：
```
delaySeconds = retryInterval * (2 ^ (retryCount - 1))
```

### Phase 3: Admin-API集成

#### 3.1 数据库表设计

创建了6张表：

**sys_message_log**: 消息日志表
- 记录所有消息的发送和消费状态
- 支持事务消息的补偿查询

**sys_webhook_config**: Webhook配置表
- 存储Webhook订阅配置
- 支持事件类型过滤
- 可配置重试策略

**sys_webhook_log**: Webhook调用日志表
- 记录每次Webhook调用
- 包含请求/响应详情
- 支持故障排查

**sys_event_subscription**: 事件订阅表
- 管理Webhook和事件的订阅关系
- 支持灵活的事件路由

**sys_dead_letter**: 死信表
- 存储处理失败的消息
- 支持重新投递
- 支持人工处理

**sys_message_queue_monitor**: 队列监控表
- 记录队列状态
- 支持趋势分析

#### 3.2 核心Service实现

**WebhookConfigService**:
- CRUD操作
- 启用/禁用控制

**WebhookLogService**:
- 日志查询
- 条件过滤

**DeadLetterService**:
- 死信查询
- 重新投递
- 批量处理

**MessageMonitorService**:
- 统计查询
- 队列监控

#### 3.3 RESTful API设计

所有API遵循统一规范：
- 使用`/api/messaging/*`前缀
- 返回统一的`Result`对象
- 支持分页查询
- 使用标准HTTP方法

### Phase 4: 前端页面实现

#### 4.1 消息监控页面

**功能特性**:
- 实时统计展示（总数、待发送、已消费等）
- 成功率计算
- 队列监控
- 自动刷新（30秒）

**技术要点**:
- 使用Statistic组件展示数据
- 颜色编码（绿色/黄色/红色）
- 轮询更新

#### 4.2 Webhook配置页面

**功能特性**:
- 列表查询和搜索
- 新增/编辑/删除
- 启用/禁用切换
- 表单验证

**配置项**:
- URL和HTTP方法
- 事件类型订阅
- 签名密钥
- 超时和重试设置

#### 4.3 事件日志页面

**功能特性**:
- 多条件查询
- 详情查看
- 事件发布
- JSON格式化展示

**查询条件**:
- Webhook筛选
- 事件类型
- 成功/失败状态
- 时间范围

#### 4.4 死信处理页面

**功能特性**:
- 列表查询
- 详情查看
- 单个重投
- 批量重投
- 丢弃操作

**状态管理**:
- 待处理
- 已重投
- 已丢弃

### Phase 5: Docker部署配置

#### 5.1 Docker Compose配置

```yaml
services:
  rabbitmq:
    image: rabbitmq:3.12-management
    ports:
      - "5672:5672"
      - "15672:15672"
    environment:
      RABBITMQ_DEFAULT_USER: admin
      RABBITMQ_DEFAULT_PASS: admin123
      RABBITMQ_DEFAULT_VHOST: basebackend
```

#### 5.2 插件配置

自动启用插件：
- `rabbitmq_management`: 管理界面
- `rabbitmq_delayed_message_exchange`: 延迟消息

#### 5.3 启动脚本

提供便捷的启动/停止脚本：
- `start.sh`: 启动服务
- `stop.sh`: 停止服务
- 自动检测sudo权限

## 核心功能实现

### 1. 幂等性保障

**实现原理**:
```java
// 检查消息是否已处理
if (idempotencyService.isDuplicate(messageId)) {
    return; // 跳过重复消息
}

// 获取处理锁
if (!idempotencyService.tryLock(messageId)) {
    return; // 其他线程正在处理
}

try {
    // 处理消息
    consumer.consume(message);
    // 标记已处理
    idempotencyService.markAsProcessed(messageId);
} finally {
    idempotencyService.unlock(messageId);
}
```

### 2. 顺序消息

**实现原理**:
```java
// 每个分区键对应一个单线程执行器
ExecutorService executor = executorMap.computeIfAbsent(partitionKey,
    k -> Executors.newSingleThreadExecutor());

// 提交到单线程执行器顺序执行
executor.submit(() -> consumer.consume(message));
```

### 3. 延迟消息

**实现原理**:
```java
// 使用延迟交换机
rabbitTemplate.convertAndSend(delayExchange, routingKey, message, msg -> {
    MessageProperties props = msg.getMessageProperties();
    props.setDelay((int) delayMillis); // 设置延迟时间
    return msg;
});
```

### 4. 事务消息

**实现原理**:
```java
// 1. 保存到本地消息表
transactionalMessageService.saveMessage(message);

// 2. 发送消息
messageProducer.send(message);

// 3. 定时补偿
@Scheduled(fixedDelay = 60000)
public void compensateTimeoutMessages() {
    // 查询超时未确认的消息
    // 重新发送
}
```

## 使用指南

### 1. 启动RabbitMQ

```bash
cd docker/messaging
./start.sh
```

### 2. 配置应用

在`application.yml`中添加：
```yaml
spring:
  profiles:
    active: dev,messaging
```

### 3. 执行数据库脚本

```sql
source basebackend-admin-api/src/main/resources/db/migration/V1.3__create_messaging_tables.sql
```

### 4. 发送消息

```java
@Autowired
private MessageProducer messageProducer;

// 发送普通消息
Message<MyData> message = Message.<MyData>builder()
    .topic("my.topic")
    .routingKey("my.key")
    .payload(myData)
    .build();
messageProducer.send(message);

// 发送延迟消息
messageProducer.sendDelay(message, 60000); // 延迟60秒

// 发送事务消息
messageProducer.sendTransactional(message);

// 发送顺序消息
messageProducer.sendOrdered(message, "userId123");
```

### 5. 消费消息

```java
@RabbitListener(queues = "my.queue")
public void handleMessage(Message<MyData> message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    messageHandler.handle(message, msg -> {
        // 处理业务逻辑
        processData(msg.getPayload());
    }, channel, deliveryTag);
}
```

### 6. 配置Webhook

访问前端页面：`http://localhost:3000/integration/webhook-config`

1. 点击"新增"
2. 填写配置信息
3. 保存并启用

### 7. 发布事件

```java
@Autowired
private EventPublisher eventPublisher;

eventPublisher.publishEvent(
    "user.created",
    userData,
    "admin-api"
);
```

## 性能优化建议

### 1. 消费者并发

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 10
        max-concurrency: 50
```

### 2. 连接池

```yaml
spring:
  rabbitmq:
    cache:
      channel:
        size: 50
```

### 3. 批量操作

使用批量API减少网络开销

### 4. 消息持久化

仅对重要消息启用持久化

## 监控和告警

### 1. RabbitMQ管理界面

访问：http://localhost:15672

监控指标：
- 队列深度
- 消息速率
- 消费者数量
- 内存使用

### 2. 应用监控

通过API获取统计：
```
GET /api/messaging/monitor/statistics
GET /api/messaging/monitor/queue
```

### 3. 告警设置

配置告警规则：
- 消息积压超过阈值
- 死信数量异常
- 消费失败率过高

## 故障排查

### 1. 消息丢失

检查点：
- 发送确认是否启用
- 队列是否持久化
- 消息是否持久化
- 确认机制是否正确

### 2. 消息重复

检查点：
- 幂等性是否启用
- 消息ID是否唯一
- Redis是否正常

### 3. 死信过多

检查点：
- 消费者是否正常
- 错误日志
- 重试次数配置
- 业务逻辑异常

## 总结

本功能实现了完整的企业级消息集成解决方案，包括：

✅ **消息队列**：基于RabbitMQ的可靠消息传递
✅ **Webhook框架**：灵活的事件订阅和回调机制
✅ **幂等性保障**：防止消息重复处理
✅ **顺序消息**：保证消息处理顺序
✅ **事务消息**：保证最终一致性
✅ **监控告警**：完善的监控和管理界面

所有代码已编译通过，功能完整可用。
