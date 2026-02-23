# 测试数据管理最佳实践 (Test Data Management Best Practices)

## 📋 概述

测试数据管理是单元测试成功的关键因素。良好的测试数据管理能够提高测试的可读性、可维护性和可重复性。本文档总结了在basebackend项目中验证的测试数据管理最佳实践。

## 🎯 核心原则

### 1. 数据独立性
每个测试的数据应该是独立的，不依赖于其他测试的状态。

### 2. 数据真实性
测试数据应该尽可能模拟真实业务场景，但保持简单。

### 3. 数据可重用
相同的数据创建逻辑应该在多个测试中复用。

### 4. 数据可读性
测试数据的命名和结构应该清晰易懂。

## 🔧 测试数据创建模式

### 模式1: 构建器模式（推荐）

```java
// ✅ 使用构建器创建复杂的测试数据
public class UserTestBuilder {
    private String id = "test-user-id";
    private String name = "Test User";
    private String email = "test@example.com";
    private List<String> roles = new ArrayList<>();
    private LocalDateTime createdAt = LocalDateTime.now();

    private UserTestBuilder() {
    }

    public static UserTestBuilder aUser() {
        return new UserTestBuilder();
    }

    public UserTestBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public UserTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestBuilder withRoles(String... roles) {
        this.roles = Arrays.asList(roles);
        return this;
    }

    public UserTestBuilder withCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public User build() {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRoles(roles);
        user.setCreatedAt(createdAt);
        return user;
    }

    // ✅ 提供常用预设
    public UserTestBuilder adminUser() {
        return withId("admin-123")
                .withName("Admin User")
                .withEmail("admin@example.com")
                .withRoles("ADMIN", "USER");
    }

    public UserTestBuilder regularUser() {
        return withId("user-456")
                .withName("Regular User")
                .withEmail("user@example.com")
                .withRoles("USER");
    }
}

// ========== 使用示例 ==========
@Test
void testAdminUser() {
    User admin = UserTestBuilder.aUser()
            .adminUser()
            .build();

    assertEquals("admin-123", admin.getId());
    assertTrue(admin.getRoles().contains("ADMIN"));
}

@Test
void testRegularUser() {
    User user = UserTestBuilder.aUser()
            .regularUser()
            .build();

    assertEquals("user-456", user.getId());
    assertFalse(user.getRoles().contains("ADMIN"));
}

@Test
void testCustomUser() {
    User user = UserTestBuilder.aUser()
            .withId("custom-789")
            .withName("Custom User")
            .withEmail("custom@example.com")
            .withRoles("CUSTOM")
            .build();

    // 验证自定义数据...
}
```

### 模式2: 工厂方法模式

```java
// ✅ 简单的工厂方法
public class TestDataFactory {

    private TestDataFactory() {
        // 防止实例化
    }

    // ========== 用户数据 ==========
    public static User createUser() {
        User user = new User();
        user.setId("test-user-id");
        user.setName("Test User");
        user.setEmail("test@example.com");
        return user;
    }

    public static User createUser(String id, String name) {
        User user = createUser();
        user.setId(id);
        user.setName(name);
        return user;
    }

    // ========== 订单数据 ==========
    public static Order createOrder() {
        Order order = new Order();
        order.setId("order-123");
        order.setStatus("PENDING");
        order.setTotalAmount(new BigDecimal("100.00"));
        order.setUser(createUser());
        return order;
    }

    public static Order createCompletedOrder() {
        Order order = createOrder();
        order.setStatus("COMPLETED");
        order.setCompletedAt(LocalDateTime.now());
        return order;
    }

    // ========== 集合数据 ==========
    public static List<User> createUserList(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> createUser("user-" + i, "User " + i))
                .collect(Collectors.toList());
    }

    public static Map<String, User> createUserMap(int size) {
        return createUserList(size).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }
}

// ========== 使用示例 ==========
@Test
void testUserService() {
    User user = TestDataFactory.createUser();
    when(userRepository.findById()).thenReturn(user);

    User result = userService.findById("test-user-id");
    assertNotNull(result);
    assertEquals("Test User", result.getName());
}

@Test
void testOrderService() {
    Order order = TestDataFactory.createCompletedOrder();
    when(orderRepository.findById()).thenReturn(order);

    Order result = orderService.findById("order-123");
    assertEquals("COMPLETED", result.getStatus());
}
```

### 模式3: 参数化测试数据

