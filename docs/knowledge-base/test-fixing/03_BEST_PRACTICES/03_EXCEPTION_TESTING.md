# 异常测试最佳实践 (Exception Testing Best Practices)

## 📋 概述

异常测试是确保应用程序健壮性的关键部分。正确的异常测试能够验证错误处理逻辑、边界条件处理和系统的容错能力。本文档总结了异常测试的完整最佳实践。

## 🎯 核心原则

### 1. 明确异常期望
测试应该明确期望哪种异常，以及异常的详细信息。

### 2. 覆盖边界情况
不仅测试正常流程，更要测试异常流程和边界情况。

### 3. 验证异常上下文
确保异常携带正确的错误信息、状态码等上下文信息。

### 4. 测试恢复逻辑
验证系统从异常状态恢复的能力。

## 🔧 JUnit 5异常测试方法

### 方法1: assertThrows（推荐）

```java
// ✅ 基本异常测试
@Test
void testThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> {
        service.validateUser(null);  // 传入null应该抛出异常
    });
}

// ✅ 验证异常消息
@Test
void testExceptionMessage() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.validateUser("invalid-id")
    );

    assertEquals("User ID cannot be empty", exception.getMessage());
    assertTrue(exception.getMessage().contains("empty"));
}

// ✅ 验证异常属性
@Test
void testExceptionProperties() {
    ValidationException exception = assertThrows(
        ValidationException.class,
        () -> service.createUser(createInvalidUser())
    );

    assertEquals("VALIDATION_FAILED", exception.getErrorCode());
    assertEquals(400, exception.getStatusCode());
    assertEquals("user", exception.getField());
}
```

### 方法2: Lambda表达式异常测试

```java
// ✅ 使用Lambda表达式
@Test
void testServiceExceptionWithLambda() {
    // ✅ 推荐: Lambda表达式更清晰
    assertThrows(RuntimeException.class, () -> {
        service.processData(null);
    });
}

// ❌ 不推荐: 旧式try-catch
@Test
void testServiceExceptionWithTryCatch() {
    try {
        service.processData(null);
        fail("Expected RuntimeException");  // ❌ 代码冗长
    } catch (RuntimeException e) {
        assertNotNull(e.getMessage());
    }
}
```

### 方法3: assertDoesNotThrow

```java
// ✅ 验证方法不抛出异常
@Test
void testDoesNotThrowException() {
    User validUser = TestDataFactory.createValidUser();

    assertDoesNotThrow(() -> {
        service.createUser(validUser);
    });
}

// ✅ 验证部分操作不抛出异常
@Test
void testPartialSuccess() {
    List<User> users = Arrays.asList(
        TestDataFactory.createValidUser(),
        TestDataFactory.createValidUser()
    );

    assertDoesNotThrow(() -> {
        userService.saveAll(users);
    });
}
```

## 📊 复杂异常场景测试

### 场景1: 嵌套异常

```java
// ✅ 验证嵌套异常
@Test
void testNestedException() {
    RuntimeException rootCause = new RuntimeException("Database connection failed");

    ServiceException exception = assertThrows(ServiceException.class, () -> {
        try {
            service.performOperation();
        } catch (Exception e) {
            throw new ServiceException("Operation failed", rootCause);
        }
    });

    // 验证根因
    assertEquals("Database connection failed", exception.getCause().getMessage());
    assertEquals(rootCause, exception.getCause());
}
```

### 场景2: 自定义异常

```java
// ✅ 测试自定义异常类型
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final Map<String, Object> details;

    public BusinessException(String message, String errorCode, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }
}

@Test
void testBusinessException() {
    Map<String, Object> details = Map.of("field", "userId", "value", "invalid");
    BusinessException exception = assertThrows(BusinessException.class, () -> {
        service.processBusinessLogic(createInvalidBusinessData());
    });

    assertEquals("BUSINESS_RULE_VIOLATION", exception.getErrorCode());
    assertEquals(details.get("field"), exception.getDetails().get("field"));
}
```

### 场景3: 异常顺序测试

```java
// ✅ 测试多个异常抛出点
@Test
void testMultipleExceptionPoints() {
    // 第一个异常点: 空输入
    assertThrows(IllegalArgumentException.class, () -> {
        service.processData(null);
    });

    // 第二个异常点: 无效数据
    assertThrows(ValidationException.class, () -> {
        service.processData("invalid-data");
    });

    // 第三个异常点: 权限不足
    assertThrows(AccessDeniedException.class, () -> {
        service.processData(createForbiddenData());
    });
}
```

## 🛠️ 异常测试辅助方法

### 辅助方法1: 异常消息验证器

