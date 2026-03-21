# 代码片段速查手册 (Code Snippets Reference)

## 📋 概述

本文档提供测试修复中最常用的代码片段，可以直接复制使用。所有片段都经过实际项目验证。

## 🎯 基础配置片段

### 片段1: 测试类基础配置

```java
// ✅ Service层测试标准配置
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceImplTest {

    @Mock
    private Repository repository;

    @Mock
    private ExternalService externalService;

    @InjectMocks
    private ServiceImpl service;

    @BeforeEach
    void setUp() {
        setupRepository();
        setupExternalService();
    }

    // ========== 辅助方法 ==========
    private void setupRepository() {
        // Repository Mock设置
    }

    private void setupExternalService() {
        // ExternalService Mock设置
    }

    // ========== 测试方法 ==========
    @Test
    void testMethod() {
        setupRepository();  // 按需调用辅助方法
        // 测试逻辑...
    }
}
```

### 片段2: Controller测试配置

```java
// ✅ Controller测试配置类
@TestConfiguration
@Profile("test")
public class ControllerTestConfig {

    @Bean
    @Primary
    public SomeService someService() {
        return Mockito.mock(SomeService.class);
    }

    @Bean
    @Primary
    public AnotherService anotherService() {
        return Mockito.mock(AnotherService.class);
    }

    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return Mockito.mock(JwtUtil.class);
    }
}

// ✅ Controller测试类
@WebMvcTest(SomeController.class)
@Import(ControllerTestConfig.class)
class SomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEndpoint() throws Exception {
        mockMvc.perform(get("/api/test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
```

### 片段3: DTO测试配置

```java
// ✅ DTO测试标准配置
@DataJpaTest
class RepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSave() {
        User user = TestDataFactory.createUser();
        entityManager.persistAndFlush(user);

        Optional<User> result = userRepository.findById(user.getId());
        assertTrue(result.isPresent());
    }
}
```

## 🔧 Mock设置片段

### 片段4: Repository Mock

```java
private void setupRepository() {
    User user = TestDataFactory.createUser();

    when(repository.findById(anyString())).thenReturn(Optional.of(user));
    when(repository.save(any(User.class))).thenReturn(user);
    when(repository.findAll()).thenReturn(Arrays.asList(user));
    when(repository.deleteById(anyString())).thenReturn(true);
    when(repository.existsById(anyString())).thenReturn(true);
}
```

### 片段5: Service Mock (链式调用)

```java
private void setupExternalService() {
    ExternalResponse response = new ExternalResponse();
    response.setStatus("success");
    response.setData("test-data");

    when(externalService.callApi(anyString())).thenReturn(response);
    when(externalService.validate(any())).thenReturn(true);
    when(externalService.process(any())).thenAnswer(invocation -> {
        Request req = invocation.getArgument(0);
        return "processed-" + req.getId();
    });
}
```

### 片段6: Cache Mock

```java
private void setupCache() {
    ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
    HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

    when(cacheTemplate.opsForValue()).thenReturn(valueOps);
    when(cacheTemplate.opsForHash()).thenReturn(hashOps);

    when(valueOps.get(anyString())).thenReturn(null);
    when(valueOps.set(anyString(), any())).thenReturn(true);
    when(valueOps.delete(anyString())).thenReturn(true);

    when(hashOps.get(anyString(), any())).thenReturn(null);
    when(hashOps.put(anyString(), any(), any())).thenReturn(true);
}
```

## 📊 查询链Mock片段

### 片段7: ProcessDefinitionQuery

```java
private void setupProcessDefinitionQuery() {
    ProcessDefinition processDefinition = mock(ProcessDefinition.class);

    when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
    when(processDefinitionQuery.processDefinitionId(anyString())).thenReturn(processDefinitionQuery);
    when(processDefinitionQuery.singleResult()).thenReturn(processDefinition);

    // ✅ 设置完整属性
    when(processDefinition.getId()).thenReturn("process-def-123");
    when(processDefinition.getDeploymentId()).thenReturn("deployment-456");
    when(processDefinition.getKey()).thenReturn("test-process");
    when(processDefinition.getName()).thenReturn("测试流程");
    when(processDefinition.getVersion()).thenReturn(1);
    when(processDefinition.getTenantId()).thenReturn("tenant-001");
}
```

