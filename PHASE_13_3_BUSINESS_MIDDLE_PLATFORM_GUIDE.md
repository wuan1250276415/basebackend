# Phase 13.3: 业务中台建设实施指南

## 📋 概述

本指南介绍如何构建企业级业务中台，通过DDD（领域驱动设计）方法论进行业务建模，沉淀可复用的业务能力，提升业务敏捷性和响应速度，支撑业务快速创新和迭代。

---

## 🏗️ 业务中台整体架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                      业务中台架构设计                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐           │
│  │   用户中台    │  │   订单中台    │  │   支付中台    │           │
│  │              │  │              │  │              │           │
│  │ • 账户管理     │  │ • 订单管理     │  │ • 支付处理     │           │
│  │ • 权限控制     │  │ • 订单状态     │  │ • 对账结算     │           │
│  │ • 用户画像     │  │ • 订单流程     │  │ • 退款处理     │           │
│  │ • 会员体系     │  │ • 物流跟踪     │  │ • 风控审核     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   商品中台    │  │   营销中台    │  │   通知中台    │           │
│  │              │  │              │  │              │           │
│  │ • 商品管理     │  │ • 优惠券     │  │ • 短信通知     │           │
│  │ • 类目管理     │  │ • 促销活动     │  │ • 邮件通知     │           │
│  │ • 价格管理     │  │ • 会员权益     │  │ • 推送通知     │           │
│  │ • 库存管理     │  │ • 分销体系     │  │ • 站内消息     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌──────▼────────┐  ┌─────▼──────┐  ┌──────▼──────┐           │
│  │   搜索中台    │  │   数据中台    │  │   日志中台    │           │
│  │              │  │              │  │              │           │
│  │ • 全文搜索     │  │ • 数据采集     │  │ • 日志收集     │           │
│  │ • 智能推荐     │  │ • 数据处理     │  │ • 日志分析     │           │
│  │ • 搜索排序     │  │ • 数据分析     │  │ • 日志搜索     │           │
│  │ • 搜索统计     │  │ • 数据可视化   │  │ • 审计日志     │           │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘           │
│         │                 │                 │                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    基础能力层                                 │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • 配置中心 (Nacos)                                            │ │
│  │ • 注册中心 (Nacos)                                            │ │
│  │ • 分布式锁 (Redis)                                             │ │
│  │ • 消息队列 (Kafka)                                             │ │
│  │ • 分布式事务 (Seata)                                           │ │
│  │ • 任务调度 (XXL-Job)                                          │ │
│  │ • 缓存 (Redis)                                                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    基础技术层                                 │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │ • Spring Cloud 微服务框架                                     │ │
│  │ • MyBatis Plus 数据库访问框架                                  │ │
│  │ • Elasticsearch 搜索引擎                                      │ │
│  │ • ClickHouse OLAP 数据库                                      │ │
│  │ • MinIO 对象存储                                               │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────┘
```

### 技术栈选型

| 层次 | 技术组件 | 版本 | 用途 |
|------|----------|------|------|
| **领域建模** | DDD + CQRS | - | 领域驱动设计 |
| **事件驱动** | Apache Kafka | 3.6.0 | 领域事件 |
| **数据存储** | MySQL 8.0 | 8.0.35 | 业务数据库 |
| **缓存层** | Redis | 7.2.0 | 热点数据缓存 |
| **搜索引擎** | Elasticsearch | 8.11.0 | 全文搜索 |
| **数据仓库** | ClickHouse | 23.12.0 | 实时分析 |
| **API网关** | Spring Cloud Gateway | 4.1.0 | 统一入口 |
| **服务治理** | Spring Cloud Alibaba | 2022.0.0.0 | 服务治理 |

---

## 🧱 DDD 领域驱动设计

### 1. 核心概念

#### 领域（Domain）
业务领域是业务中台服务的核心，每个领域包含：
- **限界上下文（Bounded Context）**：明确业务边界
- **聚合（Aggregate）**：业务一致性单元
- **聚合根（Aggregate Root）**：聚合的管理者
- **领域服务（Domain Service）**：跨聚合的业务逻辑
- **领域事件（Domain Event）**：领域内的重要业务事件

#### 领域架构模式

```java
/**
 * 领域层 - 核心业务逻辑
 * 不依赖外部框架，纯Java实现
 */
package com.basebackend.domain;

/**
 * 聚合根示例 - 用户
 * 负责维护数据一致性和业务规则
 */
public class User {
    // 聚合根ID
    private UserId id;
    // 领域属性（不可变）
    private final String username;
    private final String email;
    private final PhoneNumber phone;
    private UserStatus status;

    // 构造函数
    public User(UserId id, String username, String email, PhoneNumber phone) {
        // 业务规则检查
        if (StringUtils.isEmpty(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!EmailValidator.isValid(email)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }

        this.id = id;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.status = UserStatus.ACTIVE;
    }

    // 领域方法 - 业务行为
    public void changeStatus(UserStatus newStatus) {
        // 业务规则
        if (this.status == newStatus) {
            return;
        }

        // 状态变更验证
        if (this.status == UserStatus.DELETED && newStatus != UserStatus.ACTIVE) {
            throw new BusinessException("已删除用户不能变更状态");
        }

        this.status = newStatus;

        // 发布领域事件
        DomainEventPublisher.publish(new UserStatusChangedEvent(this.id, newStatus));
    }

    // 禁止外部修改领域属性
    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public UserStatus getStatus() {
        return status;
    }
}

/**
 * 值对象示例 - 手机号
 * 不可变，无唯一标识
 */
public class PhoneNumber {
    private final String number;

    public PhoneNumber(String number) {
        // 验证格式
        if (!isValidPhoneNumber(number)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        this.number = number;
    }

    private boolean isValidPhoneNumber(String number) {
        // 简单验证，实际可使用更复杂的规则
        return number != null && number.matches("^1[3-9]\\d{9}$");
    }

    @Override
    public String toString() {
        return number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhoneNumber)) return false;
        PhoneNumber that = (PhoneNumber) o;
        return number.equals(that.number);
    }

    @Override
    public int hashCode() {
        return number.hashCode();
    }
}

/**
 * 领域事件示例
 */
public class UserStatusChangedEvent implements DomainEvent {
    private final UserId userId;
    private final UserStatus newStatus;
    private final DateTime occurredAt;

    public UserStatusChangedEvent(UserId userId, UserStatus newStatus) {
        this.userId = userId;
        this.newStatus = newStatus;
        this.occurredAt = DateTime.now();
    }

    @Override
    public UserId getUserId() {
        return userId;
    }

    public UserStatus getNewStatus() {
        return newStatus;
    }

    @Override
    public DateTime getOccurredAt() {
        return occurredAt;
    }
}
```

#### 仓储模式（Repository）

```java
/**
 * 仓储接口 - 领域层定义
 * 抽象数据访问，不依赖具体实现
 */
public interface UserRepository {

    /**
     * 根据ID查找用户
     */
    Optional<User> findById(UserId id);

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 保存用户
     */
    void save(User user);

    /**
     * 删除用户
     */
    void delete(UserId id);

    /**
     * 查询用户列表
     */
    Page<User> findAll(int page, int size);

    /**
     * 根据条件查询
     */
    List<User> findBySpecification(UserSpecification spec);
}
```

#### 领域服务（Domain Service）

```java
/**
 * 领域服务 - 处理跨聚合的业务逻辑
 */
public class UserDomainService {

    private final UserRepository userRepository;

    public UserDomainService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 检查用户名是否可用
     */
    public boolean isUsernameAvailable(String username) {
        return userRepository.findByUsername(username).isEmpty();
    }

    /**
     * 用户注册
     * 处理复杂的注册流程
     */
    public void registerUser(String username, String email, PhoneNumber phone) {
        // 业务规则检查
        if (!isUsernameAvailable(username)) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已注册
        // 检查手机号是否已注册
        // ...

        // 创建用户
        User user = new User(new UserId(), username, email, phone);

        // 保存到仓储
        userRepository.save(user);

        // 发送注册成功事件
        DomainEventPublisher.publish(new UserRegisteredEvent(user.getId(), email, phone));
    }