```java
// ✅ 异常消息验证器
public class ExceptionTestUtils {

    private ExceptionTestUtils() {
        // 工具类
    }

    public static <T extends Throwable> T assertExceptionWithMessage(
            Class<T> exceptionClass,
            Executable executable,
            String expectedMessage) {

        T exception = assertThrows(exceptionClass, executable);
        assertTrue(exception.getMessage().contains(expectedMessage),
                   "Expected message to contain: " + expectedMessage +
                   ", but was: " + exception.getMessage());
        return exception;
    }

    public static <T extends Throwable> T assertExceptionWithCode(
            Class<T> exceptionClass,
            Executable executable,
            String expectedCode) {

        // 假设异常有getCode()方法
        T exception = assertThrows(exceptionClass, executable);
        // 根据异常类型进行验证
        return exception;
    }
}

// 使用示例
@Test
void testExceptionWithUtils() {
    ExceptionTestUtils.assertExceptionWithMessage(
        IllegalArgumentException.class,
        () -> service.validateUser(null),
        "User cannot be null"
    );
}
```

### 辅助方法2: 异常捕获器

```java
// ✅ 可重用的异常捕获器
public class ExceptionCaptor {

    private List<Throwable> capturedExceptions = new ArrayList<>();

    public Executable capture(Runnable executable) {
        return () -> {
            try {
                executable.run();
            } catch (Throwable throwable) {
                capturedExceptions.add(throwable);
            }
        };
    }

    public List<Throwable> getCapturedExceptions() {
        return new ArrayList<>(capturedExceptions);
    }

    public void assertSingleException(Class<? extends Throwable> expectedClass) {
        assertEquals(1, capturedExceptions.size());
        assertTrue(expectedClass.isInstance(capturedExceptions.get(0)));
    }

    public void assertExceptionCount(int expectedCount) {
        assertEquals(expectedCount, capturedExceptions.size());
    }
}

// 使用示例
@Test
void testMultipleExceptions() {
    ExceptionCaptor captor = new ExceptionCaptor();

    // 捕获多个异常
    captor.capture(() -> service.method1());  // 正常，不抛出
    captor.capture(() -> service.method2());  // 抛出异常
    captor.capture(() -> service.method3());  // 抛出异常

    captor.assertExceptionCount(2);
}
```

## 📝 异常测试策略

### 策略1: 参数化异常测试

```java
// ✅ 参数化测试异常
@ParameterizedTest
@CsvSource({
    "null",
    "''",
    "' '",
    "'\t'",
    "'\n'"
})
void testInvalidInputs(String invalidInput) {
    assertThrows(IllegalArgumentException.class, () -> {
        service.processInput(invalidInput);
    });
}

// ✅ 边界值异常测试
@ParameterizedTest
@ValueSource(ints = {Integer.MIN_VALUE, -1, 0})
void testInvalidNumbers(int invalidNumber) {
    assertThrows(IllegalArgumentException.class, () -> {
        service.calculate(invalidNumber);
    });
}
```

### 策略2: 异常继承层次测试

```java
// ✅ 测试异常继承
@Test
void testExceptionHierarchy() {
    // RuntimeException是 IllegalArgumentException 的父类
    assertThrows(RuntimeException.class, () -> {
        service.validateUser(null);
    });

    // 更具体的异常
    assertThrows(IllegalArgumentException.class, () -> {
        service.validateUser(null);
    });
}

// ✅ 测试多个异常类型
@Test
void testMultipleExceptionTypes() {
    assertThrowsAny(RuntimeException.class, () -> {
        service.processData(null);
    });
}
```

### 策略3: 异常前后状态验证

```java
// ✅ 验证异常前后状态
@Test
void testStateAfterException() {
    User originalUser = userRepository.findById("user-id").orElseThrow();

    assertThrows(ServiceException.class, () -> {
        service.updateUser("user-id", createInvalidUpdate());
    });

    // 验证状态未改变
    User currentUser = userRepository.findById("user-id").orElseThrow();
    assertEquals(originalUser, currentUser);
    assertEquals(originalUser.getVersion(), currentUser.getVersion());
}
```

## 🚨 异常测试常见陷阱

### 陷阱1: 未验证异常类型

```java
// ❌ 错误: 只验证代码执行，未验证异常
@Test
void testInvalidInput() {
    try {
        service.processData(null);
        // ❌ 缺少异常验证
    } catch (Exception e) {
        // ❌ 捕获所有异常，不具体
    }
}

// ✅ 正确: 明确验证异常类型
@Test
void testInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> {
        service.processData(null);
    });
}
```

### 陷阱2: 忽略异常消息

```java
// ❌ 错误: 不验证异常消息
@Test
void testException() {
    assertThrows(IllegalArgumentException.class, () -> {
        service.validateUser(null);
    });
    // ❌ 不知道抛出什么异常消息
}

// ✅ 正确: 验证异常消息
@Test
void testException() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.validateUser(null)
    );
    assertEquals("User cannot be null", exception.getMessage());
}
```

### 陷阱3: 测试异常但忽略恢复

```java// ✅ 错误: 只测试异常，不测试恢复
@Test
void testRetryLogic() {
    assertThrows(TemporaryException.class, () -> {
        service.performOperation();  // 第一次失败
    });
    // ❌ 没有验证重试逻辑
}

// ✅ 正确: 测试异常和恢复
@Test
void testRetryLogic() {
    // 模拟前两次失败，第三次成功
    when(service.performOperation())
        .thenThrow(new TemporaryException("Temporary error"))
        .thenThrow(new TemporaryException("Temporary error"))
        .thenReturn("success");

    String result = service.performWithRetry();

    assertEquals("success", result);
    verify(service, times(3)).performOperation();  // 验证重试次数
}
```