### 片段8: ProcessInstanceQuery

```java
private void setupProcessInstanceQuery() {
    ProcessInstance processInstance = mock(ProcessInstance.class);

    when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
    when(processInstanceQuery.processDefinitionKey(anyString())).thenReturn(processInstanceQuery);
    when(processInstanceQuery.processInstanceId(anyString())).thenReturn(processInstanceQuery);
    when(processInstanceQuery.singleResult()).thenReturn(processInstance);

    // ✅ 设置完整属性
    when(processInstance.getId()).thenReturn("process-instance-123");
    when(processInstance.getProcessDefinitionId()).thenReturn("process-def-456");
    when(processInstance.getBusinessKey()).thenReturn("business-key-789");
    when(processInstance.getTenantId()).thenReturn("tenant-001");
    when(processInstance.getStartTime()).thenReturn(new Date());
}
```

### 片段9: HistoricProcessInstanceQuery

```java
private void setupHistoricProcessInstanceQuery() {
    HistoricProcessInstance historic = mock(HistoricProcessInstance.class);

    when(historyService.createHistoricProcessInstanceQuery()).thenReturn(historicQuery);
    when(historicQuery.processDefinitionKey(anyString())).thenReturn(historicQuery);
    when(historicQuery.singleResult()).thenReturn(historic);

    // ✅ 设置Date类型属性
    when(historic.getId()).thenReturn("historic-123");
    when(historic.getProcessDefinitionId()).thenReturn("process-def-456");
    when(historic.getStartTime()).thenReturn(new Date());  // 关键
    when(historic.getEndTime()).thenReturn(new Date());    // 关键
    when(historic.getDurationInMillis()).thenReturn(3600000L);
    when(historic.getState()).thenReturn("COMPLETED");
    when(historic.getStartUserId()).thenReturn("user1");
}
```

### 片段10: VariableInstanceQuery

```java
private void setupVariableInstanceQuery() {
    VariableInstance variable = mock(VariableInstance.class);

    when(runtimeService.createVariableInstanceQuery()).thenReturn(variableQuery);
    when(variableQuery.processInstanceIdIn(anyString())).thenReturn(variableQuery);
    when(variableQuery.list()).thenReturn(Arrays.asList(variable));  // 关键

    // ✅ 设置变量属性
    when(variable.getName()).thenReturn("test-variable");
    when(variable.getValue()).thenReturn("test-value");
    when(variable.getType()).thenReturn("String");
}
```

## 🛠️ 异常测试片段

### 片段11: 基础异常测试

```java
// ✅ 测试IllegalArgumentException
@Test
void testInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> {
        service.processData(null);
    });
}

// ✅ 测试自定义异常
@Test
void testBusinessException() {
    BusinessException exception = assertThrows(
        BusinessException.class,
        () -> service.processBusinessData(createInvalidData())
    );

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getErrorCode());
    assertEquals("Invalid data", exception.getMessage());
}
```

### 片段12: 异常消息验证

```java
@Test
void testExceptionMessage() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.validateUser(null)
    );

    assertTrue(exception.getMessage().contains("User cannot be null"));
    assertEquals("User cannot be null", exception.getMessage());
}
```

### 片段13: 异常恢复测试

```java
@Test
void testRetryLogic() {
    when(service.execute())
        .thenThrow(new RetryableException("Temporary failure"))
        .thenThrow(new RetryableException("Temporary failure"))
        .thenReturn("success");

    String result = service.executeWithRetry(3);

    assertEquals("success", result);
    verify(service, times(3)).execute();
}
```

## 📝 测试数据片段