    /**
     * 用户登录
     */
    public LoginResult login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return LoginResult.failed("用户名或密码错误");
        }

        User user = userOpt.get();

        // 检查用户状态
        if (user.getStatus() != UserStatus.ACTIVE) {
            return LoginResult.failed("用户已禁用");
        }

        // 验证密码
        if (!PasswordEncoder.matches(password, user.getPasswordHash())) {
            return LoginResult.failed("用户名或密码错误");
        }

        // 生成登录token
        String token = generateToken(user);

        return LoginResult.success(token, user);
    }
}
```

### 2. 聚合设计

#### 用户聚合

```java
/**
 * 用户聚合
 * 包含用户基本信息、角色、权限等
 */
public class UserAggregate {

    private UserId userId;
    private UserInfo userInfo;
    private Set<Role> roles;
    private Set<Permission> permissions;
    private UserStatistics statistics;
    private List<LoginLog> loginLogs;

    // 聚合根方法
    public void assignRole(Role role) {
        this.roles.add(role);
        DomainEventPublisher.publish(new UserRoleAssignedEvent(userId, role));
    }

    public void removeRole(RoleId roleId) {
        this.roles.removeIf(role -> role.getId().equals(roleId));
        DomainEventPublisher.publish(new UserRoleRemovedEvent(userId, roleId));
    }

    public void recordLogin(String ipAddress) {
        LoginLog log = new LoginLog(userId, DateTime.now(), ipAddress);
        this.loginLogs.add(log);
        this.statistics.recordLogin();
    }
}
```

#### 订单聚合

```java
/**
 * 订单聚合
 * 包含订单主信息、订单项、物流信息等
 */
public class OrderAggregate {

    private OrderId orderId;
    private OrderNumber orderNumber;
    private UserId userId;
    private OrderStatus status;
    private Money totalAmount;
    private List<OrderItem> items;
    private ShippingAddress shippingAddress;
    private PaymentInfo paymentInfo;
    private LogisticsInfo logisticsInfo;

    // 聚合根方法
    public void confirmOrder() {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException("订单状态不正确");
        }

