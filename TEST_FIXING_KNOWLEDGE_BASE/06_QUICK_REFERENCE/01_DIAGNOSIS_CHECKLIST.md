# 快速诊断检查清单 (Quick Diagnosis Checklist)

## 📋 5分钟问题诊断流程

### Step 1: 错误分类 (60秒)

查看错误信息，快速分类：

```
❌ UnnecessaryStubbingException
   → [模式2: 分层Mock配置]

❌ Cannot invoke 'XQuery.singleResult()' because the return value is null
   → [模式4: 统一属性设置]

❌ NoSuchBeanDefinitionException
   → [模式3: Controller配置]

❌ getXXX() returns null
   → [模式1: 辅助方法] + [模式4: 统一属性设置]
```

### Step 2: 测试结构检查 (2分钟)

检查测试类结构：

```
✓ 是否有 @ExtendWith(MockitoExtension.class)
✓ 是否有 @MockitoSettings(strictness = Strictness.LENIENT)
✓ @BeforeEach 是否设置Mock
✓ 是否使用 @Mock 和 @InjectMocks
✓ 辅助方法是否完整
```

### Step 3: 修复方法选择 (2分钟)

根据错误类型选择模式：

```
UnnecessaryStubbing → 模式2
   ├─ 添加 @MockitoSettings(strictness = Strictness.LENIENT)
   └─ 将Mock设置移到辅助方法

Query返回null → 模式4
   ├─ 检查 XXXQuery.singleResult()
   └─ 设置对象完整属性

依赖注入问题 → 模式3
   ├─ 创建 XXXTestConfig
   └─ 使用 @Import 导入

复杂Mock设置 → 模式1
   └─ 创建辅助方法封装
```

## 🚨 常见错误速查表

| 错误信息 | 错误类型 | 首选修复方案 | 备选方案 | 预计时间 |
|----------|----------|--------------|----------|----------|
| UnnecessaryStubbingException | Mock配置 | 模式2 | 模式1 | 1分钟 |
| singleResult() returns null | 查询链 | 模式4 | 模式1 | 3分钟 |
| getDeploymentId() returns null | 属性设置 | 模式4 | 模式1 | 2分钟 |
| NoSuchBeanDefinitionException | 依赖注入 | 模式3 | - | 10分钟 |
| ApplicationContext loading failed | 配置问题 | 模式3 | 使用@SpringBootTest | 15分钟 |
| Date returns null | 属性类型 | 模式4 | 模式1 | 1分钟 |
| Argument mismatch | 参数匹配 | 模式1 | 模式2 | 2分钟 |

## 🔧 修复动作速查

### 动作1: 添加LENIENT配置

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ← 添加这行
class ServiceImplTest {
    // 测试代码...
}
```

### 动作2: 创建辅助方法

```java
private void setupXXX() {  // XXX = Service名称
    when(mock.method1()).thenReturn(result1);
    when(mock.method2()).thenReturn(result2);
}

@Test
void testMethod() {
    setupXXX();  // 在测试中调用
    // 测试逻辑...
}
```

### 动作3: 创建测试配置

```java
@TestConfiguration
@Profile("test")
public class ControllerTestConfig {
    @Bean @Primary
    public XXXService xxxService() {
        return Mockito.mock(XXXService.class);
    }
}

// 使用
@WebMvcTest(SomeController.class)
@Import(ControllerTestConfig.class)
class SomeControllerTest { }
```

### 动作4: 设置查询链

```java
private void setupQuery() {
    when(service.createQuery()).thenReturn(query);
    when(query.method1()).thenReturn(query);  // 链式调用
    when(query.method2()).thenReturn(query);
    when(query.singleResult()).thenReturn(entity);  // 返回结果
}

private void setupEntityProperties() {
    when(entity.getId()).thenReturn("test-id");
    when(entity.getName()).thenReturn("test-name");
    when(entity.getDeploymentId()).thenReturn("deployment-123");  // 关键属性
    when(entity.getStartTime()).thenReturn(new Date());  // Date类型
}
```

## 🎯 修复优先级决策树

```
发现测试错误
    ↓
是Service层错误？
    ├─ 是 → 优先修复 (高ROI)
    │       ├─ UnnecessaryStubbing? → 模式2
    │       ├─ Query返回null? → 模式4
    │       └─ 属性缺失? → 模式4
    │
    └─ 否 → 是Controller层错误？
            ├─ 是 → 可选修复 (低ROI)
            │       ├─ 简单依赖? → 模式3
            │       └─ 复杂依赖? → 跳过
            │
            └─ 否 → DTO层？
                    ├─ 是 → 批量修复 (高效率)
                    └─ 否 → 其他层
