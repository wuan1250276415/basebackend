[根目录](../../CLAUDE.md) > **basebackend-messaging**

# basebackend-messaging

## 模块职责

消息基础设施库。封装RocketMQ集成，提供事务消息、幂等消费、死信队列、Webhook推送等企业级消息能力。

## 对外接口

- `RocketMQProducer`: 消息生产者
- `TransactionalMessageService`: 事务消息服务
- `IdempotencyService`: 幂等性服务
- `WebhookInvoker`: Webhook调用器
- `WebhookSignatureService`: Webhook签名验证

## 关键依赖

- RocketMQ Spring Boot Starter 2.3.0
- RocketMQ Client 5.2.0

## 测试与质量

5个测试: WebhookSignatureServiceTest, WebhookInvokerTest, IdempotencyServiceTest, RocketMQProducerTest, TransactionalMessageServiceTest

## 变更记录

| 时间 | 操作 | 说明 |
|------|------|------|
| 2026-02-20 13:17:55 | 初始创建 | 全量扫描生成 |