```java
// ✅ JUnit 5参数化测试
@ParameterizedTest
@CsvSource({
    "user1, User One, user1@example.com",
    "user2, User Two, user2@example.com",
    "user3, User Three, user3@example.com"
})
void testUserValidation(String id, String name, String email) {
    User user = TestDataFactory.createUser(id, name, email);

    assertEquals(id, user.getId());
    assertEquals(name, user.getName());
    assertEquals(email, user.getEmail());
}

@ParameterizedTest
@MethodSource("createTestUsers")
void testUserServiceWithMultipleUsers(User user) {
    when(userRepository.findById(user.getId())).thenReturn(user);

    User result = userService.findById(user.getId());
    assertNotNull(result);
    assertEquals(user.getName(), result.getName());
}

static Stream<User> createTestUsers() {
    return Stream.of(
        TestDataFactory.createUser("user1", "User 1", "user1@example.com"),
        TestDataFactory.createUser("user2", "User 2", "user2@example.com"),
        TestDataFactory.createUser("user3", "User 3", "user3@example.com")
    );
}

// ✅ 提供边界值测试
@ParameterizedTest
@ValueSource(ints = {0, 1, 10, 100, 1000})
void testPagination(int pageSize) {
    List<User> users = TestDataFactory.createUserList(pageSize);

    assertEquals(pageSize, users.size());
}
```

## 📊 特殊数据类型处理

### 1. 日期时间处理

```java
// ✅ 固定时间测试
@Test
void testTimeBasedLogic() {
    // 使用固定时间，避免测试时间敏感性
    LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

    Order order = TestDataFactory.createOrder()
            .withCreatedAt(fixedTime);

    assertEquals(fixedTime, order.getCreatedAt());
}

// ✅ 时间范围测试
@Test
void testDateRange() {
    LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

    List<Order> orders = TestDataFactory.createOrdersInRange(startDate, endDate);

    orders.forEach(order -> {
        assertTrue(order.getCreatedAt().isAfter(startDate.minusSeconds(1)));
        assertTrue(order.getCreatedAt().isBefore(endDate.plusSeconds(1)));
    });
}
```

### 2. 枚举类型处理

```java
// ✅ 枚举测试数据
public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

@Test
void testOrderStatusTransitions() {
    Order order = TestDataFactory.createOrder();

    // 测试所有状态
    Arrays.stream(OrderStatus.values()).forEach(status -> {
        order.setStatus(status);
        // 验证状态转换逻辑...
    });
}

// ✅ 使用@EnumSource
@ParameterizedTest
@EnumSource(OrderStatus.class)
void testAllOrderStatuses(OrderStatus status) {
    Order order = TestDataFactory.createOrder();
    order.setStatus(status);

    // 验证每个状态的处理逻辑
}
```

### 3. 集合数据处理

```java
// ✅ 空集合测试
@Test
void testEmptyCollection() {
    List<User> emptyUsers = Collections.emptyList();
    when(userRepository.findAll()).thenReturn(emptyUsers);

    List<User> result = userService.findAllUsers();
    assertTrue(result.isEmpty());
}

// ✅ 单元素集合测试
@Test
void testSingleElementCollection() {
    User user = TestDataFactory.createUser();
    List<User> singleUser = Collections.singletonList(user);
    when(userRepository.findAll()).thenReturn(singleUser);

    List<User> result = userService.findAllUsers();
    assertEquals(1, result.size());
    assertEquals(user, result.get(0));
}

// ✅ 大集合测试
@Test
void testLargeCollection() {
    List<User> largeUserList = TestDataFactory.createUserList(10000);
    when(userRepository.findAll()).thenReturn(largeUserList);

    List<User> result = userService.findAllUsers();

    assertEquals(10000, result.size());
    // 验证性能...
}
```

## 🗄️ 数据库测试数据

### 1. 使用@TestInstance

```java
// ✅ 共享测试实例数据
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {

    private User testUser;
    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser("test-id", "Test User");
        testUsers = TestDataFactory.createUserList(5);

        // 数据库初始化...
    }

    @Test
    void testFindById() {
        Optional<User> result = userRepository.findById("test-id");
        assertTrue(result.isPresent());
        assertEquals(testUser.getName(), result.get().getName());
    }

    @Test
    void testFindAll() {
        List<User> result = userRepository.findAll();
        assertFalse(result.isEmpty());
        // 使用共享的testUsers进行验证...
    }
}
```

### 2. 使用@DataJpaTest

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        // 使用TestEntityManager进行JPA测试
        User user = TestDataFactory.createUser();
        entityManager.persistAndFlush(user);

        Optional<User> result = userRepository.findById(user.getId());
        assertTrue(result.isPresent());
        assertEquals(user.getName(), result.get().getName());
    }

    @Test
    void testQueryMethods() {
        // 创建多个用户进行查询测试
        User user1 = TestDataFactory.createUser("user1", "User One");
        User user2 = TestDataFactory.createUser("user2", "User Two");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        List<User> users = userRepository.findByNameContaining("User");
        assertEquals(2, users.size());
    }
}
```

## 🔄 测试数据清理

### 1. 自动清理

```java
// ✅ 使用JUnit生命周期
class UserServiceTest {

    private List<User> testUsers;

    @BeforeEach
    void setUp() {
        testUsers = TestDataFactory.createUserList(10);
        // 设置Mock...
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据或重置状态
        testUsers.clear();
        // 重置静态字段...
    }
}
```

### 2. 事务性测试

```java
// ✅ 事务性测试，自动回滚
@Transactional
@SpringBootTest
class UserIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testUserWorkflow() {
        // 测试会自动回滚，不影响数据库
        User user = TestDataFactory.createUser();
        userRepository.save(user);

