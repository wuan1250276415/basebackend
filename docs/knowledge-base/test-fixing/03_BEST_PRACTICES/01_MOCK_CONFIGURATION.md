# Mock配置最佳实践 (Mock Configuration Best Practices)

## 📋 概述

Mock配置是单元测试的核心，良好的Mock配置能够提高测试的可读性、可维护性和稳定性。本文档总结了在basebackend项目中验证的Mock配置最佳实践。

## 🎯 核心原则

### 1. 单一职责原则
每个Mock对象应该只模拟其对应的接口或类，避免在一个Mock上设置过多无关的方法。

```java
// ✅ 正确: 按职责分离Mock
@Mock
private Repository repository;  // 只负责数据访问

@Mock
private CacheManager cacheManager;  // 只负责缓存

// ❌ 错误: 混合职责
@Mock
private Repository repository;  // ❌ 既做数据访问又做缓存
```

### 2. 最小惊讶原则
Mock的行为应该尽可能符合真实对象的预期行为，包括返回值类型、异常抛出等。

```java
// ✅ 正确: 返回类型匹配
when(service.getCount()).thenReturn(10);  // int类型
when(service.getUser()).thenReturn(user);  // User对象

// ❌ 错误: 返回类型不匹配
when(service.getCount()).thenReturn("10");  // ❌ String而不是int
when(service.getUser()).thenReturn(null);  // ❌ 没有特殊原因不要返回null
```

### 3. 独立性原则
测试之间不应该相互依赖，每个测试应该能够独立运行和验证。

```java
// ✅ 正确: 每个测试独立设置Mock
@Test
void testMethod1() {
    when(service.method1()).thenReturn(result1);
    // 测试逻辑...
}

@Test
void testMethod2() {
    when(service.method2()).thenReturn(result2);  // 独立设置
    // 测试逻辑...
}

// ❌ 错误: 测试间依赖
private String sharedResult = "shared";

@Test
void testMethod1() {
    when(service.method1()).thenReturn(sharedResult);
}

@Test
void testMethod2() {
    // ❌ 依赖sharedResult的状态
}
```

## 🔧 Mock注解配置

### 1. 基础注解配置

```java
@ExtendWith(MockitoExtension.class)  // ✅ 必须
@MockitoSettings(strictness = Strictness.LENIENT)  // ✅ 推荐
class ServiceImplTest {

    // ✅ 使用@Mock注解
    @Mock
    private Repository repository;

    // ✅ 需要严格验证的Mock使用strict模式
    @Mock(strict = true)
    private SecurityService securityService;

    @InjectMocks
    private ServiceImpl service;
}
```

### 2. 高级注解配置

```java
@ExtendWith(MockitoExtension.class)
class ServiceImplTest {

    // ✅ 命名Mock，便于调试
    @Mock(name = "userRepository")
    private UserRepository userRepository;

    // ✅ 设置默认答案
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private NestedService nestedService;

    // ✅ 序列化Mock（需要时）
    @Mock(serializable = true)
    private RemoteService remoteService;
}
```

## 📊 Mock行为配置模式

### 模式1: 基础返回值

```java
// ✅ 简单值返回
when(service.getData()).thenReturn("test-data");
when(service.getCount()).thenReturn(42);
when(service.isActive()).thenReturn(true);

// ✅ 对象返回
User user = createTestUser();
when(userRepository.findById()).thenReturn(user);

// ✅ 集合返回
List<User> users = Arrays.asList(user1, user2);
when(userRepository.findAll()).thenReturn(users);
```

### 模式2: 参数化返回值

```java
// ✅ 基于参数值返回
when(service.getDataById("id1")).thenReturn("data1");
when(service.getDataById("id2")).thenReturn("data2");
when(service.getDataById(anyString())).thenReturn("default");  // 默认值

// ✅ 基于参数匹配
when(service.save(any(User.class))).thenAnswer(invocation -> {
    User user = invocation.getArgument(0);
    user.setId("generated-id");
    return user;
});
```

### 模式3: 异常处理

```java
// ✅ 抛出异常
when(service.failingMethod()).thenThrow(new RuntimeException("Test error"));
when(service.validate(null)).thenThrow(new IllegalArgumentException("Invalid"));

// ✅ 使用doThrow语法（void方法）
doThrow(new ValidationException("Invalid")).when(service).validate(any());
```

### 模式4: 连续调用