        this.status = OrderStatus.CONFIRMED;
        DomainEventPublisher.publish(new OrderConfirmedEvent(orderId));
    }

    public void cancelOrder() {
        if (this.status == OrderStatus.DELIVERED ||
            this.status == OrderStatus.COMPLETED) {
            throw new BusinessException("订单已发货或完成，无法取消");
        }

        this.status = OrderStatus.CANCELLED;

        // 释放库存
        this.items.forEach(item -> {
            InventoryService.releaseInventory(item.getProductId(), item.getQuantity());
        });

        // 退款
        if (this.paymentInfo != null && this.paymentInfo.isPaid()) {
            PaymentService.refund(this.paymentInfo.getTransactionId(), totalAmount);
        }

        DomainEventPublisher.publish(new OrderCancelledEvent(orderId));
    }

    public void addOrderItem(ProductId productId, int quantity, Money unitPrice) {
        OrderItem item = new OrderItem(productId, quantity, unitPrice);
        this.items.add(item);

        // 扣减库存
        InventoryService.reserveInventory(productId, quantity);

        // 重新计算总金额
        this.totalAmount = calculateTotalAmount();
    }

    private Money calculateTotalAmount() {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

### 3. 限界上下文划分

#### 用户领域限界上下文

```
用户领域限界上下文
┌─────────────────────────────────────────┐
│  用户管理边界                             │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────┐  ┌─────────────┐      │
│  │  账户管理    │  │  权限管理    │      │
│  │             │  │             │      │
│  │ • 用户注册   │  │ • 角色定义   │      │
│  │ • 用户登录   │  │ • 权限分配   │      │
│  │ • 用户信息   │  │ • 资源控制   │      │
│  └──────┬──────┘  └──────┬──────┘      │
│         │                 │              │
│  ┌──────▼────────┐  ┌─────▼──────┐     │
│  │  会员体系     │  │  用户画像    │     │
│  │             │  │             │     │
│  │ • 等级管理   │  │ • 行为分析   │     │
│  │ • 积分规则   │  │ • 标签管理   │     │
│  │ • 权益体系   │  │ • 偏好分析   │     │
│  └─────────────┘  └─────────────┘     │
│                                         │
│  领域事件：                              │
│  • UserRegistered                       │
│  • UserStatusChanged                   │
│  • UserLoggedIn                        │
│  • UserRoleChanged                     │
└─────────────────────────────────────────┘
```

---

## 🏢 业务中台架构设计

### 1. 用户中台

```java
/**
 * 用户中台核心服务
 */
@Service
@Validated
public class UserMiddlePlatformService {

    @Autowired
    private UserDomainService userDomainService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventBus eventBus;

    /**
     * 用户注册
     * 支持多种注册方式
     */
    @Transactional
    public UserRegisterResult registerUser(UserRegisterRequest request) {
        // 1. 参数验证
        RegisterValidator.validate(request);

        // 2. 业务规则检查
        if (!userDomainService.isUsernameAvailable(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }

        // 3. 创建用户
        PhoneNumber phone = new PhoneNumber(request.getPhone());
        User user = new User(
            new UserId(),
            request.getUsername(),
            request.getEmail(),
            phone
        );

        // 4. 保存用户
        userRepository.save(user);

        // 5. 发送领域事件
        eventBus.publish(new UserRegisteredEvent(
            user.getId(),
            request.getEmail(),
            phone
        ));

        // 6. 初始化用户数据
        initializeUserData(user);

        return new UserRegisterResult(user.getId().getValue(), "注册成功");
    }

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public UserLoginResult login(UserLoginRequest request) {
        return userDomainService.login(request.getUsername(), request.getPassword());
    }

    /**
     * 获取用户信息
     */
    @Transactional(readOnly = true)
    public UserInfoDTO getUserInfo(UserId userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("用户不存在"));

        return UserInfoMapper.toDTO(user);
    }

    /**
     * 批量查询用户信息
     */
    @Transactional(readOnly = true)
    public List<UserInfoDTO> batchGetUserInfo(List<UserId> userIds) {
        return userIds.stream()
            .map(this::getUserInfo)
            .collect(Collectors.toList());
    }

    private void initializeUserData(User user) {
        // 初始化默认角色
        Role defaultRole = RoleRepository.findByCode("USER");
        user.assignRole(defaultRole);

        // 初始化用户画像
        UserProfile profile = new UserProfile(user.getId());
        UserProfileRepository.save(profile);

        // 发送欢迎消息
        eventBus.publish(new WelcomeMessageEvent(user.getId(), user.getEmail()));
    }
}

/**
 * 用户注册请求DTO
 */
@Data
@Builder
public class UserRegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Length(min = 3, max = 20, message = "用户名长度必须在3-20字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Length(min = 8, max = 20, message = "密码长度必须在8-20字符之间")
    private String password;
}

/**
 * 用户信息DTO
 */
@Data
@Builder
public class UserInfoDTO {

    private String userId;
    private String username;
    private String email;
    private String phone;
    private UserStatus status;
    private List<RoleInfo> roles;
    private UserStatisticsDTO statistics;
    private Date createTime;
    private Date lastLoginTime;
}
```

#### 用户中台API设计

```java
/**
 * 用户中台API
 */
@RestController
@RequestMapping("/api/middle-platform/user")
@Api(tags = "用户中台")
@Validated
public class UserMiddlePlatformController {

    @Autowired
    private UserMiddlePlatformService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @ApiOperation("用户注册")
    public Result<UserRegisterResult> register(@Valid @RequestBody UserRegisterRequest request) {
        UserRegisterResult result = userService.registerUser(request);
        return Result.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public Result<UserLoginResult> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResult result = userService.login(request);
        return Result.success(result);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{userId}")
    @ApiOperation("获取用户信息")
    public Result<UserInfoDTO> getUserInfo(@PathVariable String userId) {
        UserInfoDTO userInfo = userService.getUserInfo(new UserId(userId));
        return Result.success(userInfo);
    }

    /**
     * 批量获取用户信息
     */
    @PostMapping("/batch")
    @ApiOperation("批量获取用户信息")
    public Result<List<UserInfoDTO>> batchGetUserInfo(@RequestBody List<String> userIds) {
        List<UserId> ids = userIds.stream()
            .map(UserId::new)
            .collect(Collectors.toList());
        List<UserInfoDTO> result = userService.batchGetUserInfo(ids);
        return Result.success(result);
    }

    /**
     * 验证用户名是否可用
     */
    @GetMapping("/check-username/{username}")
    @ApiOperation("验证用户名是否可用")
    public Result<Boolean> checkUsername(@PathVariable String username) {
        boolean available = userService.isUsernameAvailable(username);
        return Result.success(available);
    }

    /**
     * 根据用户名查询用户
     */
    @GetMapping("/by-username/{username}")
    @ApiOperation("根据用户名查询用户")
    public Result<UserInfoDTO> getUserByUsername(@PathVariable String username) {
        UserInfoDTO userInfo = userService.getUserByUsername(username);
        return Result.success(userInfo);
    }
}
```

### 2. 订单中台

```java
/**
 * 订单中台核心服务
 */
@Service
@Validated
public class OrderMiddlePlatformService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EventBus eventBus;

    /**
     * 创建订单
     */
    @Transactional
    public OrderCreateResult createOrder(OrderCreateRequest request) {
        // 1. 验证订单项
        List<OrderItem> items = validateAndCreateItems(request.getItems());

        // 2. 检查库存
        items.forEach(item -> {
            if (!inventoryService.checkInventory(item.getProductId(), item.getQuantity())) {
                throw new BusinessException("商品库存不足: " + item.getProductId());
            }
        });

        // 3. 计算订单总金额
        Money totalAmount = calculateTotalAmount(items);

        // 4. 创建订单聚合
        OrderAggregate order = new OrderAggregate(
            new OrderId(),
            new OrderNumber(generateOrderNumber()),
            new UserId(request.getUserId()),
            items,
            totalAmount,
            request.getShippingAddress()
        );

        // 5. 保存订单
        orderRepository.save(order);

        // 6. 扣减库存
        items.forEach(item -> {
            inventoryService.reserveInventory(item.getProductId(), item.getQuantity());
        });

        // 7. 发送订单创建事件
        eventBus.publish(new OrderCreatedEvent(order.getOrderId()));

        return new OrderCreateResult(order.getOrderId().getValue(), order.getOrderNumber().getValue());
    }

    /**
     * 确认订单
     */
    @Transactional
    public void confirmOrder(String orderId) {
        OrderAggregate order = findOrder(orderId);
        order.confirmOrder();
        orderRepository.save(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public void cancelOrder(String orderId, String reason) {
        OrderAggregate order = findOrder(orderId);
        order.cancelOrder();

        // 保存订单
        orderRepository.save(order);

        // 发送订单取消事件
        eventBus.publish(new OrderCancelledEvent(order.getOrderId(), reason));
    }

    /**
     * 查询订单
     */
    @Transactional(readOnly = true)
    public OrderInfoDTO getOrderInfo(String orderId) {
        OrderAggregate order = findOrder(orderId);
        return OrderMapper.toDTO(order);
    }

    /**
     * 查询用户订单列表
     */
    @Transactional(readOnly = true)
    public Page<OrderInfoDTO> getUserOrders(String userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createTime").descending());
        Page<OrderAggregate> orders = orderRepository.findByUserId(new UserId(userId), pageRequest);

        return orders.map(OrderMapper::toDTO);
    }

    private OrderAggregate findOrder(String orderId) {
        return orderRepository.findById(new OrderId(orderId))
            .orElseThrow(() -> new BusinessException("订单不存在"));
    }

    private List<OrderItem> validateAndCreateItems(List<OrderCreateRequest.OrderItem> itemRequests) {
        return itemRequests.stream()
            .map(item -> {
                Product product = ProductRepository.findById(new ProductId(item.getProductId()))
                    .orElseThrow(() -> new BusinessException("商品不存在: " + item.getProductId()));

                if (!product.isAvailable()) {
                    throw new BusinessException("商品已下架: " + item.getProductId());
                }

                return new OrderItem(
                    product.getId(),
                    item.getQuantity(),
                    product.getPrice()
                );
            })
            .collect(Collectors.toList());
    }

    private Money calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::getSubtotal)
            .reduce(Money.ZERO, Money::add);
    }

    private String generateOrderNumber() {
        // 格式：订单日期(8位) + 序号(6位)
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%06d", System.currentTimeMillis() % 1000000);
        return dateStr + sequence;
    }
}

/**
 * 订单聚合
 */
public class OrderAggregate {

    private OrderId orderId;
    private OrderNumber orderNumber;
    private UserId userId;
    private OrderStatus status;
    private Money totalAmount;
    private List<OrderItem> items;
    private ShippingAddress shippingAddress;
    private PaymentInfo paymentInfo;
    private LogisticsInfo logisticsInfo;
    private DateTime createTime;
    private DateTime updateTime;

    // 聚合根方法
    public void confirmOrder() {
        if (this.status != OrderStatus.PENDING) {
            throw new BusinessException("订单状态不正确");
        }

        this.status = OrderStatus.CONFIRMED;
        this.updateTime = DateTime.now();

        // 扣减库存
        this.items.forEach(item -> {
            InventoryService.confirmInventory(item.getProductId(), item.getQuantity());
        });
    }

    public void cancelOrder() {
        if (this.status == OrderStatus.DELIVERED ||
            this.status == OrderStatus.COMPLETED) {
            throw new BusinessException("订单已发货或完成，无法取消");
        }

        this.status = OrderStatus.CANCELLED;
        this.updateTime = DateTime.now();

        // 释放库存
        this.items.forEach(item -> {
            InventoryService.releaseInventory(item.getProductId(), item.getQuantity());
        });
    }
}
```

#### 订单中台API设计

```java
/**
 * 订单中台API
 */
@RestController
@RequestMapping("/api/middle-platform/order")
@Api(tags = "订单中台")
@Validated
public class OrderMiddlePlatformController {

    @Autowired
    private OrderMiddlePlatformService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    @ApiOperation("创建订单")
    public Result<OrderCreateResult> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        OrderCreateResult result = orderService.createOrder(request);
        return Result.success(result);
    }

    /**
     * 确认订单
     */
    @PostMapping("/{orderId}/confirm")
    @ApiOperation("确认订单")
    public Result<Void> confirmOrder(@PathVariable String orderId) {
        orderService.confirmOrder(orderId);
        return Result.success();
    }

    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    @ApiOperation("取消订单")
    public Result<Void> cancelOrder(@PathVariable String orderId,
                                    @RequestParam String reason) {
        orderService.cancelOrder(orderId, reason);
        return Result.success();
    }

    /**
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    @ApiOperation("查询订单详情")
    public Result<OrderInfoDTO> getOrderInfo(@PathVariable String orderId) {
        OrderInfoDTO orderInfo = orderService.getOrderInfo(orderId);
        return Result.success(orderInfo);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/user/{userId}")
    @ApiOperation("查询用户订单列表")
    public Result<Page<OrderInfoDTO>> getUserOrders(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderInfoDTO> orders = orderService.getUserOrders(userId, page, size);
        return Result.success(orders);
    }

    /**
     * 批量查询订单
     */
    @PostMapping("/batch")
    @ApiOperation("批量查询订单")
    public Result<List<OrderInfoDTO>> batchGetOrders(@RequestBody List<String> orderIds) {
        List<OrderInfoDTO> orders = orderService.batchGetOrders(orderIds);
        return Result.success(orders);
    }
}
```

### 3. 支付中台

```java
/**
 * 支付中台核心服务
 */
@Service
@Validated
public class PaymentMiddlePlatformService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentGatewayAdapter paymentGatewayAdapter;

    @Autowired
    private EventBus eventBus;

    /**
     * 创建支付订单
     */
    @Transactional
    public PaymentCreateResult createPayment(PaymentCreateRequest request) {
        // 1. 验证订单
        OrderAggregate order = OrderRepository.findById(new OrderId(request.getOrderId()))
            .orElseThrow(() -> new BusinessException("订单不存在"));

        // 2. 检查订单状态
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("订单状态不正确");
        }

        // 3. 创建支付订单
        PaymentAggregate payment = new PaymentAggregate(
            new PaymentId(),
            new PaymentNumber(generatePaymentNumber()),
            new OrderId(request.getOrderId()),
            new UserId(request.getUserId()),
            order.getTotalAmount(),
            PaymentMethod.valueOf(request.getPaymentMethod())
        );

        // 4. 保存支付订单
        paymentRepository.save(payment);

        // 5. 调用支付网关
        PaymentGatewayResponse response = paymentGatewayAdapter.createPayment(
            payment.getPaymentNumber().getValue(),
            order.getTotalAmount(),
            request.getPaymentMethod(),
            request.getCallbackUrl()
        );

        // 6. 更新支付信息
        if (response.isSuccess()) {
            payment.markAsProcessing(response.getGatewayPaymentId());
            paymentRepository.save(payment);
        }

        return new PaymentCreateResult(
            payment.getPaymentId().getValue(),
            response.getPaymentUrl(),
            response.getQrCode()
        );
    }

    /**
     * 处理支付回调
     */
    @Transactional
    public void handlePaymentCallback(PaymentCallbackRequest request) {
        // 1. 验证回调
        if (!paymentGatewayAdapter.verifyCallback(request)) {
            throw new BusinessException("支付回调验证失败");
        }

        // 2. 查找支付订单
        PaymentAggregate payment = paymentRepository
            .findByGatewayPaymentId(request.getGatewayPaymentId())
            .orElseThrow(() -> new BusinessException("支付订单不存在"));

        // 3. 处理支付结果
        if ("SUCCESS".equals(request.getStatus())) {
            handlePaymentSuccess(payment, request);
        } else if ("FAILED".equals(request.getStatus())) {
            handlePaymentFailed(payment, request);
        }

        paymentRepository.save(payment);
    }

    /**
     * 查询支付状态
     */
    @Transactional(readOnly = true)
    public PaymentInfoDTO getPaymentInfo(String paymentId) {
        PaymentAggregate payment = paymentRepository.findById(new PaymentId(paymentId))
            .orElseThrow(() -> new BusinessException("支付订单不存在"));

        return PaymentMapper.toDTO(payment);
    }

    private void handlePaymentSuccess(PaymentAggregate payment, PaymentCallbackRequest request) {
        payment.markAsCompleted();

        // 更新订单状态
        OrderAggregate order = OrderRepository.findById(payment.getOrderId())
            .orElseThrow(() -> new BusinessException("订单不存在"));
        order.markAsPaid();
        OrderRepository.save(order);

        // 发送支付成功事件
        eventBus.publish(new PaymentSuccessEvent(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getAmount()
        ));
    }

    private void handlePaymentFailed(PaymentAggregate payment, PaymentCallbackRequest request) {
        payment.markAsFailed(request.getFailureReason());

        // 发送支付失败事件
        eventBus.publish(new PaymentFailedEvent(
            payment.getPaymentId(),
            payment.getOrderId(),
            request.getFailureReason()
        ));
    }

    private String generatePaymentNumber() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String sequence = String.format("%08d", System.currentTimeMillis() % 100000000);
        return "PAY" + dateStr + sequence;
    }
}

/**
 * 支付聚合
 */
public class PaymentAggregate {

    private PaymentId paymentId;
    private PaymentNumber paymentNumber;
    private OrderId orderId;
    private UserId userId;
    private Money amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String gatewayPaymentId;
    private String failureReason;
    private DateTime createTime;
    private DateTime updateTime;

    public void markAsProcessing(String gatewayPaymentId) {
        this.status = PaymentStatus.PROCESSING;
        this.gatewayPaymentId = gatewayPaymentId;
        this.updateTime = DateTime.now();
    }

    public void markAsCompleted() {
        this.status = PaymentStatus.COMPLETED;
        this.updateTime = DateTime.now();

        // 发布领域事件
        DomainEventPublisher.publish(new PaymentCompletedEvent(paymentId, orderId, amount));
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.updateTime = DateTime.now();
    }
}
```

#### 支付中台API设计

```java
/**
 * 支付中台API
 */
@RestController
@RequestMapping("/api/middle-platform/payment")
@Api(tags = "支付中台")
@Validated
public class PaymentMiddlePlatformController {

    @Autowired
    private PaymentMiddlePlatformService paymentService;

    /**
     * 创建支付订单
     */
    @PostMapping
    @ApiOperation("创建支付订单")
    public Result<PaymentCreateResult> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentCreateResult result = paymentService.createPayment(request);
        return Result.success(result);
    }

    /**
     * 支付回调
     */
    @PostMapping("/callback")
    @ApiOperation("支付回调")
    public Result<Void> paymentCallback(@RequestBody PaymentCallbackRequest request) {
        paymentService.handlePaymentCallback(request);
        return Result.success();
    }

    /**
     * 查询支付状态
     */
    @GetMapping("/{paymentId}")
    @ApiOperation("查询支付状态")
    public Result<PaymentInfoDTO> getPaymentInfo(@PathVariable String paymentId) {
        PaymentInfoDTO paymentInfo = paymentService.getPaymentInfo(paymentId);
        return Result.success(paymentInfo);
    }

    /**
     * 退款申请
     */
    @PostMapping("/{paymentId}/refund")
    @ApiOperation("退款申请")
    public Result<Void> refund(@PathVariable String paymentId, @RequestParam String reason) {
        paymentService.refund(paymentId, reason);
        return Result.success();
    }
}
```

---

## 📊 数据模型设计

### 1. 领域模型 → 数据模型映射

```java
/**
 * 用户实体映射
 * 领域模型 → 数据模型
 */
@Entity
@Table(name = "bm_user")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    /**
     * 用户ID
     */
    @Id
    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 50, unique = true)
    private String username;

    /**
     * 邮箱
     */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 密码哈希
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    // 构造函数
    public UserEntity() {
        this.userId = UUID.randomUUID().toString().replace("-", "");
        this.status = UserStatus.ACTIVE;
        this.createTime = LocalDateTime.now();
    }

    /**
     * 从领域模型转换
     */
    public static UserEntity fromDomain(User user) {
        UserEntity entity = new UserEntity();
        entity.userId = user.getId().getValue();
        entity.username = user.getUsername();
        entity.email = user.getEmail();
        entity.phone = user.getPhone().toString();
        entity.passwordHash = user.getPasswordHash();
        entity.status = user.getStatus();
        return entity;
    }

    /**
     * 转换为领域模型
     */
    public User toDomain() {
        User user = new User(
            new UserId(userId),
            username,
            email,
            new PhoneNumber(phone)
        );

        // 设置密码哈希
        if (passwordHash != null) {
            user.setPasswordHash(passwordHash);
        }

        return user;
    }
}

/**
 * 订单实体映射
 */
@Entity
@Table(name = "bm_order")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderEntity extends BaseEntity {

    /**
     * 订单ID
     */
    @Id
    @Column(name = "order_id", nullable = false, length = 32)
    private String orderId;

    /**
     * 订单号
     */
    @Column(name = "order_number", nullable = false, length = 32, unique = true)
    private String orderNumber;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 32)
    private String userId;

    /**
     * 订单状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /**
     * 订单总金额
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 支付时间
     */
    @Column(name = "paid_time")
    private LocalDateTime paidTime;

    /**
     * 发货时间
     */
    @Column(name = "shipped_time")
    private LocalDateTime shippedTime;

    /**
     * 订单项
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items;

    /**
     * 收货地址
     */
    @Embedded
    private ShippingAddressEntity shippingAddress;
}

/**
 * 订单项实体
 */
@Entity
@Table(name = "bm_order_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItemEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "product_id", nullable = false, length = 32)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
```

### 2. 数据仓储实现

```java
/**
 * 用户仓储实现
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    @Autowired
    private UserEntityMapper userEntityMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public Optional<User> findById(UserId id) {
        UserEntity entity = userEntityMapper.selectById(id.getValue());
        return Optional.ofNullable(entity).map(this::convertToDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        UserEntity entity = userEntityMapper.selectByUsername(username);
        return Optional.ofNullable(entity).map(this::convertToDomain);
    }

    @Override
    public void save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);

        if (user.getId() == null) {
            // 新增
            userEntityMapper.insert(entity);
        } else {
            // 更新
            entity.setUpdateTime(LocalDateTime.now());
            userEntityMapper.updateById(entity);
        }
    }

    @Override
    public void delete(UserId id) {
        userEntityMapper.deleteById(id.getValue());
    }

    @Override
    public Page<User> findAll(int page, int size) {
        Page<UserEntity> pageRequest = PageRequest.of(page, size,
            Sort.by(Sort.Direction.DESC, "createTime"));
        Page<UserEntity> entityPage = userEntityMapper.selectPage(pageRequest, null);

        return entityPage.map(this::convertToDomain);
    }

    @Override
    public List<User> findBySpecification(UserSpecification spec) {
        UserEntityExample example = new UserEntityExample();
        UserEntityExample.Criteria criteria = example.createCriteria();

        if (spec.hasStatus()) {
            criteria.andStatusEqualTo(spec.getStatus().name());
        }

        if (spec.hasCreateTimeRange()) {
            criteria.andCreateTimeBetween(spec.getStartTime(), spec.getEndTime());
        }

        List<UserEntity> entities = userEntityMapper.selectByExample(example);
        return entities.stream()
            .map(this::convertToDomain)
            .collect(Collectors.toList());
    }

    private User convertToDomain(UserEntity entity) {
        return UserEntity.toDomain(entity);
    }
}
```

### 3. 数据访问层设计

```java
/**
 * 用户实体映射器
 */
@Mapper
public interface UserEntityMapper extends BaseMapper<UserEntity> {

    /**
     * 根据用户名查询
     */
    @Select("SELECT * FROM bm_user WHERE username = #{username}")
    UserEntity selectByUsername(@Param("username") String username);

    /**
     * 根据邮箱查询
     */
    @Select("SELECT * FROM bm_user WHERE email = #{email}")
    UserEntity selectByEmail(@Param("email") String email);

    /**
     * 根据手机号查询
     */
    @Select("SELECT * FROM bm_user WHERE phone = #{phone}")
    UserEntity selectByPhone(@Param("phone") String phone);

    /**
     * 分页查询用户
     */
    @Select("<script>" +
            "SELECT * FROM bm_user" +
            "<where>" +
            "<if test='status != null'>AND status = #{status}</if>" +
            "<if test='createTimeStart != null'>AND create_time &gt;= #{createTimeStart}</if>" +
            "<if test='createTimeEnd != null'>AND create_time &lt;= #{createTimeEnd}</if>" +
            "</where>" +
            "ORDER BY create_time DESC" +
            "</script>")
    IPage<UserEntity> selectPage(IPage<UserEntity> page,
                                  @Param("status") String status,
                                  @Param("createTimeStart") LocalDateTime startTime,
                                  @Param("createTimeEnd") LocalDateTime endTime);

    /**
     * 批量查询用户
     */
    @Select("<script>" +
            "SELECT * FROM bm_user WHERE user_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<UserEntity> selectBatchByIds(@Param("ids") List<String> ids);

    /**
     * 更新用户状态
     */
    @Update("UPDATE bm_user SET status = #{status}, update_time = NOW() WHERE user_id = #{userId}")
    int updateStatus(@Param("userId") String userId, @Param("status") String status);

    /**
     * 更新最后登录时间
     */
    @Update("UPDATE bm_user SET last_login_time = NOW() WHERE user_id = #{userId}")
    int updateLastLoginTime(@Param("userId") String userId);
}

/**
 * 订单实体映射器
 */
@Mapper
public interface OrderEntityMapper extends BaseMapper<OrderEntity> {

    /**
     * 根据用户ID查询订单
     */
    @Select("SELECT * FROM bm_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    IPage<OrderEntity> selectByUserId(IPage<OrderEntity> page, @Param("userId") String userId);

    /**
     * 根据订单号查询
     */
    @Select("SELECT * FROM bm_order WHERE order_number = #{orderNumber}")
    OrderEntity selectByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 根据状态查询订单数量
     */
    @Select("SELECT COUNT(*) FROM bm_order WHERE status = #{status}")
    int countByStatus(@Param("status") String status);

    /**
     * 更新订单状态
     */
    @Update("<script>" +
            "UPDATE bm_order SET " +
            "<choose>" +
            "<when test='status == \"PAID\"'>paid_time = NOW(),</when>" +
            "<when test='status == \"SHIPPED\"'>shipped_time = NOW(),</when>" +
            "</choose>" +
            "status = #{status}, update_time = NOW() " +
            "WHERE order_id = #{orderId}" +
            "</script>")
    int updateStatus(@Param("orderId") String orderId, @Param("status") String status);

    /**
     * 查询过期未支付订单
     */
    @Select("SELECT * FROM bm_order WHERE status = 'PENDING' AND create_time < #{expireTime}")
    List<OrderEntity> selectExpiredUnpaidOrders(@Param("expireTime") LocalDateTime expireTime);
}

/**
 * 用户角色关联映射器
 */
@Mapper
public interface UserRoleMapper {

    /**
     * 查询用户角色
     */
    @Select("SELECT r.* FROM bm_role r " +
            "JOIN bm_user_role ur ON r.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<RoleEntity> selectRolesByUserId(@Param("userId") String userId);

    /**
     * 分配角色
     */
    @Insert("INSERT INTO bm_user_role (user_id, role_id, create_time) VALUES (#{userId}, #{roleId}, NOW())")
    int assignRole(@Param("userId") String userId, @Param("roleId") String roleId);

    /**
     * 移除角色
     */
    @Delete("DELETE FROM bm_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int removeRole(@Param("userId") String userId, @Param("roleId") String roleId);

    /**
     * 清空用户所有角色
     */
    @Delete("DELETE FROM bm_user_role WHERE user_id = #{userId}")
    int clearUserRoles(@Param("userId") String userId);
}
```

---

## 🔌 中台服务间集成

### 1. 事件驱动架构

```java
/**
 * 领域事件发布器
 */
@Component
public class DomainEventPublisher {

    private static final EventBus EVENT_BUS = new EventBus();

    /**
     * 发布领域事件
     */
    public static void publish(DomainEvent event) {
        EVENT_BUS.post(event);
    }

    /**
     * 异步发布领域事件
     */
    public static void publishAsync(DomainEvent event) {
        CompletableFuture.runAsync(() -> publish(event));
    }

    /**
     * 订阅领域事件
     */
    public static void subscribe(Object subscriber) {
        EVENT_BUS.register(subscriber);
    }
}

/**
 * 领域事件处理器
 */
@Component
public class DomainEventHandler {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private StatisticsService statisticsService;

    /**
     * 处理用户注册事件
     */
    @Subscribe
    public void handleUserRegistered(UserRegisteredEvent event) {
        // 发送欢迎邮件
        notificationService.sendWelcomeEmail(event.getUserId(), event.getEmail());

        // 创建用户画像
        userProfileService.createProfile(event.getUserId());

        // 统计指标
        statisticsService.incrementUserRegisteredCount();
    }

    /**
     * 处理订单创建事件
     */
    @Subscribe
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 预占库存
        inventoryService.confirmReservation(event.getOrderId());

        // 发送订单确认短信
        notificationService.sendOrderCreatedSMS(event.getUserId(), event.getOrderId());

        // 统计指标
        statisticsService.incrementOrderCreatedCount(event.getAmount());
    }

    /**
     * 处理支付成功事件
     */
    @Subscribe
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        // 更新订单状态
        OrderAggregate order = OrderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new BusinessException("订单不存在"));
        order.markAsPaid();
        OrderRepository.save(order);

        // 扣减库存
        inventoryService.deductInventory(event.getOrderId());

        // 发送支付成功通知
        notificationService.sendPaymentSuccessSMS(event.getUserId(), event.getAmount());

        // 会员积分奖励
        userProfileService.addPoints(event.getUserId(), event.getAmount().getValue().intValue() / 10);
    }
}
```

### 2. 防腐层设计

```java
/**
 * 防腐层 - 适配外部系统
 */
@Component
public class ExternalSystemAdapter {

    @Autowired
    private InventoryFeignClient inventoryFeignClient;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private LogisticsFeignClient logisticsFeignClient;

    /**
     * 查询商品信息
     */
    public ProductInfo queryProduct(String productId) {
        try {
            ProductFeignResponse response = productFeignClient.getProduct(productId);

            if (response.isSuccess()) {
                return ProductInfo.builder()
                    .productId(response.getData().getProductId())
                    .productName(response.getData().getProductName())
                    .price(response.getData().getPrice())
                    .stock(response.getData().getStock())
                    .isAvailable(response.getData().getStock() > 0)
                    .build();
            } else {
                throw new BusinessException("查询商品信息失败: " + response.getMessage());
            }
        } catch (Exception e) {
            // 降级处理
            log.warn("查询商品信息失败，使用缓存数据", e);
            return getProductFromCache(productId);
        }
    }

    /**
     * 扣减库存
     */
    public boolean deductInventory(String productId, int quantity) {
        try {
            InventoryDeductRequest request = new InventoryDeductRequest();
            request.setProductId(productId);
            request.setQuantity(quantity);

            InventoryFeignResponse response = inventoryFeignClient.deductInventory(request);

            return response.isSuccess();
        } catch (Exception e) {
            log.error("扣减库存失败", e);
            return false;
        }
    }

    /**
     * 创建物流订单
     */
    public LogisticsInfo createLogisticsOrder(LogisticsCreateRequest request) {
        try {
            LogisticsFeignResponse response = logisticsFeignClient.createOrder(request);

            if (response.isSuccess()) {
                return LogisticsInfo.builder()
                    .logisticsId(response.getData().getLogisticsId())
                    .trackingNumber(response.getData().getTrackingNumber())
                    .company(response.getData().getCompany())
                    .estimatedDeliveryTime(response.getData().getEstimatedDeliveryTime())
                    .build();
            } else {
                throw new BusinessException("创建物流订单失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("创建物流订单失败", e);
            throw new BusinessException("物流服务异常，请稍后重试");
        }
    }

    private ProductInfo getProductFromCache(String productId) {
        // 从缓存获取商品信息
        return RedisCache.get("product:" + productId, ProductInfo.class);
    }
}

/**
 * 防腐层拦截器
 * 防止外部系统变化影响核心领域
 */
@Around("execution(* com.basebackend.application.service..*Service.*(..))")
public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    // 记录调用开始时间
    long startTime = System.currentTimeMillis();

    try {
        // 参数校验
        validateParameters(joinPoint.getArgs());

        // 执行目标方法
        Object result = joinPoint.proceed();

        // 记录成功日志
        log.info("方法调用成功: {}, 耗时: {}ms",
            joinPoint.getSignature().getName(),
            System.currentTimeMillis() - startTime);

        return result;
    } catch (Exception e) {
        // 记录错误日志
        log.error("方法调用失败: {}, 错误: {}",
            joinPoint.getSignature().getName(),
            e.getMessage(), e);

        throw e;
    }
}
```

### 3. API网关集成

```java
/**
 * 中台服务网关路由配置
 */
@Configuration
public class MiddlePlatformGatewayConfig {

    @Bean
    public RouteLocator middlePlatformRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 用户中台路由
            .route("user-middle-platform", r -> r
                .path("/api/middle-platform/user/**")
                .filters(f -> f.stripPrefix(3))
                .uri("lb://user-middle-platform"))

            // 订单中台路由
            .route("order-middle-platform", r -> r
                .path("/api/middle-platform/order/**")
                .filters(f -> f.stripPrefix(3))
                .uri("lb://order-middle-platform"))

            // 支付中台路由
            .route("payment-middle-platform", r -> r
                .path("/api/middle-platform/payment/**")
                .filters(f -> f.stripPrefix(3))
                .uri("lb://payment-middle-platform"))

            // 商品中台路由
            .route("product-middle-platform", r -> r
                .path("/api/middle-platform/product/**")
                .filters(f -> f.stripPrefix(3))
                .uri("lb://product-middle-platform"))

            // 营销中台路由
            .route("promotion-middle-platform", r -> r
                .path("/api/middle-platform/promotion/**")
                .filters(f -> f.stripPrefix(3))
                .uri("lb://promotion-middle-platform"))

            // 通知中台路由
            .route("notification-middle-platform", r -> r
                .path("/api/middle-platform/notification/**")
                .filters(f -> f.stripPrefix(3))
                .uri("lb://notification-middle-platform"))

            .build();
    }

    /**
     * 全局限流配置
     */
    @Bean
    public KeyResolver userIdKeyResolver() {
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getHeaders().getFirst("X-User-Id")
        );
    }
}

/**
 * 中台服务负载均衡配置
 */
@Configuration
public class MiddlePlatformLoadBalancerConfig {

    /**
     * 用户中台服务配置
     */
    @LoadBalancerClient(name = "user-middle-platform",
        configuration = UserMiddlePlatformLoadBalancerConfig.class)
    public interface UserMiddlePlatformLoadBalancerConfig {

    }

    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDnsRoundRobinInDiscovery()
            .withSameInstanceReplicaServiceFilter()
            .build(context);
    }
}
```

---

## 📱 搜索中台

### 1. 搜索引擎服务

```java
/**
 * 搜索中台核心服务
 */
@Service
@Validated
public class SearchMiddlePlatformService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Autowired
    private SearchIndexManager searchIndexManager;

    /**
     * 创建搜索索引
     */
    public void createIndex(String indexName, SearchIndexConfig config) {
        searchIndexManager.createIndex(indexName, config);
    }

    /**
     * 索引商品
     */
    @Transactional
    public void indexProduct(ProductInfo product) {
        try {
            // 构建索引文档
            ProductSearchDocument document = ProductSearchDocument.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .brand(product.getBrand())
                .price(product.getPrice())
                .description(product.getDescription())
                .tags(product.getTags())
                .searchKeywords(generateSearchKeywords(product))
                .build();

            // 索引到ES
            elasticsearchTemplate.save(document, "products");

        } catch (Exception e) {
            log.error("索引商品失败: {}", product.getProductId(), e);
            throw new BusinessException("搜索索引失败");
        }
    }

    /**
     * 搜索商品
     */
    @Transactional(readOnly = true)
    public SearchResult<ProductSearchResult> searchProducts(SearchRequest request) {
        try {
            // 构建查询条件
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

            // 关键词搜索
            if (StringUtils.hasText(request.getKeyword())) {
                queryBuilder.should(QueryBuilders.matchQuery("productName", request.getKeyword()));
                queryBuilder.should(QueryBuilders.matchQuery("description", request.getKeyword()));
                queryBuilder.should(QueryBuilders.termQuery("tags", request.getKeyword()));
            }

            // 分类过滤
            if (StringUtils.hasText(request.getCategoryId())) {
                queryBuilder.filter(QueryBuilders.termQuery("categoryId", request.getCategoryId()));
            }

            // 价格区间过滤
            if (request.getMinPrice() != null || request.getMaxPrice() != null) {
                RangeQueryBuilder priceQuery = QueryBuilders.rangeQuery("price");
                if (request.getMinPrice() != null) {
                    priceQuery.gte(request.getMinPrice());
                }
                if (request.getMaxPrice() != null) {
                    priceQuery.lte(request.getMaxPrice());
                }
                queryBuilder.filter(priceQuery);
            }

            // 构建搜索源
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(queryBuilder);

            // 分页
            searchSourceBuilder.from(request.getPage() * request.getSize());
            searchSourceBuilder.size(request.getSize());

            // 排序
            if (StringUtils.hasText(request.getSortBy())) {
                SortOrder sortOrder = request.getSortOrder() != null ?
                    request.getSortOrder() : SortOrder.DESC;
                searchSourceBuilder.sort(request.getSortBy(), sortOrder);
            } else {
                searchSourceBuilder.sort("_score", SortOrder.DESC);
            }

            // 高亮显示
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("productName");
            highlightBuilder.field("description");
            highlightBuilder.preTags("<font color='red'>");
            highlightBuilder.postTags("</font>");
            searchSourceBuilder.highlighter(highlightBuilder);

            // 执行搜索
            SearchRequest searchRequest = new SearchRequest("products");
            searchRequest.source(searchSourceBuilder);

            SearchResponse response = elasticsearchTemplate.search(searchRequest, RequestOptions.DEFAULT);

            // 解析结果
            return parseSearchResponse(response, request);

        } catch (Exception e) {
            log.error("搜索商品失败", e);
            throw new BusinessException("搜索服务异常");
        }
    }

    /**
     * 智能推荐
     */
    @Transactional(readOnly = true)
    public List<ProductSearchResult> recommendProducts(String userId, int size) {
        try {
            // 获取用户历史浏览记录
            List<String> historyProducts = getUserBrowseHistory(userId);

            // 基于用户画像的推荐
            UserProfile userProfile = getUserProfile(userId);

            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

            // 推荐相似商品
            if (!historyProducts.isEmpty()) {
                queryBuilder.should(QueryBuilders.termsQuery("productId", historyProducts));
            }

            // 基于用户偏好的推荐
            if (userProfile != null && userProfile.hasPreferences()) {
                queryBuilder.should(QueryBuilders.termsQuery("categoryId", userProfile.getFavoriteCategories()));
                queryBuilder.should(QueryBuilders.termsQuery("brand", userProfile.getFavoriteBrands()));
            }

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(queryBuilder);
            searchSourceBuilder.size(size);

            SearchRequest searchRequest = new SearchRequest("products");
            searchRequest.source(searchSourceBuilder);

            SearchResponse response = elasticsearchTemplate.search(searchRequest, RequestOptions.DEFAULT);

            return parseRecommendResponse(response);

        } catch (Exception e) {
            log.error("商品推荐失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 搜索统计
     */
    @Transactional(readOnly = true)
    public SearchStatistics getSearchStatistics() {
        try {
            // 搜索量统计
            long todaySearchCount = getTodaySearchCount();
            long weekSearchCount = getWeekSearchCount();
            long monthSearchCount = getMonthSearchCount();

            // 热门搜索词
            List<SearchKeywordStats> hotKeywords = getHotKeywords(10);

            // 热门分类
            List<CategoryStats> hotCategories = getHotCategories(10);

            // 热门商品
            List<ProductStats> hotProducts = getHotProducts(10);

            return SearchStatistics.builder()
                .todaySearchCount(todaySearchCount)
                .weekSearchCount(weekSearchCount)
                .monthSearchCount(monthSearchCount)
                .hotKeywords(hotKeywords)
                .hotCategories(hotCategories)
                .hotProducts(hotProducts)
                .build();

        } catch (Exception e) {
            log.error("获取搜索统计失败", e);
            return SearchStatistics.builder().build();
        }
    }

    private SearchResult<ProductSearchResult> parseSearchResponse(SearchResponse response,
                                                                   SearchRequest request) {
        List<ProductSearchResult> results = new ArrayList<>();

        for (SearchHit hit : response.getHits().getHits()) {
            try {
                ProductSearchDocument document = JSON.parseObject(
                    hit.getSourceAsString(),
                    ProductSearchDocument.class
                );

                ProductSearchResult result = ProductSearchResult.builder()
                    .productId(document.getProductId())
                    .productName(document.getProductName())
                    .categoryName(document.getCategoryName())
                    .brand(document.getBrand())
                    .price(document.getPrice())
                    .description(document.getDescription())
                    .tags(document.getTags())
                    .score(hit.getScore())
                    .build();

                // 高亮显示
                if (hit.getHighlightFields() != null) {
                    Highlight productNameHighlight = hit.getHighlightFields().get("productName");
                    if (productNameHighlight != null && !productNameHighlight.getFragments().isEmpty()) {
                        result.setProductNameHighlight(productNameHighlight.getFragments().get(0).string());
                    }

                    Highlight descHighlight = hit.getHighlightFields().get("description");
                    if (descHighlight != null && !descHighlight.getFragments().isEmpty()) {
                        result.setDescriptionHighlight(descHighlight.getFragments().get(0).string());
                    }
                }

                results.add(result);

            } catch (Exception e) {
                log.warn("解析搜索结果失败", e);
            }
        }

        return SearchResult.<ProductSearchResult>builder()
            .total(response.getHits().getTotalHits().value)
            .results(results)
            .page(request.getPage())
            .size(request.getSize())
            .build();
    }
}
```

### 2. 搜索中台API

```java
/**
 * 搜索中台API
 */
@RestController
@RequestMapping("/api/middle-platform/search")
@Api(tags = "搜索中台")
@Validated
public class SearchMiddlePlatformController {

    @Autowired
    private SearchMiddlePlatformService searchService;

    /**
     * 搜索商品
     */
    @PostMapping("/products")
    @ApiOperation("搜索商品")
    public Result<SearchResult<ProductSearchResult>> searchProducts(
            @Valid @RequestBody SearchRequest request) {
        SearchResult<ProductSearchResult> result = searchService.searchProducts(request);
        return Result.success(result);
    }

    /**
     * 智能推荐
     */
    @GetMapping("/recommend")
    @ApiOperation("智能推荐")
    public Result<List<ProductSearchResult>> recommendProducts(
            @RequestParam String userId,
            @RequestParam(defaultValue = "10") int size) {
        List<ProductSearchResult> result = searchService.recommendProducts(userId, size);
        return Result.success(result);
    }

    /**
     * 搜索统计
     */
    @GetMapping("/statistics")
    @ApiOperation("搜索统计")
    public Result<SearchStatistics> getSearchStatistics() {
        SearchStatistics result = searchService.getSearchStatistics();
        return Result.success(result);
    }

    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    @ApiOperation("获取搜索建议")
    public Result<List<String>> getSearchSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int size) {
        List<String> suggestions = searchService.getSearchSuggestions(keyword, size);
        return Result.success(suggestions);
    }

    /**
     * 索引商品
     */
    @PostMapping("/index/product")
    @ApiOperation("索引商品")
    public Result<Void> indexProduct(@Valid @RequestBody ProductInfo product) {
        searchService.indexProduct(product);
        return Result.success();
    }

    /**
     * 批量索引商品
     */
    @PostMapping("/index/product/batch")
    @ApiOperation("批量索引商品")
    public Result<Void> batchIndexProducts(@Valid @RequestBody List<ProductInfo> products) {
        products.forEach(searchService::indexProduct);
        return Result.success();
    }
}
```

---

## 🔐 权限与安全

### 1. 权限中台

```java
/**
 * 权限中台核心服务
 */
@Service
@Validated
public class PermissionMiddlePlatformService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 创建角色
     */
    @Transactional
    public RoleId createRole(RoleCreateRequest request) {
        // 检查角色名称是否已存在
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException("角色名称已存在");
        }

        Role role = new Role(
            new RoleId(),
            request.getName(),
            request.getDescription(),
            request.getPermissions()
        );

        roleRepository.save(role);

        return role.getId();
    }

    /**
     * 分配角色权限
     */
    @Transactional
    public void assignRolePermissions(String roleId, List<String> permissionIds) {
        Role role = roleRepository.findById(new RoleId(roleId))
            .orElseThrow(() -> new BusinessException("角色不存在"));

        List<Permission> permissions = permissionRepository.findByIds(
            permissionIds.stream()
                .map(PermissionId::new)
                .collect(Collectors.toList())
        );

        role.setPermissions(permissions);
        roleRepository.save(role);

        // 清除用户权限缓存
        clearUserPermissionCache(roleId);
    }

    /**
     * 检查用户权限
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(String userId, String permissionCode) {
        // 从缓存获取用户权限
        Set<String> userPermissions = getUserPermissionCache(userId);

        if (userPermissions.contains(permissionCode)) {
            return true;
        }

        // 缓存中没有，查询数据库
        List<Permission> permissions = permissionRepository.findByUserId(new UserId(userId));

        Set<String> permissionCodes = permissions.stream()
            .map(Permission::getCode)
            .collect(Collectors.toSet());

        // 缓存权限
        setUserPermissionCache(userId, permissionCodes);

        return permissionCodes.contains(permissionCode);
    }

    /**
     * 获取用户所有权限
     */
    @Transactional(readOnly = true)
    public Set<String> getUserPermissions(String userId) {
        Set<String> cachedPermissions = getUserPermissionCache(userId);

        if (!cachedPermissions.isEmpty()) {
            return cachedPermissions;
        }

        List<Permission> permissions = permissionRepository.findByUserId(new UserId(userId));

        Set<String> permissionCodes = permissions.stream()
            .map(Permission::getCode)
            .collect(Collectors.toSet());

        setUserPermissionCache(userId, permissionCodes);

        return permissionCodes;
    }

    /**
     * 检查用户角色
     */
    @Transactional(readOnly = true)
    public boolean hasRole(String userId, String roleName) {
        return roleRepository.existsByUserIdAndRoleName(new UserId(userId), roleName);
    }

    private Set<String> getUserPermissionCache(String userId) {
        String cacheKey = "user:permissions:" + userId;
        return RedisCache.get(cacheKey, Set.class);
    }

    private void setUserPermissionCache(String userId, Set<String> permissions) {
        String cacheKey = "user:permissions:" + userId;
        RedisCache.set(cacheKey, permissions, Duration.ofHours(1));
    }

    private void clearUserPermissionCache(String roleId) {
        // 查找所有拥有该角色的用户，清除权限缓存
        List<UserId> userIds = roleRepository.findUserIdsByRoleId(new RoleId(roleId));
        userIds.forEach(userId -> {
            String cacheKey = "user:permissions:" + userId.getValue();
            RedisCache.delete(cacheKey);
        });
    }
}

/**
 * 权限校验注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限代码
     */
    String value();

    /**
     * 是否需要所有权限
     */
    boolean all() default false;
}

/**
 * 权限校验切面
 */
@Aspect
@Component
public class PermissionInterceptor {

    @Autowired
    private PermissionMiddlePlatformService permissionService;

    @Around("@annotation(requirePermission)")
    public Object around(ProceedingJoinPoint joinPoint, RequirePermission requirePermission) throws Throwable {
        // 从请求头获取用户ID
        String userId = RequestContextHolder.getRequestHeader("X-User-Id");

        if (StringUtils.isEmpty(userId)) {
            throw new UnauthorizedException("未获取到用户信息");
        }

        // 校验权限
        boolean hasPermission = permissionService.hasPermission(userId, requirePermission.value());

        if (!hasPermission) {
            throw new ForbiddenException("没有权限执行此操作");
        }

        return joinPoint.proceed();
    }
}
```

---

## 📊 中台监控与统计

### 1. 业务监控

```java
/**
 * 中台业务监控服务
 */
@Service
public class MiddlePlatformMonitoringService {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private TagCounter counter;

    /**
     * 记录业务指标
     */
    public void recordBusinessMetric(String metricName, String dimension, long value) {
        counter.increment(metricName, dimension, value);
    }

    /**
     * 记录用户注册
     */
    public void recordUserRegistered(String channel) {
        meterRegistry.counter("user.registered.total", "channel", channel).increment();
    }

    /**
     * 记录订单创建
     */
    public void recordOrderCreated(String orderType, BigDecimal amount) {
        meterRegistry.counter("order.created.total", "orderType", orderType).increment();
        meterRegistry.counter("order.created.amount", "orderType", orderType).increment(amount.doubleValue());
    }

    /**
     * 记录支付
     */
    public void recordPayment(String paymentMethod, String status, BigDecimal amount) {
        meterRegistry.counter("payment.total", "method", paymentMethod, "status", status).increment();
        meterRegistry.counter("payment.amount", "method", paymentMethod, "status", status).increment(amount.doubleValue());
    }

    /**
     * 记录搜索
     */
    public void recordSearch(String keyword, String categoryId, long resultCount) {
        meterRegistry.counter("search.total", "categoryId", categoryId).increment();
        meterRegistry.counter("search.resultCount", "categoryId", categoryId, "resultCount", String.valueOf(resultCount)).increment();
    }

    /**
     * 获取实时统计数据
     */
    @Transactional(readOnly = true)
    public RealtimeStats getRealtimeStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        return RealtimeStats.builder()
            .userRegisteredCount(getTodayUserRegisteredCount(todayStart, now))
            .orderCreatedCount(getTodayOrderCreatedCount(todayStart, now))
            .paymentAmount(getTodayPaymentAmount(todayStart, now))
            .searchCount(getTodaySearchCount(todayStart, now))
            .build();
    }

    /**
     * 生成业务报表
     */
    @Transactional(readOnly = true)
    public BusinessReport generateBusinessReport(DateRange range) {
        LocalDateTime startTime = range.getStartTime();
        LocalDateTime endTime = range.getEndTime();

        return BusinessReport.builder()
            .userStats(getUserStats(startTime, endTime))
            .orderStats(getOrderStats(startTime, endTime))
            .paymentStats(getPaymentStats(startTime, endTime))
            .revenueStats(getRevenueStats(startTime, endTime))
            .userRetentionStats(getUserRetentionStats(startTime, endTime))
            .conversionStats(getConversionStats(startTime, endTime))
            .build();
    }
}
```

---

## 📋 实施检查清单

### DDD领域建模
- [ ] 限界上下文划分完成
- [ ] 聚合根设计完成
- [ ] 领域事件定义完成
- [ ] 领域服务实现完成
- [ ] 仓储模式实现完成

### 用户中台
- [ ] 用户注册登录实现
- [ ] 权限管理实现
- [ ] 用户画像实现
- [ ] 会员体系实现
- [ ] API接口实现

### 订单中台
- [ ] 订单创建流程实现
- [ ] 订单状态管理实现
- [ ] 订单查询实现
- [ ] 订单统计实现
- [ ] API接口实现

### 支付中台
- [ ] 支付流程实现
- [ ] 支付回调处理
- [ ] 退款处理实现
- [ ] 对账功能实现
- [ ] API接口实现

### 商品中台
- [ ] 商品管理实现
- [ ] 类目管理实现
- [ ] 价格管理实现
- [ ] 库存管理实现
- [ ] API接口实现

### 搜索中台
- [ ] 搜索引擎搭建
- [ ] 索引管理实现
- [ ] 搜索功能实现
- [ ] 智能推荐实现
- [ ] API接口实现

### 营销中台
- [ ] 优惠券管理实现
- [ ] 促销活动实现
- [ ] 会员权益实现
- [ ] 分销体系实现
- [ ] API接口实现

### 通知中台
- [ ] 短信通知实现
- [ ] 邮件通知实现
- [ ] 推送通知实现
- [ ] 站内消息实现
- [ ] API接口实现

### 权限中台
- [ ] 角色管理实现
- [ ] 权限分配实现
- [ ] 权限校验实现
- [ ] 安全审计实现
- [ ] API接口实现

### 监控与统计
- [ ] 业务指标采集
- [ ] 实时数据统计
- [ ] 报表生成
- [ ] 告警配置
- [ ] Dashboard展示

---

**编制：** 浮浮酱 🐱（猫娘工程师）
**日期：** 2025-11-15
**状态：** 📋 指南完成，准备实施

**加油喵～ 业务中台建设即将完成！** ฅ'ω'ฅ
