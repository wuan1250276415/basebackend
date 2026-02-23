# 嵌套数据源切换使用指南

## 概述

嵌套数据源切换功能允许在方法调用链中动态切换数据源，并在方法执行完毕后自动恢复到上一个数据源。该功能使用栈结构管理数据源上下文，支持任意深度的嵌套调用。

## 核心特性

### 1. 栈式管理
- 使用 `ThreadLocal<Deque<String>>` 实现线程安全的栈式数据源管理
- 每次切换数据源时，将新数据源压入栈顶
- 方法执行完毕后，弹出栈顶元素，自动恢复到上一个数据源

### 2. 自动恢复
- 通过 AOP 切面自动管理数据源的切换和恢复
- 无需手动编写 try-finally 代码
- 即使方法抛出异常，也能正确恢复数据源

### 3. 详细日志
- 记录每次数据源切换的详细信息
- 显示嵌套层级（栈深度）
- 记录切换前后的数据源名称

## 使用方式

### 基本用法

```java
@Service
public class OrderService {
    
    @DS("master")
    public Order getOrder(Long orderId) {
        // 使用 master 数据源查询订单
        return orderRepository.findById(orderId);
    }
}
```

### 嵌套调用

```java
@Service
public class OrderService {
    
    @Autowired
    private UserService userService;
    
    @DS("master")
    public OrderDetail getOrderDetail(Long orderId) {
        // 当前使用 master 数据源
        Order order = orderRepository.findById(orderId);
        
        // 调用 userService，自动切换到 slave1
        User user = userService.getUserById(order.getUserId());
        
        // 返回后自动恢复到 master 数据源
        return new OrderDetail(order, user);
    }
}

@Service
public class UserService {
    
    @DS("slave1")
    public User getUserById(Long userId) {
        // 使用 slave1 数据源查询用户
        return userRepository.findById(userId);
    }
}
```

### 多层嵌套

```java
@Service
public class OrderService {
    
    @Autowired
    private UserService userService;
    
    @DS("master")
    public CompleteOrderInfo getCompleteOrderInfo(Long orderId) {
        // 层级 1: master
        Order order = orderRepository.findById(orderId);
        
        // 调用 userService，切换到层级 2
        UserWithProducts userInfo = userService.getUserWithProducts(order.getUserId());
        
        // 返回层级 1: master
        return new CompleteOrderInfo(order, userInfo);
    }
}

@Service
public class UserService {
    
    @Autowired
    private ProductService productService;
    
    @DS("slave1")
    public UserWithProducts getUserWithProducts(Long userId) {
        // 层级 2: slave1
        User user = userRepository.findById(userId);
        
        // 调用 productService，切换到层级 3
        List<Product> products = productService.getUserProducts(userId);
        
        // 返回层级 2: slave1
        return new UserWithProducts(user, products);
    }
}

@Service
public class ProductService {
    
    @DS("slave2")
    public List<Product> getUserProducts(Long userId) {
        // 层级 3: slave2
        return productRepository.findByUserId(userId);
    }
}
```

## 工作原理

### 数据源栈管理

```
调用链: OrderService.getCompleteOrderInfo() 
        -> UserService.getUserWithProducts() 
        -> ProductService.getUserProducts()

栈状态变化:
1. 初始状态: []
2. 进入 OrderService: [master]
3. 进入 UserService: [master, slave1]
4. 进入 ProductService: [master, slave1, slave2]
5. 退出 ProductService: [master, slave1]
6. 退出 UserService: [master]
7. 退出 OrderService: []
```

### 日志输出示例

```
INFO  - Datasource switch: [master] (depth: 1) for method: OrderService.getCompleteOrderInfo(..)
INFO  - Nested datasource switch: [master] -> [slave1] (depth: 1 -> 2) for method: UserService.getUserWithProducts(..)
INFO  - Nested datasource switch: [slave1] -> [slave2] (depth: 2 -> 3) for method: ProductService.getUserProducts(..)
INFO  - Restored datasource: [slave2] -> [slave1] (depth: 2) after method: ProductService.getUserProducts(..)
INFO  - Restored datasource: [slave1] -> [master] (depth: 1) after method: UserService.getUserWithProducts(..)
INFO  - Cleared datasource: [master] (depth: 0) after method: OrderService.getCompleteOrderInfo(..)
```

## API 说明

### DataSourceContextHolder

```java
public class DataSourceContextHolder {
    
    /**
     * 设置当前数据源（压入栈）
     * @param dataSourceKey 数据源键
     */
    public static void setDataSourceKey(String dataSourceKey);
    
    /**
     * 获取当前数据源（查看栈顶）
     * @return 当前数据源键，如果栈为空则返回 null
     */
    public static String getDataSourceKey();
    
    /**
     * 清除当前数据源（弹出栈顶）
     * 用于方法执行完成后恢复到上一个数据源
     */
    public static void clearDataSourceKey();
    
    /**
     * 完全清空数据源上下文
     * 用于线程结束时清理
     */
    public static void clear();
    
    /**
     * 获取当前栈深度（用于调试）
     * @return 栈深度
     */
    public static int getStackDepth();
}
```

