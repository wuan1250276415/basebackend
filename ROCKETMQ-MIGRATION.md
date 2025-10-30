# RabbitMQ → RocketMQ 迁移指南

## 迁移概述

本文档记录了从 RabbitMQ 完全迁移到 RocketMQ 的过程和注意事项。

**迁移日期**: 2025-10-30
**迁移类型**: 完全替换（一次性切换）
**RocketMQ 版本**: 5.1.4
**Spring Boot RocketMQ Starter**: 2.3.0

---

## 一、环境部署

### 1.1 Docker 部署 RocketMQ

项目根目录下的 `docker-compose-rocketmq.yml` 提供了完整的 RocketMQ 部署方案：

```bash
# 启动 RocketMQ
docker-compose -f docker-compose-rocketmq.yml up -d

# 查看日志
docker-compose -f docker-compose-rocketmq.yml logs -f

# 停止服务
docker-compose -f docker-compose-rocketmq.yml down
```

**服务列表**:
- **NameServer**: `localhost:9876`
- **Broker**: `localhost:10911` (VIP端口: 10909)
- **Dashboard**: `http://localhost:8080`

### 1.2 验证部署

访问 Dashboard: `http://localhost:8080`

检查：
- ✅ Cluster 状态正常
- ✅ Broker 在线
- ✅ NameServer 连接正常

---

## 二、核心变更

### 2.1 依赖变更

**移除**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**添加**:
```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2.2 配置变更

**配置文件**: `application-messaging.yml`

| RabbitMQ | RocketMQ |
|----------|----------|
| `spring.rabbitmq.host` | `rocketmq.name-server` |
| `spring.rabbitmq.port` | （包含在 name-server 中） |
| `exchange` | `topic` |
| `routingKey` | `tag` |
| `queue` | `consumerGroup` |

**RocketMQ 配置示例**:
```yaml
rocketmq:
  name-server: 192.168.66.31:9876
  producer:
    group: basebackend-producer-group
    send-message-timeout: 3000
  consumer:
    group: basebackend-consumer-group
    consume-thread-max: 20
```

### 2.3 概念映射

| RabbitMQ | RocketMQ | 说明 |
|----------|----------|------|
| Exchange | Topic | 消息主题 |
| RoutingKey | Tag | 消息标签（用于过滤） |
| Queue | ConsumerGroup | 消费者组 |
| Virtual Host | - | RocketMQ 无此概念 |
| Binding | - | RocketMQ 通过 Topic+Tag 订阅 |

---

## 三、代码变更

### 3.1 核心组件

**新增文件**:
1. `RocketMQConfig.java` - 配置类
2. `RocketMQProducer.java` - 生产者实现
3. `BaseRocketMQConsumer.java` - 消费者基类
4. `DeadLetterConsumer.java` - 死信消费者
5. `RocketMQConstants.java` - 常量定义

**删除文件**:
1. `RabbitMQConfig.java`
2. `RabbitMQProducer.java`

**修改文件**:
1. `MessagingProperties.java` - 配置属性
2. `DeadLetterService.java` - 死信处理

### 3.2 消息生产示例

**发送普通消息**:
```java
@Autowired
private MessageProducer messageProducer;

Message<OrderDTO> message = Message.<OrderDTO>builder()
        .messageId(IdUtil.fastSimpleUUID())
        .topic("ORDER_TOPIC")
        .tags("ORDER_CREATE")  // 原 routingKey
        .messageType("ORDER_CREATED")
        .payload(orderDTO)
        .build();

messageProducer.send(message);
```

**发送延迟消息**:
```java
// RocketMQ 延迟级别：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
messageProducer.sendDelay(message, 60000); // 1分钟延迟
```

**发送顺序消息**:
```java
messageProducer.sendOrdered(message, orderId); // 相同 orderId 保证顺序
```

### 3.3 消息消费示例

**创建消费者**:
```java
@Slf4j
@Component
@RocketMQMessageListener(
        topic = "ORDER_TOPIC",
        consumerGroup = "order-consumer-group",
        selectorExpression = "ORDER_CREATE || ORDER_UPDATE"  // Tag 过滤
)
public class OrderConsumer extends BaseRocketMQConsumer<OrderDTO> {

