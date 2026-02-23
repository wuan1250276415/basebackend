# 分布式锁使用指南

## 概述

basebackend-cache 模块提供了完整的分布式锁功能，支持多种锁类型和使用方式。

## 功能特性

- ✅ 可重入锁（Reentrant Lock）
- ✅ 公平锁（Fair Lock）
- ✅ 联锁（Multi Lock）
- ✅ 红锁（Red Lock）
- ✅ 读写锁（Read-Write Lock）
- ✅ 注解驱动（@DistributedLock）
- ✅ 编程式API
- ✅ 自动释放（租约机制）

## 使用方式

### 1. 注解方式（推荐）

最简单的使用方式是通过 `@DistributedLock` 注解：

```java
@Service
public class OrderService {
    
    // 基本用法
    @DistributedLock(key = "order:create")
    public Order createOrder(OrderRequest request) {
        // 业务逻辑
        return order;
    }
    
    // 使用 SpEL 表达式动态生成锁键
    @DistributedLock(key = "user:#{#userId}:order")
    public Order createUserOrder(Long userId, OrderRequest request) {
        // 业务逻辑
        return order;
    }
    
    // 自定义等待时间和租约时间
    @DistributedLock(
        key = "inventory:#{#productId}",
        waitTime = 5,
        leaseTime = 30,
        timeUnit = TimeUnit.SECONDS
    )
    public void updateInventory(Long productId, int quantity) {
        // 业务逻辑
    }
    
    // 使用公平锁
    @DistributedLock(
        key = "fair:queue",
        lockType = DistributedLock.LockType.FAIR
    )
    public void processQueue() {
        // 按请求顺序处理
    }
    
    // 使用读锁
    @DistributedLock(
        key = "data:read",
        lockType = DistributedLock.LockType.READ
    )
    public Data readData() {
        // 多个线程可以同时读取
        return data;
    }
    
    // 使用写锁
    @DistributedLock(
        key = "data:write",
        lockType = DistributedLock.LockType.WRITE
    )
    public void writeData(Data data) {
        // 只有一个线程可以写入
    }
}
```

### 2. 编程式API

通过注入 `DistributedLockService` 使用：

```java
@Service
@RequiredArgsConstructor
public class PaymentService {
    
    private final DistributedLockService lockService;
    
    // 基本用法
    public void processPayment(String orderId) {
        String lockKey = "payment:" + orderId;
        boolean locked = lockService.tryLock(lockKey, 3, 10, TimeUnit.SECONDS);
        
        if (locked) {
            try {
                // 业务逻辑
            } finally {
                lockService.unlock(lockKey);
            }
        } else {
            throw new RuntimeException("Failed to acquire lock");
        }
    }
    
    // 使用 executeWithLock 简化代码
    public Order createOrder(OrderRequest request) {
        return lockService.executeWithLock(
            "order:create",
            () -> {
                // 业务逻辑
                return order;
            },
            3,  // waitTime
            10  // leaseTime
        );
    }
    
    // 使用联锁（同时锁定多个资源）
    public void transferMoney(String fromAccount, String toAccount, BigDecimal amount) {
        RLock multiLock = lockService.getMultiLock(
            "account:" + fromAccount,
            "account:" + toAccount
        );
        
        try {
            if (multiLock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 转账逻辑
                } finally {
                    multiLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted", e);
        }
    }
    
    // 使用读写锁
    public Data readData(String dataId) {
        boolean locked = lockService.tryReadLock("data:" + dataId, 3, 10, TimeUnit.SECONDS);
        if (locked) {
            try {
                // 读取数据
                return data;
            } finally {
                lockService.unlockRead("data:" + dataId);
            }
        }
        return null;
    }
    
    public void updateData(String dataId, Data newData) {
        boolean locked = lockService.tryWriteLock("data:" + dataId, 3, 10, TimeUnit.SECONDS);
        if (locked) {
            try {
                // 更新数据
            } finally {
                lockService.unlockWrite("data:" + dataId);
            }
        }
    }
}
```

### 3. 使用工具类

通过 `RedissonLockUtil` 工具类：