## 最佳实践

### 1. 使用注解而非手动管理

**推荐：**
```java
@DS("slave1")
public User getUser(Long id) {
    return userRepository.findById(id);
}
```

**不推荐：**
```java
public User getUser(Long id) {
    try {
        DataSourceContextHolder.setDataSourceKey("slave1");
        return userRepository.findById(id);
    } finally {
        DataSourceContextHolder.clearDataSourceKey();
    }
}
```

### 2. 合理规划数据源层级

- 主业务逻辑使用 master
- 查询操作使用 slave
- 避免过深的嵌套（建议不超过 5 层）

### 3. 注意事务边界

```java
@Service
public class OrderService {
    
    @DS("master")
    @Transactional  // 事务在 master 数据源上
    public void createOrder(Order order) {
        orderRepository.save(order);
        
        // 注意：这里切换到 slave1，但事务仍在 master 上
        // 如果需要在 slave1 上执行事务操作，需要单独的事务管理
        userService.notifyUser(order.getUserId());
    }
}
```

### 4. 异常处理

数据源切换在 finally 块中执行，即使方法抛出异常也能正确恢复：

```java
@DS("master")
public Order getOrder(Long id) {
    Order order = orderRepository.findById(id);
    if (order == null) {
        throw new OrderNotFoundException(id);
    }
    // 即使抛出异常，数据源也会正确恢复
    return order;
}
```

## 调试技巧

### 1. 查看当前数据源

```java
String currentDs = DataSourceContextHolder.getDataSourceKey();
log.info("Current datasource: {}", currentDs);
```

### 2. 查看栈深度

```java
int depth = DataSourceContextHolder.getStackDepth();
log.info("Datasource stack depth: {}", depth);
```

### 3. 启用详细日志

在 `application.yml` 中配置：

```yaml
logging:
  level:
    com.basebackend.database.dynamic: DEBUG
```

## 注意事项

### 1. 线程安全

- `DataSourceContextHolder` 使用 `ThreadLocal`，每个线程有独立的栈
- 在异步调用或线程池场景下，需要手动传递数据源上下文

### 2. 内存泄漏

- 确保在线程结束时调用 `DataSourceContextHolder.clear()`
- 在 Web 应用中，可以使用 Filter 或 Interceptor 自动清理

### 3. 性能影响

- 每次方法调用都会进行栈操作，有轻微的性能开销
- 对于高频调用的方法，建议评估性能影响

## 配置

在 `application.yml` 中配置动态数据源：

```yaml
database:
  enhanced:
    dynamic-datasource:
      enabled: true
      primary: master
      strict: true  # 严格模式，数据源不存在时抛异常

spring:
  datasource:
    master:
      url: jdbc:mysql://localhost:3306/master_db
      username: root
      password: password
    slave1:
      url: jdbc:mysql://localhost:3306/slave1_db
      username: root
      password: password
    slave2:
      url: jdbc:mysql://localhost:3306/slave2_db
      username: root
      password: password
```

## 故障排查

### 问题 1: 数据源未切换

**症状：** 使用了 @DS 注解，但数据源没有切换

**可能原因：**
1. AOP 未生效（方法不是 public）
2. 在同一个类内部调用（绕过了代理）
3. 数据源配置错误

**解决方案：**
```java
// 错误：内部调用
@Service
public class UserService {
    @DS("slave1")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getUsers() {
        // 这里不会触发 AOP，因为是内部调用
        return Arrays.asList(getUser(1L), getUser(2L));
    }
}

// 正确：通过注入的代理调用
@Service
public class UserService {
    @Autowired
    private UserService self;  // 注入自己
    
    @DS("slave1")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getUsers() {
        // 通过代理调用，AOP 生效
        return Arrays.asList(self.getUser(1L), self.getUser(2L));
    }
}
```

### 问题 2: 栈深度异常

**症状：** 栈深度不断增加，没有正确清理

**可能原因：**
1. 异常处理不当
2. 手动管理数据源时忘记清理

**解决方案：**
- 使用 @DS 注解而非手动管理
- 确保在 finally 块中清理

### 问题 3: 数据源不存在异常

**症状：** `DataSourceException: DataSource [xxx] not found`

**解决方案：**
1. 检查数据源配置
2. 确认数据源已注册
3. 检查 @DS 注解的值是否正确

## 总结

嵌套数据源切换功能提供了灵活、安全、易用的数据源管理能力。通过栈式管理和 AOP 切面，实现了自动的数据源切换和恢复，大大简化了多数据源场景下的开发工作。