    @Autowired
    private OrderService orderService;

    @Override
    protected MessageHandler<OrderDTO> getMessageHandler() {
        return message -> {
            log.info("处理订单消息: {}", message.getPayload());
            orderService.handleOrder(message.getPayload());
        };
    }

    @Override
    protected Class<OrderDTO> getPayloadClass() {
        return OrderDTO.class;
    }
}
```

---

## 四、功能对比

| 功能 | RabbitMQ 方案 | RocketMQ 方案 | 状态 |
|------|--------------|--------------|------|
| 普通消息 | `rabbitTemplate.send()` | `rocketMQTemplate.syncSend()` | ✅ 已实现 |
| 延迟消息 | 延迟插件 | 原生支持（18 个延迟级别） | ✅ 已实现 |
| 顺序消息 | 路由键 + 单线程 | `syncSendOrderly()` | ✅ 已实现 |
| 事务消息 | 本地消息表 | 本地消息表（保留原方案） | ✅ 已实现 |
| 死信队列 | DLX/DLQ | 重试主题 + 死信表 | ✅ 已实现 |
| 消息重试 | RetryTemplate | RocketMQ 原生（最多 16 次） | ✅ 已实现 |
| 幂等性 | Redis | Redis（逻辑不变） | ✅ 已实现 |

---

## 五、延迟消息差异

### 5.1 RabbitMQ（插件方式）

- 支持任意时间延迟（毫秒级精度）
- 需要安装 `rabbitmq_delayed_message_exchange` 插件

### 5.2 RocketMQ（固定级别）

RocketMQ 只支持 **18 个固定延迟级别**：

| 级别 | 延迟时间 | 级别 | 延迟时间 |
|------|---------|------|---------|
| 1 | 1s | 10 | 6m |
| 2 | 5s | 11 | 7m |
| 3 | 10s | 12 | 8m |
| 4 | 30s | 13 | 9m |
| 5 | 1m | 14 | 10m |
| 6 | 2m | 15 | 20m |
| 7 | 3m | 16 | 30m |
| 8 | 4m | 17 | 1h |
| 9 | 5m | 18 | 2h |

**自动映射**: `RocketMQConstants.getDelayLevel(delayMillis)` 会自动将任意延迟时间映射到最接近的延迟级别。

---

## 六、死信处理

### 6.1 流程

1. **消费失败** → 抛出异常
2. **自动重试** → RocketMQ 最多重试 16 次
3. **进入死信主题** → `%DLQ%Topic`
4. **死信消费者监听** → `DeadLetterConsumer`
5. **持久化到数据库** → `sys_dead_letter` 表
6. **手动重投** → 通过管理接口

### 6.2 死信重投

```bash
# API 调用
POST /admin/messaging/dead-letter/{id}/redeliver

# 逻辑
1. 从数据库读取死信消息
2. 重新构造 Message 对象
3. 使用 messageProducer.send() 发送到原始 Topic
4. 更新死信状态为 REDELIVERED
```

---

## 七、监控和管理

### 7.1 RocketMQ Dashboard

访问: `http://localhost:8080`

功能：
- **Cluster** - 查看集群状态
- **Topic** - 管理 Topic
- **Consumer** - 查看消费进度
- **Message** - 消息查询和追踪
- **Dashboard** - 监控指标

### 7.2 关键指标

- **消息堆积（Message Accumulation）**: Consumer Lag
- **消费 TPS**: Consume TPS
- **发送 TPS**: Produce TPS
- **消费延迟**: Consume RT

---

## 八、注意事项

### 8.1 延迟消息限制