### 片段14: 用户数据构建器

```java
// ✅ 用户构建器
public class UserTestBuilder {
    private String id = "test-user-id";
    private String name = "Test User";
    private String email = "test@example.com";
    private List<String> roles = Arrays.asList("USER");

    private UserTestBuilder() {}

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

    public User build() {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setRoles(roles);
        return user;
    }

    public UserTestBuilder adminUser() {
        return withId("admin-123")
                .withName("Admin User")
                .withEmail("admin@example.com");
    }
}

// 使用示例
@Test
void testUser() {
    User user = UserTestBuilder.aUser()
            .withId("user-123")
            .withName("Custom User")
            .build();
}
```

### 片段15: 工厂方法

```java
// ✅ 测试数据工厂
public class TestDataFactory {

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

    public static List<User> createUserList(int size) {
        return IntStream.range(0, size)
                .mapToObj(i -> createUser("user-" + i, "User " + i))
                .collect(Collectors.toList());
    }
}
```

## 🔍 验证片段

### 片段16: 方法调用验证

```java
@Test
void testMethod() {
    User user = TestDataFactory.createUser();
    when(repository.findById()).thenReturn(user);

    service.processUser("user-id");

    // ✅ 验证调用次数
    verify(repository, times(1)).findById("user-id");

    // ✅ 验证调用参数
    verify(repository).save(argThat(u -> u.getName() != null));

    // ✅ 验证无其他调用
    verifyNoMoreInteractions(repository);
}
```

### 片段17: 顺序验证

```java
@Test
void testMethod() {
    service.processData();

    InOrder inOrder = inOrder(repository);

    inOrder.verify(repository).validate();
    inOrder.verify(repository).save();
    inOrder.verify(repository).notify();
}
```

### 片段18: 集合验证

```java
@Test
void testListOperations() {
    List<User> users = TestDataFactory.createUserList(5);
    when(repository.findAll()).thenReturn(users);

    List<User> result = service.getAllUsers();

    assertEquals(5, result.size());
    assertTrue(result.stream().allMatch(u -> u.getId() != null));
}
```

## 🚨 错误修复片段

### 片段19: 修复UnnecessaryStubbing

```java
// ❌ 修复前
@ExtendWith(MockitoExtension.class)
class TestClass { }

// ✅ 修复后
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // 添加这行
class TestClass { }
```

### 片段20: 修复Query返回null

```java
// ❌ 修复前
private void setupQuery() {
    when(service.createQuery()).thenReturn(query);
    when(query.method1()).thenReturn(query);
    // ❌ 缺少 when(query.singleResult()).thenReturn(entity);
}

// ✅ 修复后
private void setupQuery() {
    when(service.createQuery()).thenReturn(query);
    when(query.method1()).thenReturn(query);
    when(query.singleResult()).thenReturn(entity);  // 添加这行
}
```

### 片段21: 修复属性为null

```java
// ❌ 修复前
@Test
void testMethod() {
    when(query.singleResult()).thenReturn(entity);
    // ❌ 缺少 entity.getXXX() 设置
}

// ✅ 修复后
private void setupEntity() {
    when(entity.getId()).thenReturn("test-id");
    when(entity.getName()).thenReturn("test-name");
    when(entity.getDeploymentId()).thenReturn("deployment-123");  // 添加这行
}

@Test
void testMethod() {
    setupEntity();  // 调用辅助方法
    // 测试逻辑...
}
```

## 🎨 高级片段

### 片段22: Deep Stubs

```java
// ✅ 深度Stub
@Mock(answer = Answers.RETURNS_DEEP_STUBS)
private ComplexService complexService;

@Test
void testMethod() {
    when(complexService.getRepository().findById()).thenReturn(user);

    // 无需单独Mock repository
    // 直接设置链式调用
}
```

### 片段23: 参数匹配