```java
@Service
@RequiredArgsConstructor
public class InventoryService {
    
    private final RedissonLockUtil lockUtil;
    
    public void updateStock(Long productId, int quantity) {
        String lockKey = "stock:" + productId;
        
        lockUtil.executeWithLock(
            lockKey,
            3, 10, TimeUnit.SECONDS,
            () -> {
                // 更新库存逻辑
            }
        );
    }
    
    // 使用公平锁
    public void fairProcess() {
        RLock fairLock = lockUtil.getFairLock("fair:process");
        try {
            if (fairLock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // 业务逻辑
                } finally {
                    fairLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 锁类型说明

### 可重入锁（Reentrant Lock）
- 默认锁类型
- 同一线程可以多次获取同一把锁
- 适用于大多数场景

### 公平锁（Fair Lock）
- 按照请求顺序分配锁
- 防止线程饥饿
- 性能略低于非公平锁

### 联锁（Multi Lock）
- 同时锁定多个资源
- 要么全部成功，要么全部失败
- 适用于需要原子性操作多个资源的场景

### 红锁（Red Lock）
- 在多个 Redis 实例上获取锁
- 提高可靠性和容错性
- 需要配置多个 Redis 实例

### 读写锁（Read-Write Lock）
- 读锁：多个线程可以同时持有
- 写锁：与任何其他锁互斥
- 适用于读多写少的场景

## 配置

在 `application.yml` 中配置：

```yaml
basebackend:
  cache:
    lock:
      # 默认等待时间
      default-wait-time: 10s
      # 默认租约时间
      default-lease-time: 30s
      # 是否启用公平锁
      fair-lock-enabled: false
      # 是否启用红锁
      red-lock-enabled: false

# Redisson 配置
redisson:
  single-server-config:
    address: redis://localhost:6379
    database: 0
    password: 
    connect-timeout: 3000
    timeout: 3000
    connection-pool-size: 64
    connection-minimum-idle-size: 10
```

## 最佳实践

### 1. 合理设置超时时间

```java
// 等待时间不宜过长，避免阻塞
// 租约时间要大于业务执行时间
@DistributedLock(
    key = "business:key",
    waitTime = 3,      // 等待3秒
    leaseTime = 30     // 持有30秒
)
```

### 2. 使用 try-finally 确保锁释放

```java
boolean locked = lockService.tryLock(lockKey, 3, 10, TimeUnit.SECONDS);
if (locked) {
    try {
        // 业务逻辑
    } finally {
        lockService.unlock(lockKey);  // 确保释放
    }
}
```

### 3. 锁粒度要合适

```java
// ❌ 锁粒度太粗
@DistributedLock(key = "global:lock")
public void processAll() { }

// ✅ 锁粒度合适
@DistributedLock(key = "order:#{#orderId}")
public void processOrder(String orderId) { }
```

### 4. 避免死锁

```java
// 使用联锁时，确保锁的顺序一致
// ✅ 正确：按照固定顺序获取锁
RLock multiLock = lockService.getMultiLock(
    "resource:" + Math.min(id1, id2),
    "resource:" + Math.max(id1, id2)
);
```

### 5. 处理锁获取失败

```java
@DistributedLock(
    key = "business:key",
    throwException = false  // 不抛异常，返回null
)
public Result process() {
    // 如果获取锁失败，方法返回null
    return result;
}
```

## 注意事项

1. **租约时间**：设置合理的租约时间，避免锁长时间占用
2. **异常处理**：确保在异常情况下也能释放锁
3. **性能考虑**：公平锁性能略低，根据场景选择
4. **网络分区**：使用红锁提高在网络分区情况下的可靠性
5. **监控告警**：监控锁的获取失败率和持有时间

## 故障排查

### 锁无法获取
- 检查 Redis 连接是否正常
- 检查等待时间是否足够
- 检查是否有死锁情况

### 锁未释放
- 检查是否使用了 try-finally
- 检查租约时间是否合理
- 查看 Redisson 的看门狗机制是否正常

### 性能问题
- 减小锁的粒度
- 使用读写锁优化读多写少场景
- 考虑使用本地锁替代分布式锁