```java
// ✅ 连续调用返回不同值
when(service.getNext()).thenReturn(1, 2, 3);
when(service.getNext()).thenReturn(1).thenReturn(2).thenReturn(3);

// ✅ 循环返回
when(service.getRound()).thenReturn(1, 2, 3).thenThrow(new NoSuchElementException());

// ✅ 高级连续调用
when(service.getData()).thenAnswer(invocation -> {
    int callCount = ++callCountMap.getOrDefault(invocation, 0);
    switch (callCount) {
        case 1: return "first";
        case 2: return "second";
        default: return "default";
    }
});
```

## 🛠️ 高级配置技巧

### 技巧1: Deep Stubs（深度Stub）

```java
// ✅ 场景: 需要模拟链式调用
@Mock(answer = Answers.RETURNS_DEEP_STUBS)
private ComplexService complexService;

// 使用Deep Stubs
@Test
void testMethod() {
    when(complexService.getRepository().findById()).thenReturn(user);

    // 不用单独Mock repository
    // 直接设置链式调用
}

// ❌ 传统方式需要
@Mock
private Repository repository;

@BeforeEach
void setUp() {
    when(complexService.getRepository()).thenReturn(repository);
    when(repository.findById()).thenReturn(user);
}
```

### 技巧2: Spy（部分模拟）

```java
// ✅ 场景: 需要部分真实方法
@Test
void testWithPartialMock() {
    List<String> list = spy(new ArrayList<>());

    // Spy部分真实方法
    when(list.size()).thenReturn(100);  // Mock size
    // list.add() 仍使用真实方法

    // Spy也可以使用doReturn语法
    doReturn(100).when(list).size();
}
```

### 技巧3: 自定义Answer

```java
// ✅ 自定义Answer逻辑
when(service.process()).thenAnswer(invocation -> {
    // 获取参数
    String input = invocation.getArgument(0);

    // 自定义逻辑
    if ("error".equals(input)) {
        throw new RuntimeException("Simulated error");
    }

    // 返回结果
    return "processed-" + input;
});
```

### 技巧4: 时间控制

```java
// ✅ 控制时间相关行为
@Test
void testTimeSensitive() {
    Clock fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.systemDefault());
    when(clockService.getClock()).thenReturn(fixedClock);

    // 测试逻辑...
}
```

## 🎨 Mock设置组织模式

### 模式1: 辅助方法模式（推荐）

```java
class ServiceImplTest {

    @Mock
    private Repository repository;

    @InjectMocks
    private ServiceImpl service;

    // ========== 辅助方法 ==========
    private void setupRepository() {
        User user = createTestUser();
        when(repository.findById()).thenReturn(user);
        when(repository.save()).thenReturn(user);
    }

    // ========== 测试方法 ==========
    @Test
    void testMethod() {
        setupRepository();
        // 测试逻辑...
    }
}
```

### 模式2: 分层设置模式

```java
class ServiceImplTest {

    @BeforeEach
    void setUp() {
        setupCoreServices();
        setupRepositories();
        setupExternalServices();
    }

    private void setupCoreServices() {
        // 核心服务设置
    }

    private void setupRepositories() {
        // Repository设置
    }

    private void setupExternalServices() {
        // 外部服务设置
    }
}
```

### 模式3: 条件设置模式

```java
class ServiceImplTest {

    private boolean useCache = false;

    private void setupRepository() {
        if (useCache) {
            when(repository.findById()).thenReturn(cachedData);
        } else {
            when(repository.findById()).thenReturn(dbData);
        }
    }

    @Test
    void testWithCache() {
        useCache = true;
        setupRepository();
        // 测试逻辑...
    }
}
```

## 🚨 常见错误与避免

### 错误1: 过度Mock

```java
// ❌ 错误: Mock了太多不必要的方法
@BeforeEach
void setUp() {
    when(service.method1()).thenReturn(result1);
    when(service.method2()).thenReturn(result2);
    when(service.method3()).thenReturn(result3);
    when(service.method4()).thenReturn(result4);
    when(service.method5()).thenReturn(result5);
    // ❌ 测试只用到method1，其他都是无用的
}

// ✅ 正确: 只Mock需要的方法
@Test
void testMethod() {
    when(service.method1()).thenReturn(result1);  // ✅ 只设置需要的
    // 测试逻辑...
}
```

### 错误2: Mock状态泄露

