# 商城事件 Topic 约定（首版）

> 目的：统一商品、交易、支付三域的事件命名，便于 RocketMQ 订阅编排。

## 1. 商品域（Product）

- `mall.product.on-shelf`：商品上架
- `mall.product.off-shelf`：商品下架
- `mall.stock.reserved`：库存预占
- `mall.stock.released`：库存释放

## 2. 交易域（Trade）

- `mall.trade.order-created`：订单创建
- `mall.trade.order-cancelled`：订单取消
- `mall.trade.order-paid`：订单已支付
- `mall.trade.order-timeout-closed`：订单超时关闭

## 3. 支付域（Pay）

- `mall.pay.payment-created`：支付单创建
- `mall.pay.payment-succeeded`：支付成功
- `mall.pay.payment-failed`：支付失败
- `mall.pay.refund-created`：退款单创建
- `mall.pay.refund-succeeded`：退款成功

## 4. 命名规范

1. 统一前缀：`mall.<domain>.<event>`
2. 事件语义：使用过去式或结果态，表达“已发生”
3. 版本演进：新增语义优先新增 topic，避免重用旧 topic 破坏兼容

## 5. 字段契约文档

- 字段级 Schema、枚举值、生产消费关系详见：
  - `docs/mall/mall-event-schema-contract.md`
- 标准事件 JSON 样例详见：
  - `docs/mall/mall-event-json-examples.md`
