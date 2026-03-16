# 商城三阶段联调 Runbook

## 1. 目标

本文用于在当前基础架构上快速验证商城三条核心链路：

1. 成功链路：下单 -> 库存预占 -> 支付成功 -> 订单已支付 + 库存实扣
2. 失败链路：下单 -> 支付失败 -> 订单取消 + 库存释放
3. 超时链路：下单后超时关单 -> 支付单关闭 + 库存释放

## 2. 前置条件

### 2.1 基础设施

- MySQL、Redis、Nacos、RocketMQ 已启动并可访问
- 建议先启动基础设施：

```bash
docker-compose -f docker/compose/base/docker-compose.base.yml up -d
```

### 2.2 配置准备

- 已上传商城相关 Nacos 配置：
  - `basebackend-mall-product-api-dev.yml`
  - `basebackend-mall-trade-api-dev.yml`
  - `basebackend-mall-pay-api-dev.yml`
  - `basebackend-gateway-mall-routes-dev.yml`
- 交易域超时关单开关已开启：
  - `mall.trade.timeout-close.enabled=true`
  - `mall.trade.timeout-close.minutes=15`
  - `mall.trade.timeout-close.fixed-delay-ms=60000`

### 2.3 服务启动

- 启动网关 + 三个商城服务
- 网关地址默认使用：`http://127.0.0.1:8080`

## 3. 状态值约定（联调校验重点）

### 3.1 订单状态（mall_order.order_status）

- `CREATED`
- `PAID`
- `CANCELLED`
- `TIMEOUT_CLOSED`

### 3.2 订单支付状态（mall_order.pay_status）

- `UNPAID`
- `PAID`
- `PAY_FAILED`
- `CLOSED`

### 3.3 支付单状态（mall_payment.pay_status）

- `WAIT_PAY`
- `PAY_SUCCESS`
- `PAY_FAILED`
- `CLOSED`

## 4. 成功链路联调

### 4.1 提交订单

```bash
curl -X POST 'http://127.0.0.1:8080/api/mall/trades/orders/submit' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10001,
    "payAmount": 198.00,
    "items": [
      {"skuId": 10001, "quantity": 1},
      {"skuId": 10002, "quantity": 1}
    ]
  }'
```

- 从响应中记录 `orderNo`（后续步骤用 `${ORDER_NO}` 代替）

### 4.2 模拟支付成功

```bash
curl -X POST 'http://127.0.0.1:8080/api/mall/payments/mock-success/${ORDER_NO}'
```

### 4.3 数据库校验

```sql
-- 订单状态应为：PAID / PAID
SELECT order_no, order_status, pay_status, submit_time, pay_time, close_time
FROM mall_order
WHERE order_no = '${ORDER_NO}';

-- 支付单状态应为：PAY_SUCCESS
SELECT order_no, pay_no, pay_status, pay_amount, paid_time
FROM mall_payment
WHERE order_no = '${ORDER_NO}';

-- 库存应体现：stock_quantity 扣减 + lock_quantity 释放
SELECT id, sku_code, stock_quantity, lock_quantity
FROM mall_sku
WHERE id IN (10001, 10002);
```

## 5. 失败链路联调

### 5.1 提交订单

```bash
curl -X POST 'http://127.0.0.1:8080/api/mall/trades/orders/submit' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10002,
    "payAmount": 99.00,
    "items": [
      {"skuId": 10001, "quantity": 1}
    ]
  }'
```

- 从响应中记录 `orderNo`（后续步骤用 `${ORDER_NO}`）

### 5.2 模拟支付失败

```bash
curl -X POST 'http://127.0.0.1:8080/api/mall/payments/mock-fail/${ORDER_NO}?reason=MOCK_PAY_FAILED'
```

### 5.3 数据库校验

```sql
-- 订单状态应为：CANCELLED / PAY_FAILED
SELECT order_no, order_status, pay_status, close_time
FROM mall_order
WHERE order_no = '${ORDER_NO}';

-- 支付单状态应为：PAY_FAILED
SELECT order_no, pay_no, pay_status, paid_time
FROM mall_payment
WHERE order_no = '${ORDER_NO}';

-- 锁定库存应释放
SELECT id, sku_code, stock_quantity, lock_quantity
FROM mall_sku
WHERE id = 10001;
```

## 6. 超时链路联调

> 可选两种方式：等待调度器自动扫描，或手工触发超时关单。

### 6.1 提交订单

```bash
curl -X POST 'http://127.0.0.1:8080/api/mall/trades/orders/submit' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": 10003,
    "payAmount": 129.00,
    "items": [
      {"skuId": 10002, "quantity": 1}
    ]
  }'
```