```java
// ❌ 错误: 共享Mock状态
private User user = new User();

@BeforeEach
void setUp() {
    when(repository.findById()).thenReturn(user);
}

@Test
void testModifyUser() {
    user.setName("modified");  // ❌ 影响其他测试
}

// ✅ 正确: 每个测试独立
private User createTestUser() {
    return new User();  // ✅ 独立对象
}

@BeforeEach
void setUp() {
    User user = createTestUser();
    when(repository.findById()).thenReturn(user);
}
```

### 错误3: 忽略严格模式警告

```java
// ❌ 错误: 严格模式下未使用的Stub
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class ServiceImplTest {
    @Test
    void testMethod() {
        when(service.method1()).thenReturn(result1);  // ❌ 未使用
        when(service.method2()).thenReturn(result2);  // ❌ 未使用
        // 只调用了service.method3()
    }
}

// ✅ 正确: 使用宽松模式或移除未使用的Stub
@MockitoSettings(strictness = Strictness.LENIENT)  // ✅ 推荐
class ServiceImplTest {
    // 或者只设置使用的方法
}
```

### 错误4: 不正确的参数匹配

```java
// ❌ 错误: 使用any()时忽略返回值类型
when(service.save(any(User.class))).thenReturn("string");  // ❌ 返回类型不匹配

// ✅ 正确: 参数匹配和返回值类型一致
when(service.save(any(User.class))).thenAnswer(invocation -> {
    User user = invocation.getArgument(0);
    user.setId("generated");
    return user;  // ✅ 返回User对象
});
```

## 📊 性能优化

### 1. 减少Mock创建开销

```java
// ✅ 重复使用的Mock在setUp中创建
@BeforeEach
void setUp() {
    User user = createTestUser();
    testUsers = Arrays.asList(user1, user2, user3);  // ✅ 预先创建
}

// ✅ 避免在测试方法中创建大对象
@Test
void testMethod() {
    // ❌ 在测试中创建大对象
    List<User> users = createLargeUserList(1000);

    // ✅ 使用预创建的对象
    when(repository.findAll()).thenReturn(testUsers);
}
```

### 2. 并行测试考虑

```java
// ✅ 并行测试时避免共享状态
class ServiceImplTest {

    // ❌ 避免使用静态Mock
    // private static MockRepository mockRepo;  // ❌ 并行测试问题

    // ✅ 每个测试实例独立
    @Mock
    private Repository repository;  // ✅ 实例级别
}
```

## 🧪 测试验证模式

### 1. 验证调用次数

```java
// ✅ 验证调用次数
@Test
void testMethod() {
    service.process();

    // 验证调用次数
    verify(repository, times(1)).save();
    verify(repository, atLeastOnce()).findById();
    verify(repository, atMost(3)).update();
    verifyNoMoreInteractions(repository);  // ✅ 验证没有其他调用
}
```

### 2. 验证调用参数

```java
// ✅ 验证具体参数
@Test
void testMethod() {
    service.save(user);

    // 验证参数
    verify(repository).save(eq(user));  // ✅ 验证特定参数
    verify(repository).save(argThat(u -> u.getName() != null));  // ✅ 验证参数条件
}
```

### 3. 验证调用顺序

```java
// ✅ 验证调用顺序
@Test
void testMethod() {
    InOrder inOrder = inOrder(repository);

    service.process();

    inOrder.verify(repository).validate();
    inOrder.verify(repository).save();
    inOrder.verify(repository).notify();
}
```

## 📚 相关文档

- [模式1: 辅助方法模式](../02_PATTERNS/PATTERN_01_HELPER_METHOD.md) - Mock设置模式
- [模式2: 分层Mock配置](../02_PATTERNS/PATTERN_02_LAYERED_MOCK.md) - 高级配置
- [模式3: Controller测试配置](../02_PATTERNS/PATTERN_03_CONTROLLER_CONFIG.md) - 复杂依赖配置
- [模式4: 统一Mock属性](../02_PATTERNS/PATTERN_04_UNIFIED_PROPERTIES.md) - 对象属性Mock

---

**使用提示**:
1. 优先使用辅助方法模式组织Mock
2. 根据需要选择合适的strictness级别
3. 避免Mock状态泄露和过度Mock
4. 使用验证功能确保测试的准确性

**更新日期**: 2025-12-03
**版本**: v1.0
**应用频率**: ⭐⭐⭐⭐⭐ (最高)
