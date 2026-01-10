# 测试覆盖率指南

## 概述

本文档说明如何运行测试、查看测试覆盖率并确保代码质量达标。

## 目标

- **单元测试覆盖率**: ≥ 60%
- **关键模块覆盖率**: ≥ 80%
- **API契约测试覆盖率**: 100%

## 运行测试

### 1. 运行所有测试

```bash
# 在项目根目录执行
mvn clean test
```

### 2. 运行特定测试类

```bash
# 运行单个测试类
mvn test -Dtest=ProcessInstanceControllerTest

# 运行多个测试类
mvn test -Dtest=ProcessInstanceControllerTest,CamundaMetricsConfigurationTest
```

### 3. 运行测试并生成覆盖率报告

```bash
# 运行测试并生成Jacoco报告
mvn clean test jacoco:report

# 查看覆盖率报告
open target/site/jacoco/index.html
```

### 4. 生成XML格式报告（用于CI/CD）

```bash
# 生成XML格式的覆盖率报告
mvn test jacoco:report-aggregate
```

## 测试类型

### 单元测试 (Unit Tests)

位于 `src/test/java`，使用 `@WebMvcTest`、`@MockBean` 等注解。

```java
@WebMvcTest(ProcessInstanceController.class)
class ProcessInstanceControllerTest {
    // 测试Controller层
}
```

### 集成测试 (Integration Tests)

使用 `@SpringBootTest` 进行完整应用上下文测试。

```java
@SpringBootTest
class WorkflowIntegrationTest {
    // 测试完整流程
}
```

### API契约测试 (Contract Tests)

验证前后端API契约的一致性。

```java
@Test
void terminate_shouldAcceptPostWithReason() throws Exception {
    mockMvc.perform(post("/api/camunda/process-instances/123/terminate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\": \"User cancelled\"}"))
            .andExpect(status().isOk());
}
```

## 覆盖率检查

### 1. 命令行检查

```bash
# 检查覆盖率是否达标
mvn jacoco:check
```

### 2. 配置覆盖率阈值

在 `pom.xml` 中配置：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3. 查看详细报告

覆盖率报告位于：
- HTML报告: `target/site/jacoco/index.html`
- CSV报告: `target/site/jacoco/jacoco.csv`
- XML报告: `target/site/jacoco/jacoco.xml`

## 最佳实践

### 1. 测试命名规范

```java
@Test
void shouldReturnOk_whenTerminateValidInstance() {
    // 测试方法名应描述测试场景
}
```

### 2. 测试结构 (Given-When-Then)

```java
@Test
void shouldClaimTaskSuccessfully() {
    // Given: 准备测试数据
    String taskId = createTestTask();
    ClaimTaskRequest request = ClaimTaskRequest.builder()
        .taskId(taskId)
        .userId("user123")
        .build();

    // When: 执行测试操作
    taskService.claimTask(request);

    // Then: 验证结果
    assertThat(taskService.getTask(taskId).getAssignee())
        .isEqualTo("user123");
}
```

### 3. Mock使用规范

```java
@WebMvcTest(ProcessInstanceController.class)
class ProcessInstanceControllerTest {

    @MockBean
    private ProcessInstanceService processInstanceService;

    // 只Mock需要的Service，其他依赖使用真实实现
}
```

### 4. 异常测试

```java
@Test
void shouldThrowException_whenTaskNotFound() {
    // Given
    when(processInstanceService.getTask("invalid-id"))
        .thenThrow(new EntityNotFoundException("Task not found"));

    // When & Then
    assertThatThrownBy(() -> processInstanceService.getTask("invalid-id"))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Task not found");
}
```

### 5. 参数化测试

```java
@ParameterizedTest
@ValueSource(strings = {"", " ", "invalid"})
void shouldRejectInvalidTerminateReason(String reason) {
    // 测试多种输入参数
}
```

## CI/CD集成

### 1. GitHub Actions配置示例

```yaml
name: Test and Coverage

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Run tests
        run: mvn clean test
      - name: Check coverage
        run: mvn jacoco:check
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: target/site/jacoco/jacoco.xml
```

### 2. 质量门禁

在CI/CD流水线中设置质量门禁：
- 测试覆盖率 < 60% 失败
- 关键模块覆盖率 < 80% 失败
- 有未修复的测试失败失败

## 常见问题

### Q1: 如何提高测试覆盖率？

A1: 识别未覆盖的代码分支，添加测试用例：
```bash
# 查看未覆盖的行
open target/site/jacoco/index.html
```

### Q2: 如何测试私有方法？

A2: 通过公共方法间接测试，或使用反射但不推荐。

### Q3: 如何处理外部依赖？

A3: 使用@MockBean模拟外部服务。

### Q4: 集成测试运行缓慢怎么办？

A4: 使用@WebMvcTest进行Controller测试，只加载必要组件。

## 参考资源

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://site.mockito.org/)
- [AssertJ Core](https://assertj.github.io/assertj/)
- [Jacoco Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
