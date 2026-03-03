# 商城事件字段级契约（Schema Contract）

> 版本：`v1.0`  
> 更新时间：`2026-03-03`  
> 适用范围：商城核心链路（交易 / 支付 / 商品）在 RocketMQ 上传递的业务事件

## 1. 范围说明

- 本文档聚焦当前已落地、已联调的事件 Topic 与消息体字段契约。
- 事件命名与全量 Topic 清单参考：`docs/mall/mall-event-topic-conventions.md`。
- 本文档不包含预留但暂未实际投递的事件（如退款类事件）。

## 2. 通用消息 Envelope 契约

所有业务事件统一使用 `com.basebackend.messaging.model.Message<T>` 封装。

### 2.1 Envelope 字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `messageId` | `String` | 是 | 消息唯一标识，生产端使用 UUID |
| `topic` | `String` | 是 | RocketMQ Topic |
| `tags` | `String` | 是 | RocketMQ Tag；当前与 `messageType` 保持一致 |
| `messageType` | `String` | 是 | 事件类型码（如 `ORDER_CREATED`） |
| `payload` | `Object` | 是 | 业务消息体，结构见后续 Topic 章节 |
| `timestamp` | `LocalDateTime` | 是 | 事件发送时间 |
| `routingKey` | `String` | 否 | RabbitMQ 路由键；RocketMQ 场景通常为空 |
| `headers` | `Map<String,Object>` | 否 | 扩展头；当前商城链路未使用 |
| `sendTime` | `LocalDateTime` | 否 | 扩展时间字段；当前商城链路未使用 |
| `delayMillis`/`retryCount`/`maxRetries`/`partitionKey`/`transactional`/`source`/`traceId` | 多类型 | 否 | 预留扩展字段，当前商城链路未设置 |

### 2.2 Envelope 示例

```json
{
  "messageId": "7f7c2fcb-b7f9-4d8f-a8ac-9f0b6b6e0f4c",
  "topic": "mall.trade.order-created",
  "tags": "ORDER_CREATED",
  "messageType": "ORDER_CREATED",
  "payload": {
    "...": "..."
  },
  "timestamp": "2026-03-03T15:00:00"
}
```

## 3. 状态枚举字典

### 3.1 订单状态 `orderStatus`

- `CREATED`：已创建
- `PAID`：已支付
- `CANCELLED`：已取消
- `TIMEOUT_CLOSED`：超时关闭

### 3.2 订单支付状态 `orderPayStatus`

- `UNPAID`：未支付
- `PAID`：已支付
- `PAY_FAILED`：支付失败
- `CLOSED`：已关闭

### 3.3 支付单状态 `paymentStatus`

- `WAIT_PAY`：待支付
- `PAY_SUCCESS`：支付成功
- `PAY_FAILED`：支付失败
- `CLOSED`：已关闭

## 4. Topic -> Payload 字段契约

## 4.1 `mall.trade.order-created`

- `messageType`：`ORDER_CREATED`
- 生产者：`basebackend-mall-trade-api`
- 消费者：`basebackend-mall-pay-api`、`basebackend-mall-product-api`
- Payload：`OrderCreatedMessage`

| 字段 | 类型 | 必填 | 说明 | 枚举/固定值 |
| --- | --- | --- | --- | --- |
| `orderId` | `Long` | 是 | 订单 ID | - |
| `orderNo` | `String` | 是 | 订单号 | - |
| `userId` | `Long` | 是 | 用户 ID | - |
| `payAmount` | `BigDecimal` | 是 | 订单应付金额 | - |
| `orderStatus` | `Enum` | 是 | 订单状态 | `CREATED/PAID/CANCELLED/TIMEOUT_CLOSED`（当前生产固定 `CREATED`） |
| `orderPayStatus` | `Enum` | 是 | 订单支付状态 | `UNPAID/PAID/PAY_FAILED/CLOSED`（当前生产固定 `UNPAID`） |
| `items` | `List<OrderItem>` | 是 | 下单商品列表 | - |
| `items[].skuId` | `Long` | 是 | SKU ID | - |
| `items[].quantity` | `Integer` | 是 | 购买数量 | > 0 |

## 4.2 `mall.trade.order-cancelled`

- `messageType`：`ORDER_CANCELLED`
- 生产者：`basebackend-mall-trade-api`
- 消费者：`basebackend-mall-product-api`
- Payload：`OrderCancelledMessage`

| 字段 | 类型 | 必填 | 说明 | 枚举/固定值 |
| --- | --- | --- | --- | --- |
| `orderId` | `Long` | 是 | 订单 ID | - |
| `orderNo` | `String` | 是 | 订单号 | - |
| `reason` | `String` | 是 | 取消原因 | 当前常见：`PAYMENT_FAILED` 或支付失败原因透传 |
| `orderStatus` | `Enum` | 是 | 订单状态 | `CREATED/PAID/CANCELLED/TIMEOUT_CLOSED`（当前生产固定 `CANCELLED`） |
| `orderPayStatus` | `Enum` | 是 | 订单支付状态 | `UNPAID/PAID/PAY_FAILED/CLOSED`（当前生产固定 `PAY_FAILED`） |
| `items` | `List<OrderItemSnapshot>` | 是 | 订单商品快照 | - |
| `items[].skuId` | `Long` | 是 | SKU ID | - |
| `items[].quantity` | `Integer` | 是 | 数量 | > 0 |