```

## 📊 检查清单

### 检查清单1: Mock配置

```
□ 添加了 @ExtendWith(MockitoExtension.class)
□ 添加了 @MockitoSettings(strictness = Strictness.LENIENT)
□ 使用了 @Mock 注解 (不是 Mockito.mock())
□ 使用了 @InjectMocks 注解
□ Mock设置在辅助方法中
□ 辅助方法按功能分组
```

### 检查清单2: 查询链配置

```
□ 创建Query的Mock
□ 设置Query链式调用 (method1().method2()...)
□ 设置 singleResult() 返回值
□ 设置对象的完整属性
□ 设置Date类型属性
□ 设置枚举类型属性
```

### 检查清单3: Controller测试

```
□ 创建了 @TestConfiguration 类
□ 配置了 @Bean @Primary
□ 使用了 @Import 导入配置
□ 移除了重复的 @MockBean
□ 考虑使用 @SpringBootTest (复杂依赖时)
```

### 检查清单4: 异常测试

```
□ 使用 assertThrows 而不是 try-catch
□ 验证异常类型
□ 验证异常消息
□ 验证异常上下文信息
□ 测试异常恢复逻辑
```

## 🚀 5分钟修复脚本

### 脚本1: UnnecessaryStubbingException

```bash
# 1分钟: 添加LENIENT配置
sed -i '1a @MockitoSettings(strictness = Strictness.LENIENT)' TestClass.java

# 2分钟: 检查setUp方法
grep -n "setUp\|@BeforeEach" TestClass.java

# 2分钟: 验证结果
mvn test -Dtest=TestClass
```

### 脚本2: Query返回null

```bash
# 1分钟: 查找错误
grep -r "singleResult()" src/test/

# 2分钟: 添加singleResult设置
grep -A10 "when(query" TestClass.java

# 2分钟: 添加对象属性设置
grep -A10 "when(entity" TestClass.java
```

### 脚本3: 依赖注入问题

```bash
# 3分钟: 创建TestConfig
cat > ControllerTestConfig.java << 'EOF'
@TestConfiguration
@Profile("test")
public class ControllerTestConfig {
    @Bean @Primary
    public SomeService someService() {
        return Mockito.mock(SomeService.class);
    }
}
EOF

# 2分钟: 导入配置
sed -i '1a @Import(ControllerTestConfig.class)' TestClass.java
```

## ⚡ 性能优化检查

### 优化1: 减少Mock数量

```
检查点:
□ 是否有未使用的Mock
□ 是否可以合并相似Mock
□ 是否有重复的Mock设置

优化动作:
1. 移除未使用的Mock
2. 使用参数化辅助方法
3. 移动Mock到测试方法中
```

### 优化2: 加速测试执行

```
检查点:
□ 是否有不必要的setUp逻辑
□ 是否有大对象创建在setUp中
□ 是否可以复用不可变对象

优化动作:
1. 简化setUp方法
2. 使用@TestInstance(Lifecycle.PER_CLASS)
3. 预创建不可变数据
```

## 📱 移动端速查卡

### 错误 → 修复动作对照卡

```
UnnecessaryStubbing
→ @MockitoSettings(strictness = Strictness.LENIENT)

Query返回null
→ when(query.singleResult()).thenReturn(entity)
→ when(entity.getXXX()).thenReturn(value)

NoSuchBeanDefinition
→ 创建 @TestConfiguration
→ @Bean @Primary

属性为null
→ 设置所有可能用到的属性
→ 特别注意Date类型
```

### 注释模板

```java
/**
 * 修复模板: Service层测试
 * 1. 添加 @MockitoSettings(strictness = Strictness.LENIENT)
 * 2. 创建辅助方法 setupXXX()
 * 3. 设置完整属性
 */
```

## 🎓 速记口诀

```
测试修复四步走:
1. 看错误 (分类问题)
2. 选模式 (匹配修复模式)
3. 加配置 (@MockitoSettings)
4. 设属性 (完整Mock)

Service层优先:
复杂先跳过,
简单先修复,
批量处理高效率!
```

## 📞 应急方案

### 方案1: 快速修复所有测试

```bash
# 如果时间紧急，只做这三步：
1. 所有测试类添加:
   @MockitoSettings(strictness = Strictness.LENIENT)

2. setUp方法中统一设置:
   when(service.createQuery()).thenReturn(query);
   when(query.singleResult()).thenReturn(mock(Entity.class));

3. 批量运行验证:
   mvn test -Dtest=*Test
```

### 方案2: 临时禁用严格模式

```java
// 临时方案: 全局禁用
@MockitoSettings(strictness = Strictness.LENIENT)
class AllTests {
    // 所有测试自动应用LENIENT模式
}
```

### 方案3: 跳过复杂测试

```bash
# 跳过Controller测试，只测试Service
mvn test -Dtest=*ServiceImplTest

# 跳过集成测试，只测试单元测试
mvn test -Dtest=*UnitTest
```

## 📚 相关链接

- [模式1: 辅助方法模式](../02_PATTERNS/PATTERN_01_HELPER_METHOD.md)
- [模式2: 分层Mock配置](../02_PATTERNS/PATTERN_02_LAYERED_MOCK.md)
- [模式3: Controller配置](../02_PATTERNS/PATTERN_03_CONTROLLER_CONFIG.md)
- [模式4: 统一属性设置](../02_PATTERNS/PATTERN_04_UNIFIED_PROPERTIES.md)
- [诊断流程](../01_ARCHITECTURE/03_DIAGNOSIS_PROCESS.md)

---

**使用提示**:
1. 按照5分钟流程系统诊断
2. 优先选择高ROI的修复目标
3. 使用检查清单确保修复完整性
4. 记录常见问题和解决方案

**更新日期**: 2025-12-03
**版本**: v1.0
**使用场景**: 紧急问题快速定位