## 🔄 异常恢复测试

### 1. 重试机制测试

```java
// ✅ 重试机制
@Test
void testRetryWithSuccess() {
    RetryableException firstFailure = new RetryableException("Temporary failure");
    RetryableException secondFailure = new RetryableException("Temporary failure");

    when(service.execute())
        .thenThrow(firstFailure)
        .thenThrow(secondFailure)
        .thenReturn("success");

    String result = service.executeWithRetry(3);

    assertEquals("success", result);
    verify(service, times(3)).execute();
}

// ✅ 重试超限
@Test
void testRetryExhausted() {
    when(service.execute()).thenThrow(new RetryableException("Always fails"));

    RetryExhaustedException exception = assertThrows(
        RetryExhaustedException.class,
        () -> service.executeWithRetry(3)
    );

    assertEquals(3, exception.getAttemptCount());
}
```

### 2. 补偿事务测试

```java
// ✅ 补偿事务
@Test
void testCompensationTransaction() {
    // 主操作成功
    when(repository.save()).thenReturn(entity);

    try {
        // 子操作失败
        service.performCompoundOperation();
        fail("Expected ServiceException");
    } catch (ServiceException e) {
        // 验证补偿操作被调用
        verify(compensationService).compensate(entity);
    }
}
```

## 📊 异常测试工具

### 工具1: 异常模拟器

```java
// ✅ 异常模拟器
@Component
public class ExceptionSimulator {

    private boolean shouldThrowException = false;
    private RuntimeException exceptionToThrow;

    public void throwException(RuntimeException exception) {
        this.shouldThrowException = true;
        this.exceptionToThrow = exception;
    }

    public void reset() {
        this.shouldThrowException = false;
        this.exceptionToThrow = null;
    }

    public void checkAndThrow() {
        if (shouldThrowException && exceptionToThrow != null) {
            throw exceptionToThrow;
        }
    }
}

// 使用示例
@Test
void testWithSimulatedException() {
    exceptionSimulator.throwException(new ServiceException("Simulated error"));

    assertThrows(ServiceException.class, () -> {
        service.performOperation();
    });

    exceptionSimulator.reset();
}
```

### 工具2: 异常规则

```java
// ✅ JUnit异常规则
public class ExpectedExceptionRule implements TestRule {
    private Class<? extends Throwable> expectedException;
    private String expectedMessage;

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (expectedException != null) {
                    try {
                        base.evaluate();
                        throw new AssertionError("Expected " + expectedException.getName());
                    } catch (Throwable t) {
                        if (!expectedException.isInstance(t)) {
                            throw t;
                        }
                        if (expectedMessage != null && !t.getMessage().contains(expectedMessage)) {
                            throw t;
                        }
                    }
                } else {
                    base.evaluate();
                }
            }
        };
    }

    public ExpectedExceptionRule expect(Class<? extends Throwable> clazz) {
        this.expectedException = clazz;
        return this;
    }

    public ExpectedExceptionRule expectMessage(String message) {
        this.expectedMessage = message;
        return this;
    }
}
```

## 🎯 异常测试最佳实践总结

### 1. 测试覆盖原则

```java
// ✅ 完整的异常测试覆盖
@Test
void testCompleteExceptionFlow() {
    // 1. 正常情况
    assertDoesNotThrow(() -> service.processValidData());

    // 2. 参数验证异常
    assertThrows(IllegalArgumentException.class, () -> service.processData(null));

    // 3. 业务逻辑异常
    assertThrows(BusinessException.class, () -> service.processInvalidBusinessData());

    // 4. 系统异常
    assertThrows(SystemException.class, () -> service.processDataWithSystemFailure());

    // 5. 恢复逻辑
    when(service.execute()).thenThrow(new TemporaryException()).thenReturn("success");
    assertDoesNotThrow(() -> service.executeWithRetry());
}
```

### 2. 异常文档化

```java
// ✅ 通过测试文档化异常行为
/**
 * 测试用例文档化:
 * - 传入null时抛出IllegalArgumentException
 * - 异常消息: "Input cannot be null"
 */
@Test
void testNullInput() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> service.processData(null)
    );
    assertEquals("Input cannot be null", exception.getMessage());
}
```

## 📚 相关文档

- [模式1: 辅助方法模式](../02_PATTERNS/PATTERN_01_HELPER_METHOD.md) - 异常测试辅助方法
- [模式2: 分层Mock配置](../02_PATTERNS/PATTERN_02_LAYERED_MOCK.md) - 异常Mock配置
- [诊断流程](../01_ARCHITECTURE/03_DIAGNOSIS_PROCESS.md) - 异常问题诊断

---

**使用提示**:
1. 优先使用assertThrows进行异常测试
2. 验证异常的完整信息（类型、消息、上下文）
3. 测试异常恢复和重试逻辑
4. 使用参数化测试覆盖边界值

**更新日期**: 2025-12-03
**版本**: v1.0
**应用频率**: ⭐⭐⭐⭐ (高)