## 4.3 `mall.trade.order-timeout-closed`

- `messageType`：`ORDER_TIMEOUT_CLOSED`
- 生产者：`basebackend-mall-trade-api`
- 消费者：`basebackend-mall-pay-api`、`basebackend-mall-product-api`
- Payload：`OrderTimeoutClosedMessage`

| 字段 | 类型 | 必填 | 说明 | 枚举/固定值 |
| --- | --- | --- | --- | --- |
| `orderId` | `Long` | 是 | 订单 ID | - |
| `orderNo` | `String` | 是 | 订单号 | - |
| `reason` | `String` | 是 | 关单原因 | 当前生产固定 `ORDER_TIMEOUT` |
| `orderStatus` | `Enum` | 是 | 订单状态 | `CREATED/PAID/CANCELLED/TIMEOUT_CLOSED`（当前生产固定 `TIMEOUT_CLOSED`） |
| `orderPayStatus` | `Enum` | 是 | 订单支付状态 | `UNPAID/PAID/PAY_FAILED/CLOSED`（当前生产固定 `CLOSED`） |
| `items` | `List<OrderItemSnapshot>` | 是 | 订单商品快照 | - |
| `items[].skuId` | `Long` | 是 | SKU ID | - |
| `items[].quantity` | `Integer` | 是 | 数量 | > 0 |

## 4.4 `mall.pay.payment-succeeded`

- `messageType`：`PAYMENT_SUCCEEDED`
- 生产者：`basebackend-mall-pay-api`
- 消费者：`basebackend-mall-trade-api`、`basebackend-mall-product-api`
- Payload：`PaymentSucceededMessage`

| 字段 | 类型 | 必填 | 说明 | 枚举/固定值 |
| --- | --- | --- | --- | --- |
| `payNo` | `String` | 是 | 支付单号 | - |
| `orderId` | `Long` | 是 | 订单 ID | - |
| `orderNo` | `String` | 是 | 订单号 | - |
| `payAmount` | `BigDecimal` | 是 | 实际支付金额 | - |
| `paymentStatus` | `Enum` | 是 | 支付单状态 | `WAIT_PAY/PAY_SUCCESS/PAY_FAILED/CLOSED`（当前生产固定 `PAY_SUCCESS`） |
| `items` | `List<PaidItem>` | 是 | 已支付商品明细 | 允许为空列表，库存服务将按业务规则处理 |
| `items[].skuId` | `Long` | 是 | SKU ID | - |
| `items[].quantity` | `Integer` | 是 | 数量 | > 0 |

## 4.5 `mall.pay.payment-failed`

- `messageType`：`PAYMENT_FAILED`
- 生产者：`basebackend-mall-pay-api`
- 消费者：`basebackend-mall-trade-api`
- Payload：`PaymentFailedMessage`

| 字段 | 类型 | 必填 | 说明 | 枚举/固定值 |
| --- | --- | --- | --- | --- |
| `payNo` | `String` | 是 | 支付单号 | - |
| `orderId` | `Long` | 是 | 订单 ID | - |
| `orderNo` | `String` | 是 | 订单号 | - |
| `paymentStatus` | `Enum` | 是 | 支付单状态 | `WAIT_PAY/PAY_SUCCESS/PAY_FAILED/CLOSED`（当前生产固定 `PAY_FAILED`） |
| `reason` | `String` | 是 | 失败原因 | 默认值 `MOCK_PAY_FAILED`（若调用方未传） |

## 5. 事件关系总览

1. `mall.trade.order-created`：交易域生产，支付域/商品域消费。
2. `mall.pay.payment-succeeded`：支付域生产，交易域/商品域消费。
3. `mall.pay.payment-failed`：支付域生产，交易域消费。
4. `mall.trade.order-cancelled`：交易域生产，商品域消费。
5. `mall.trade.order-timeout-closed`：交易域生产，支付域/商品域消费。

## 6. 兼容性与演进约束

1. 消费端对 `orderStatus`、`orderPayStatus`、`paymentStatus` 实现了 `null` 兼容（旧消息未带状态字段时不因缺字段失败）。
2. 新增字段应采用“只增不改”策略，避免删除或重命名既有字段。
3. 变更 Topic、`messageType`、字段含义属于破坏性变更，必须通过新 Topic 或版本化方案演进。
4. 枚举扩展需先评估下游消费者解析能力，避免旧版本因未知枚举导致消费异常。

## 7. 联调建议

- 联调时先核对 `topic` 与 `messageType` 是否匹配本文档约定。
- 状态流转核对可配合 `docs/mall/mall-e2e-debug-runbook.md` 一起使用。

## 8. 标准 JSON 样例

- 可直接复用的事件报文样例见：
  - `docs/mall/mall-event-json-examples.md`