        // 测试逻辑...

        // 测试结束后自动回滚
    }
}
```

## 📝 测试数据验证

### 1. 数据一致性验证

```java
// ✅ 验证数据一致性
@Test
void testUserDataConsistency() {
    User user = UserTestBuilder.aUser()
            .withId("test-123")
            .adminUser()
            .build();

    // 验证数据一致性
    assertEquals("test-123", user.getId());
    assertTrue(user.getRoles().contains("ADMIN"));
    assertNotNull(user.getCreatedAt());

    // 验证关联数据
    assertNotNull(user.getProfile());
    assertEquals(user.getId(), user.getProfile().getUserId());
}
```

### 2. 边界值验证

```java
// ✅ 边界值测试
@Test
void testBoundaryValues() {
    // 测试最小值
    User minUser = UserTestBuilder.aUser()
            .withName("A")
            .build();
    assertTrue(minUser.getName().length() >= 1);

    // 测试最大值
    User maxUser = UserTestBuilder.aUser()
            .withName("A".repeat(100))
            .build();
    assertTrue(maxUser.getName().length() <= 100);

    // 测试空值
    assertThrows(IllegalArgumentException.class, () -> {
        UserTestBuilder.aUser().withName(null).build();
    });
}
```

## 🚨 常见陷阱

### 陷阱1: 共享可变状态

```java
// ❌ 错误: 共享可变对象
public class UserServiceTest {
    private static User sharedUser = new User();  // ❌ 所有测试共享

    @Test
    void test1() {
        sharedUser.setName("User1");  // 影响其他测试
    }

    @Test
    void test2() {
        sharedUser.getName();  // 可能被test1修改
    }
}

// ✅ 正确: 独立的对象
public class UserServiceTest {

    @Test
    void test1() {
        User user = TestDataFactory.createUser();  // ✅ 独立对象
        user.setName("User1");
    }

    @Test
    void test2() {
        User user = TestDataFactory.createUser();  // ✅ 独立对象
        // 不受test1影响
    }
}
```

### 陷阱2: 硬编码数据

```java
// ❌ 错误: 测试中硬编码数据
@Test
void testUserService() {
    User user = new User();
    user.setId("hardcoded-id");  // ❌ 硬编码
    user.setName("Hardcoded Name");  // ❌ 硬编码

    // 测试逻辑...
}

// ✅ 正确: 使用工厂或构建器
@Test
void testUserService() {
    User user = TestDataFactory.createUser("test-id", "Test User");  // ✅ 统一管理

    // 测试逻辑...
}
```

### 陷阱3: 忽略边界情况

```java
// ❌ 错误: 只测试正常情况
@Test
void testUserService() {
    User user = TestDataFactory.createUser();  // ✅ 只测试正常用户

    // 测试逻辑...
}

// ✅ 正确: 测试各种情况
@Test
void testUserService() {
    // 正常用户
    User normalUser = TestDataFactory.createUser();

    // 空值测试
    assertThrows(IllegalArgumentException.class, () -> {
        User nullUser = TestDataFactory.createUser(null, null);
    });

    // 边界值测试
    User boundaryUser = UserTestBuilder.aUser()
            .withName("")  // 空字符串
            .build();
}
```

## 📊 性能考虑

### 1. 避免不必要的对象创建

```java
// ✅ 复用不可变对象
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    private final User immutableUser = TestDataFactory.createUser();

    @Test
    void test1() {
        // 使用共享的不可变对象
    }

    @Test
    void test2() {
        // 使用共享的不可变对象
    }
}
```

### 2. 大数据集优化

```java
// ✅ 惰性创建大数据集
public class TestDataFactory {

    private static List<User> largeUserCache;

    public static List<User> createLargeUserList(int size) {
        if (largeUserCache == null || largeUserCache.size() != size) {
            largeUserCache = IntStream.range(0, size)
                    .mapToObj(i -> createUser("user-" + i, "User " + i))
                    .collect(Collectors.toList());
        }
        return largeUserCache;
    }
}
```

## 📚 相关文档

- [模式1: 辅助方法模式](../02_PATTERNS/PATTERN_01_HELPER_METHOD.md) - 辅助方法与测试数据
- [模式2: 分层Mock配置](../02_PATTERNS/PATTERN_02_LAYERED_MOCK.md) - 测试数据与Mock结合
- [诊断流程](../01_ARCHITECTURE/03_DIAGNOSIS_PROCESS.md) - 数据问题诊断

---

**使用提示**:
1. 优先使用构建器模式创建复杂测试数据
2. 使用工厂方法管理常用数据
3. 确保测试数据的独立性和可重用性
4. 重视边界值和异常情况的数据测试

**更新日期**: 2025-12-03
**版本**: v1.0
**应用频率**: ⭐⭐⭐⭐⭐ (最高)