❌ **不支持**: 任意时间延迟（如 2小时1分钟）
✅ **支持**: 18 个固定延迟级别

**解决方案**:
- 使用定时任务补充
- 接受最接近的延迟级别

### 8.2 消息顺序性

RocketMQ 的顺序消息是 **队列级别** 的顺序：
- 同一 `hashKey` 的消息发送到同一队列
- 队列内消息按顺序消费
- 不同队列之间无顺序保证

### 8.3 事务消息

本次迁移 **保留了本地消息表方案**，未使用 RocketMQ 原生事务消息：
- 原因：保持现有稳定性，降低迁移风险
- 优点：逻辑不变，业务无感知
- 未来：可逐步迁移到 RocketMQ 事务消息

### 8.4 配置检查

❌ **已移除配置**:
```yaml
spring:
  rabbitmq:  # 整个配置块已移除
```

✅ **新增配置**:
```yaml
rocketmq:
  name-server: 192.168.66.31:9876  # 必填
```

---

## 九、迁移检查清单

### 9.1 部署前

- [ ] 启动 RocketMQ 服务
- [ ] 验证 Dashboard 可访问
- [ ] 检查 NameServer 和 Broker 状态

### 9.2 代码检查

- [ ] 所有 Producer 使用 `MessageProducer` 接口
- [ ] 所有 Consumer 继承 `BaseRocketMQConsumer`
- [ ] 移除 RabbitMQ 相关 import
- [ ] 死信处理逻辑已适配

### 9.3 配置检查

- [ ] `application-messaging.yml` 已更新
- [ ] `rocketmq.name-server` 配置正确
- [ ] 移除 `spring.rabbitmq` 配置

### 9.4 功能测试

- [ ] 普通消息发送和消费
- [ ] 延迟消息发送和消费
- [ ] 顺序消息发送和消费
- [ ] 事务消息发送
- [ ] 死信处理和重投
- [ ] 幂等性验证

---

## 十、回滚方案

如果需要回滚到 RabbitMQ：

1. **恢复 POM 依赖**
   - 移除 `rocketmq-spring-boot-starter`
   - 添加 `spring-boot-starter-amqp`

2. **恢复配置文件**
   - 从 Git 历史恢复 `application-messaging.yml`

3. **恢复代码**
   - 从 Git 历史恢复 `RabbitMQConfig.java`
   - 从 Git 历史恢复 `RabbitMQProducer.java`
   - 移除 RocketMQ 相关文件

4. **重启服务**

**建议**: 在生产环境迁移前，在测试环境充分验证。

---

## 十一、性能对比

| 指标 | RabbitMQ | RocketMQ |
|------|----------|----------|
| 单机 TPS | 1万+ | 10万+ |
| 平均延迟 | 5-10ms | 1-5ms |
| 消息堆积能力 | GB级 | TB级 |
| 集群扩展 | 复杂 | 简单 |

---

## 十二、常见问题

### Q1: 延迟消息不精确？

**A**: RocketMQ 只支持 18 个固定延迟级别，系统会自动选择最接近的级别。如需精确延迟，可使用定时任务。

### Q2: 死信消息没有进入 DLQ？

**A**: 检查以下几点：
1. 消费者是否抛出异常
2. 是否达到最大重试次数（16次）
3. `DeadLetterConsumer` 是否启动

### Q3: 顺序消息乱序？

**A**: 确保：
1. 使用 `sendOrdered()` 发送
2. 同一业务键使用相同的 `hashKey`
3. 消费者使用 `ConsumeMode.ORDERLY`

### Q4: 如何查看消息轨迹？

**A**: 在 RocketMQ Dashboard 的 "Message" 页面，输入 MessageId 查询。

---

## 联系方式

如有问题，请联系：
- **开发团队**: dev@basebackend.com
- **技术文档**: https://docs.basebackend.com

---

**文档版本**: 1.0
**最后更新**: 2025-10-30