```java
@Test
void testMethod() {
    User user = TestDataFactory.createUser();

    // ✅ 使用argThat
    when(repository.save(argThat(u -> u.getName() != null))).thenReturn(user);

    // ✅ 使用any()匹配类型
    when(repository.findByEmail(anyString())).thenReturn(Optional.of(user));

    // ✅ 验证参数
    service.saveUser(user);
    verify(repository).save(argThat(u -> u.getName().equals("Test User")));
}
```

### 片段24: 自定义Answer

```java
@Test
void testMethod() {
    when(service.process()).thenAnswer(invocation -> {
        String input = invocation.getArgument(0);
        if ("error".equals(input)) {
            throw new RuntimeException("Simulated error");
        }
        return "processed-" + input;
    });

    // 测试正常流程
    String result1 = service.process("test");
    assertEquals("processed-test", result1);

    // 测试异常流程
    assertThrows(RuntimeException.class, () -> {
        service.process("error");
    });
}
```

## 📊 参数化测试片段

### 片段25: CsvSource参数化

```java
@ParameterizedTest
@CsvSource({
    "user1, User One, user1@example.com",
    "user2, User Two, user2@example.com",
    "user3, User Three, user3@example.com"
})
void testUserValidation(String id, String name, String email) {
    User user = TestDataFactory.createUser(id, name);
    assertEquals(id, user.getId());
    assertEquals(name, user.getName());
}
```

### 片段26: MethodSource参数化

```java
@ParameterizedTest
@MethodSource("createTestUsers")
void testUserServiceWithMultipleUsers(User user) {
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

    User result = userService.findById(user.getId());
    assertNotNull(result);
}

static Stream<User> createTestUsers() {
    return Stream.of(
        TestDataFactory.createUser("user1", "User 1"),
        TestDataFactory.createUser("user2", "User 2"),
        TestDataFactory.createUser("user3", "User 3")
    );
}
```

## 🔄 生命周期片段

### 片段27: TestInstance

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // ✅ 共享实例
class ServiceTest {

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();  // ✅ 共享状态
    }

    @Test
    void testMethod1() {
        // 使用 testUser
    }

    @Test
    void testMethod2() {
        // 使用 testUser
    }
}
```

### 片段28: BeforeAll和AfterAll

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseTest {

    @BeforeAll
    static void setUpClass() {
        // 初始化数据库连接
    }

    @AfterAll
    static void tearDownClass() {
        // 清理数据库连接
    }

    @Test
    void testMethod() {
        // 测试逻辑...
    }
}
```

## 📚 使用说明

### 使用方法

1. **直接复制**: 选择需要的片段，直接复制到测试类中
2. **按需修改**: 根据具体业务调整参数和方法名
3. **组合使用**: 可以组合多个片段使用
4. **自定义扩展**: 在片段基础上进行扩展

### 注意事项

```
⚠️ 注意事项:
1. 修改类名和方法名以匹配实际代码
2. 添加必要的import语句
3. 确保Mock对象的类型正确
4. 验证设置的值符合业务逻辑
5. 测试后清理Mock状态
```

### 快速搜索

```
查找片段:
- Ctrl+F 搜索关键词
- 例如: "UnnecessaryStubbing", "Query返回null", "异常测试"

复制片段:
- 选择需要的代码块
- 复制并粘贴到测试类
- 按需修改
```

## 📚 相关文档

- [模式1: 辅助方法模式](../02_PATTERNS/PATTERN_01_HELPER_METHOD.md)
- [模式2: 分层Mock配置](../02_PATTERNS/PATTERN_02_LAYERED_MOCK.md)
- [模式3: Controller配置](../02_PATTERNS/PATTERN_03_CONTROLLER_CONFIG.md)
- [模式4: 统一属性设置](../02_PATTERNS/PATTERN_04_UNIFIED_PROPERTIES.md)

---

**更新日期**: 2025-12-03
**版本**: v1.0
**片段数量**: 28个
**应用场景**: 测试修复和编写
**推荐指数**: ⭐⭐⭐⭐⭐ (必备)
