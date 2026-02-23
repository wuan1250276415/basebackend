# BaseBackend Scheduler 测试指南

## 测试运行

### 运行所有测试
```bash
mvn test
```

### 运行特定测试类
```bash
mvn test -Dtest=SendEmailDelegateTest
mvn test -Dtest=ProcessDefinitionServiceImplTest
```

### 运行测试并生成覆盖率报告
```bash
mvn clean test
```

### 查看覆盖率报告
```bash
open target/site/jacoco/index.html
```

## 测试结构

```
src/test/java/com/basebackend/scheduler/
├── camunda/
│   ├── delegate/          # JavaDelegate测试
│   │   ├── SendEmailDelegateTest.java
│   │   ├── DataSyncDelegateTest.java
│   │   ├── OrderApprovalDelegateTest.java
│   │   └── MicroserviceCallDelegateTest.java
│   ├── service/impl/      # Service实现测试
│   │   ├── ProcessDefinitionServiceImplTest.java
│   │   ├── TaskManagementServiceImplTest.java
│   │   ├── HistoricProcessInstanceServiceImplTest.java
│   │   ├── ProcessStatisticsServiceImplTest.java
│   │   └── FormTemplateServiceImplTest.java
│   └── config/
│       └── TestConfig.java    # 测试配置
└── resources/
    └── application-test.yml   # 测试环境配置
```

## 测试框架

- **JUnit 5** - 测试框架
- **Mockito** - Mock框架
- **AssertJ** - 断言库
- **JaCoCo** - 覆盖率工具

## 覆盖率目标

- 行覆盖率: 80%
- 分支覆盖率: 75%

## 测试最佳实践

1. 每个测试方法独立运行
2. 使用@BeforeEach准备测试数据
3. 使用Mock避免外部依赖
4. 测试方法命名清晰明确
5. 断言结果验证完整

## 注意事项

- 测试使用H2内存数据库
- Mock了Camunda服务
- 测试配置通过@ActiveProfiles("test")激活