- 从响应中记录 `orderNo`（后续步骤用 `${ORDER_NO}`）

### 6.2 手工触发超时关单（联调推荐）

```bash
curl -X POST 'http://127.0.0.1:8080/api/mall/trades/orders/${ORDER_NO}/timeout-close'
```

### 6.3 数据库校验

```sql
-- 订单状态应为：TIMEOUT_CLOSED / CLOSED
SELECT order_no, order_status, pay_status, close_time
FROM mall_order
WHERE order_no = '${ORDER_NO}';

-- 支付单状态应为：CLOSED
SELECT order_no, pay_no, pay_status, paid_time
FROM mall_payment
WHERE order_no = '${ORDER_NO}';

-- 锁定库存应释放
SELECT id, sku_code, stock_quantity, lock_quantity
FROM mall_sku
WHERE id = 10002;
```

## 7. 常见排查

1. 消息未消费
   - 检查 RocketMQ topic 是否存在：
     - `mall.trade.order-created`
     - `mall.pay.payment-succeeded`
     - `mall.pay.payment-failed`
     - `mall.trade.order-timeout-closed`
   - 检查对应消费者服务日志是否有异常栈

2. 库存预占/释放异常
   - 检查 `mall_sku.stock_quantity` 与 `mall_sku.lock_quantity` 是否异常
   - 关注错误：`库存不足，无法预占`、`锁定库存不足，无法完成扣减`

3. 状态未流转
   - 订单是否已进入终态（PAID / CANCELLED / TIMEOUT_CLOSED）后重复触发
   - 确认支付单是否仍是 `WAIT_PAY`（mock 接口只对该状态生效）

4. 超时关单不生效
   - 检查 Nacos：`mall.trade.timeout-close.enabled` 是否为 `true`
   - 检查 `minutes` 与 `fixed-delay-ms` 是否符合预期

## 8. 快速核对 SQL（通用）

```sql
-- 订单 + 支付联合核对
SELECT o.order_no,
       o.order_status,
       o.pay_status AS order_pay_status,
       p.pay_status AS payment_status,
       o.submit_time,
       o.pay_time,
       o.close_time
FROM mall_order o
LEFT JOIN mall_payment p ON o.order_no = p.order_no
WHERE o.order_no = '${ORDER_NO}';

-- 订单明细核对
SELECT order_no, sku_id, quantity, unit_price, line_amount
FROM mall_order_item
WHERE order_no = '${ORDER_NO}';
```

## 9. 事件字段核对

- 建议在 RocketMQ 控制台或消费日志中抽样核对消息 envelope：
  - `topic`
  - `tags`
  - `messageType`
  - `payload`
- 字段级核对基线（含枚举值、固定值、兼容规则）：
  - `docs/mall/mall-event-schema-contract.md`
- 可直接用于回放/联调的标准报文样例：
  - `docs/mall/mall-event-json-examples.md`
- 重点关注状态字段：
  - `orderStatus`
  - `orderPayStatus`
  - `paymentStatus`

## 10. 一键回放脚本

> 用于快速触发“成功 / 失败 / 超时”三条链路并生成对应事件。

### 10.1 脚本位置

- Linux/macOS：`docs/mall/scripts/replay-mall-e2e.sh`
- Windows：`docs/mall/scripts/replay-mall-e2e.bat`

### 10.2 使用示例

```bash
# 回放全部链路（success + fail + timeout）
./docs/mall/scripts/replay-mall-e2e.sh --scenario all

# 仅回放成功链路
./docs/mall/scripts/replay-mall-e2e.sh --scenario success

# 仅回放失败链路，并自定义失败原因
./docs/mall/scripts/replay-mall-e2e.sh --scenario fail --reason MANUAL_FAIL

# 指定网关地址与步骤间隔（秒）
./docs/mall/scripts/replay-mall-e2e.sh \
  --base-url http://127.0.0.1:8080 \
  --scenario all \
  --wait-seconds 1
```

```bat
REM 回放全部链路（Windows）
docs\mall\scripts\replay-mall-e2e.bat --scenario all

REM 仅回放失败链路，并自定义失败原因
docs\mall\scripts\replay-mall-e2e.bat --scenario fail --reason MANUAL_FAIL
```

### 10.3 参数说明

- `--base-url`：网关地址，默认 `http://127.0.0.1:8080`
- `--scenario`：`success | fail | timeout | all`
- `--reason`：失败链路 `mock-fail` 的 `reason` 参数，默认 `MOCK_PAY_FAILED`
- `--wait-seconds`：步骤间隔秒数，默认 `1`
