# 嵌套数据源切换快速入门

## 5 分钟快速上手

### 1. 基本概念

嵌套数据源切换允许你在方法调用链中使用不同的数据源，系统会自动管理数据源的切换和恢复。

### 2. 快速示例

```java
@Service
public class OrderService {
    
    @Autowired
    private UserService userService;
    
    // 使用 master 数据源
    @DS("master")
    public Order getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId);
        
        // 这里会自动切换到 slave1
        User user = userService.getUser(order.getUserId());
        
        // 返回后自动恢复到 master
        order.setUser(user);
        return order;
    }
}

@Service
public class UserService {
    
    // 使用 slave1 数据源
    @DS("slave1")
    public User getUser(Long userId) {
        return userRepository.findById(userId);
    }
}
```

### 3. 工作原理

```
调用链: OrderService.getOrder() -> UserService.getUser()

数据源栈变化:
1. 进入 OrderService: [master]
2. 进入 UserService: [master, slave1]  ← 嵌套
3. 退出 UserService: [master]          ← 自动恢复
4. 退出 OrderService: []               ← 清空
```

### 4. 日志输出

```
INFO - Datasource switch: [master] (depth: 1) for method: OrderService.getOrder(..)
INFO - Nested datasource switch: [master] -> [slave1] (depth: 1 -> 2) for method: UserService.getUser(..)
INFO - Restored datasource: [slave1] -> [master] (depth: 1) after method: UserService.getUser(..)
INFO - Cleared datasource: [master] (depth: 0) after method: OrderService.getOrder(..)
```

### 5. 配置

在 `application.yml` 中配置数据源：

```yaml
database:
  enhanced:
    dynamic-datasource:
      enabled: true
      primary: master
      strict: true

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
```

### 6. 常见场景

#### 场景 1: 主从分离

```java
@Service
public class ProductService {
    
    @DS("master")
    public void createProduct(Product product) {
        // 写操作使用 master
        productRepository.save(product);
    }
    
    @DS("slave1")
    public Product getProduct(Long id) {
        // 读操作使用 slave
        return productRepository.findById(id);
    }
}
```

#### 场景 2: 多层嵌套

```java
@Service
public class OrderService {
    @Autowired
    private UserService userService;
    
    @DS("master")
    public void processOrder(Long orderId) {
        // 层级 1: master
        Order order = orderRepository.findById(orderId);
        userService.notifyUser(order.getUserId());
    }
}

@Service
public class UserService {
    @Autowired
    private NotificationService notificationService;
    
    @DS("slave1")
    public void notifyUser(Long userId) {
        // 层级 2: slave1
        User user = userRepository.findById(userId);
        notificationService.send(user.getEmail());
    }
}

@Service
public class NotificationService {
    
    @DS("slave2")
    public void send(String email) {
        // 层级 3: slave2
        // 发送通知
    }
}
```

#### 场景 3: 同一数据源嵌套

```java
@Service
public class ReportService {
    
    @DS("master")
    public void generateReport() {
        // 栈深度: 1
        List<Data> data = loadData();
        processData(data);
    }
    
    @DS("master")
    private void processData(List<Data> data) {
        // 栈深度: 2 (即使是同一数据源)
        // 处理数据
    }
}
```

### 7. 注意事项

#### ❌ 错误用法

```java
@Service
public class UserService {
    
    @DS("slave1")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getUsers() {
        // 错误：内部调用不会触发 AOP
        return Arrays.asList(getUser(1L), getUser(2L));
    }
}
```

#### ✅ 正确用法

```java
@Service
public class UserService {
    
    @Autowired
    private UserService self;  // 注入自己
    
    @DS("slave1")
    public User getUser(Long id) {
        return userRepository.findById(id);
    }
    
    public List<User> getUsers() {
        // 正确：通过代理调用
        return Arrays.asList(self.getUser(1L), self.getUser(2L));
    }
}
```

### 8. 调试技巧

#### 查看当前数据源

```java
String currentDs = DataSourceContextHolder.getDataSourceKey();
log.info("Current datasource: {}", currentDs);
```

#### 查看栈深度

```java
int depth = DataSourceContextHolder.getStackDepth();
log.info("Stack depth: {}", depth);
```

#### 启用详细日志

```yaml
logging:
  level:
    com.basebackend.database.dynamic: DEBUG
```

### 9. 完整示例

```java
@Service
@Slf4j
public class OrderService {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ProductService productService;
    
    @DS("master")
    @Transactional
    public OrderDetail createOrder(OrderRequest request) {
        log.info("开始创建订单，数据源: {}", 
            DataSourceContextHolder.getDataSourceKey());
        
        // 1. 在 master 上创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        orderRepository.save(order);
        
        // 2. 从 slave1 查询用户信息
        User user = userService.getUser(request.getUserId());
        
        // 3. 从 slave2 查询产品信息
        Product product = productService.getProduct(request.getProductId());
        
        log.info("订单创建完成，数据源: {}", 
            DataSourceContextHolder.getDataSourceKey());
        
        return new OrderDetail(order, user, product);
    }
}

@Service
public class UserService {
    
    @DS("slave1")
    public User getUser(Long userId) {
        log.info("查询用户，数据源: {}", 
            DataSourceContextHolder.getDataSourceKey());
        return userRepository.findById(userId);
    }
}

@Service
public class ProductService {
    
    @DS("slave2")
    public Product getProduct(Long productId) {
        log.info("查询产品，数据源: {}", 
            DataSourceContextHolder.getDataSourceKey());
        return productRepository.findById(productId);
    }
}
```

### 10. 更多信息

- 详细文档: [NESTED_DATASOURCE_SWITCHING.md](NESTED_DATASOURCE_SWITCHING.md)
- 实现总结: [NESTED_DATASOURCE_IMPLEMENTATION_SUMMARY.md](NESTED_DATASOURCE_IMPLEMENTATION_SUMMARY.md)
- 示例代码: `src/main/java/com/basebackend/database/dynamic/example/NestedDataSourceExample.java`

## 快速检查清单

- [ ] 已配置多个数据源
- [ ] 已启用动态数据源功能
- [ ] 方法上添加了 @DS 注解
- [ ] 方法是 public 的（AOP 要求）
- [ ] 避免了内部调用（使用注入的代理）
- [ ] 已启用日志查看切换过程

## 常见问题

**Q: 为什么数据源没有切换？**
A: 检查方法是否是 public，是否通过代理调用（避免内部调用）

**Q: 如何查看当前使用的数据源？**
A: 使用 `DataSourceContextHolder.getDataSourceKey()`

**Q: 嵌套调用会影响性能吗？**
A: 有轻微的栈操作开销，但对大多数应用来说可以忽略不计

**Q: 支持异步调用吗？**
A: 需要手动传递数据源上下文，因为 ThreadLocal 不会自动传递到新线程

**Q: 可以嵌套多少层？**
A: 理论上无限制，但建议不超过 5 层以保持代码清晰

---

**开始使用**: 只需在方法上添加 `@DS("数据源名称")` 注解即可！
