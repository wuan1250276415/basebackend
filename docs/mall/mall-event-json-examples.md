# 商城事件标准 JSON 样例

> 版本：`v1.0`  
> 更新时间：`2026-03-03`  
> 适用范围：商城核心链路（交易 / 支付 / 商品）RocketMQ 事件报文示例

## 1. 使用说明

- 本文档提供“可直接用于联调、压测、回放”的标准 JSON 示例。
- 字段定义与枚举基线请以 `docs/mall/mall-event-schema-contract.md` 为准。
- 示例均采用统一 envelope：`com.basebackend.messaging.model.Message<T>`。
- 示例中仅展示当前链路强相关字段，扩展字段（`headers`、`traceId` 等）按需补充。
- 自动回放脚本见：
  - Linux/macOS：`docs/mall/scripts/replay-mall-e2e.sh`
  - Windows：`docs/mall/scripts/replay-mall-e2e.bat`

## 2. 核心 Topic 标准样例

## 2.1 下单事件 `mall.trade.order-created`

```json
{
  "messageId": "c2cc7584-34ea-4d4e-bf63-5b8f5a1e2f11",
  "topic": "mall.trade.order-created",
  "tags": "ORDER_CREATED",
  "messageType": "ORDER_CREATED",
  "timestamp": "2026-03-03T15:40:00",
  "payload": {
    "orderId": 200001,
    "orderNo": "ORD20260303154000001",
    "userId": 10001,
    "payAmount": 198.00,
    "orderStatus": "CREATED",
    "orderPayStatus": "UNPAID",
    "items": [
      {
        "skuId": 10001,
        "quantity": 1
      },
      {
        "skuId": 10002,
        "quantity": 1
      }
    ]
  }
}
```

## 2.2 支付成功事件 `mall.pay.payment-succeeded`

```json
{
  "messageId": "0eecf0cc-775c-4893-8de6-955412af6921",
  "topic": "mall.pay.payment-succeeded",
  "tags": "PAYMENT_SUCCEEDED",
  "messageType": "PAYMENT_SUCCEEDED",
  "timestamp": "2026-03-03T15:41:00",
  "payload": {
    "payNo": "PAY20260303154100200001",
    "orderId": 200001,
    "orderNo": "ORD20260303154000001",
    "payAmount": 198.00,
    "paymentStatus": "PAY_SUCCESS",
    "items": [
      {
        "skuId": 10001,
        "quantity": 1
      },
      {
        "skuId": 10002,
        "quantity": 1
      }
    ]
  }
}
```

## 2.3 支付失败事件 `mall.pay.payment-failed`

```json
{
  "messageId": "1456f4f2-9fe6-4f55-8d45-4c0ef0073b61",
  "topic": "mall.pay.payment-failed",
  "tags": "PAYMENT_FAILED",
  "messageType": "PAYMENT_FAILED",
  "timestamp": "2026-03-03T15:42:00",
  "payload": {
    "payNo": "PAY20260303154200200002",
    "orderId": 200002,
    "orderNo": "ORD20260303154100002",
    "paymentStatus": "PAY_FAILED",
    "reason": "MOCK_PAY_FAILED"
  }
}
```

## 2.4 支付失败回滚事件 `mall.trade.order-cancelled`

```json
{
  "messageId": "16a6a7db-6428-4810-bf44-7ab9f16b1786",
  "topic": "mall.trade.order-cancelled",
  "tags": "ORDER_CANCELLED",
  "messageType": "ORDER_CANCELLED",
  "timestamp": "2026-03-03T15:42:01",
  "payload": {
    "orderId": 200002,
    "orderNo": "ORD20260303154100002",
    "reason": "MOCK_PAY_FAILED",
    "orderStatus": "CANCELLED",
    "orderPayStatus": "PAY_FAILED",
    "items": [
      {
        "skuId": 10001,
        "quantity": 1
      }
    ]
  }
}
```

## 2.5 超时关单事件 `mall.trade.order-timeout-closed`

```json
{
  "messageId": "7f95cfb2-70e0-4ef8-8da2-5fece35ac312",
  "topic": "mall.trade.order-timeout-closed",
  "tags": "ORDER_TIMEOUT_CLOSED",
  "messageType": "ORDER_TIMEOUT_CLOSED",
  "timestamp": "2026-03-03T15:50:00",
  "payload": {
    "orderId": 200003,
    "orderNo": "ORD20260303154500003",
    "reason": "ORDER_TIMEOUT",
    "orderStatus": "TIMEOUT_CLOSED",
    "orderPayStatus": "CLOSED",
    "items": [
      {
        "skuId": 10002,
        "quantity": 1
      }
    ]
  }
}
```

## 3. 兼容历史消息样例（状态字段缺失）

> 用于验证消费端“状态字段 `null` / 缺失兼容”。

### 3.1 历史下单消息（缺少 `orderStatus`、`orderPayStatus`）

```json
{
  "messageId": "legacy-order-created-001",
  "topic": "mall.trade.order-created",
  "tags": "ORDER_CREATED",
  "messageType": "ORDER_CREATED",
  "timestamp": "2026-03-03T16:00:00",
  "payload": {
    "orderId": 300001,
    "orderNo": "ORD_LEGACY_001",
    "userId": 10009,
    "payAmount": 88.00,
    "items": [
      {
        "skuId": 10001,
        "quantity": 1
      }
    ]
  }
}
```

### 3.2 历史支付成功消息（缺少 `paymentStatus`）

```json
{
  "messageId": "legacy-pay-success-001",
  "topic": "mall.pay.payment-succeeded",
  "tags": "PAYMENT_SUCCEEDED",
  "messageType": "PAYMENT_SUCCEEDED",
  "timestamp": "2026-03-03T16:01:00",
  "payload": {
    "payNo": "PAY_LEGACY_001",
    "orderId": 300001,
    "orderNo": "ORD_LEGACY_001",
    "payAmount": 88.00,
    "items": [
      {
        "skuId": 10001,
        "quantity": 1
      }
    ]
  }
}
```

## 4. 联调建议

1. 先校验 envelope：`topic`、`tags`、`messageType` 三者一致性。
2. 再校验 payload：状态字段与业务动作一致（成功/失败/超时）。
3. 最后核对数据库落库状态与事件状态是否一致（参考 `mall-e2e-debug-runbook.md`）。
