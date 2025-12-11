# 循环依赖问题修复总结

## 概述

在启动 user-api 服务时，遇到了多个循环依赖问题。本文档记录了所有问题的原因和解决方案。

## 问题列表

### 1. Bean 冲突：SqlStatisticsCollector

#### 问题
```
Parameter 0 of method sqlStatisticsInterceptor required a single bean, but 2 were found:
- sqlStatisticsCollector
- mySqlStatisticsCollector
```

#### 原因
`SqlStatisticsCollector` 被定义了两次：
- 类上使用 `@Component` 注解
- 配置类中使用 `@Bean` 方法

#### 解决方案
移除 `SqlStatisticsCollector` 类上的 `@Component` 注解。

**修改文件：**
- `basebackend-database/src/main/java/com/basebackend/database/statistics/collector/SqlStatisticsCollector.java`

---

### 2. 循环依赖：SqlStatisticsInterceptor

#### 问题
```
Error creating bean with name 'sqlSessionFactory': 
Requested bean is currently in creation: Is there an unresolvable circular reference?

循环链：
SqlSessionFactory → SqlStatisticsInterceptor → SqlStatisticsCollector → SqlStatisticsMapper → SqlSessionFactory
```

#### 原因
- `SqlSessionFactory` 创建时需要注册 `SqlStatisticsInterceptor`
- `SqlStatisticsInterceptor` 依赖 `SqlStatisticsCollector`
- `SqlStatisticsCollector` 依赖 `SqlStatisticsMapper`
- `SqlStatisticsMapper` 需要 `SqlSessionFactory`

#### 解决方案
在 `SqlStatisticsConfig` 中使用 `@Lazy` 注解打破循环依赖。

**修改文件：**
- `basebackend-database/src/main/java/com/basebackend/database/statistics/config/SqlStatisticsConfig.java`

**关键代码：**
```java
@Bean
public SqlStatisticsInterceptor sqlStatisticsInterceptor(
        @Lazy SqlStatisticsCollector sqlStatisticsCollector,
        DatabaseEnhancedProperties properties) {
    return new SqlStatisticsInterceptor(sqlStatisticsCollector, properties);
}

@Bean
public SqlStatisticsCollector sqlStatisticsCollector(
        @Lazy SqlStatisticsMapper sqlStatisticsMapper,
        DatabaseEnhancedProperties properties) {
    return new SqlStatisticsCollector(sqlStatisticsMapper, properties);
}
```

---

### 3. 循环依赖：TenantDataSourceConfig

#### 问题
```
The dependencies of some of the beans in the application context form a cycle:
sqlSessionFactory → tenantDataSourceConfig → tenantConfigServiceImpl → tenantConfigMapper → sqlSessionFactory
```

#### 原因
- `SqlSessionFactory` 创建时需要 `TenantDataSourceConfig`
- `TenantDataSourceConfig` 依赖 `TenantConfigService`
- `TenantConfigService` 实现类依赖 `TenantConfigMapper`
- `TenantConfigMapper` 需要 `SqlSessionFactory`

#### 解决方案
在 `TenantDataSourceConfig` 的构造函数中使用 `@Lazy` 注解。

**修改文件：**
- `basebackend-database/src/main/java/com/basebackend/database/tenant/config/TenantDataSourceConfig.java`

**关键代码：**
```java
@Configuration
@ConditionalOnProperty(prefix = "database.enhanced.multi-tenancy", name = "enabled", havingValue = "true")
public class TenantDataSourceConfig {
    
    private final DatabaseEnhancedProperties properties;
    private final TenantConfigService tenantConfigService;
    private final TenantDataSourceService tenantDataSourceService;
    
    /**
     * Constructor with @Lazy to break circular dependency with SqlSessionFactory
     */
    public TenantDataSourceConfig(
            DatabaseEnhancedProperties properties,
            @Lazy TenantConfigService tenantConfigService,
            @Lazy TenantDataSourceService tenantDataSourceService) {
        this.properties = properties;
        this.tenantConfigService = tenantConfigService;
        this.tenantDataSourceService = tenantDataSourceService;
    }
    // ...
}
```

---

## 解决方案总结

### @Lazy 注解的使用

`@Lazy` 注解是解决循环依赖的关键：

1. **作用**：延迟 Bean 的实例化，创建代理对象
2. **时机**：当第一次调用代理对象的方法时，才会真正创建 Bean
3. **效果**：打破循环依赖链

### 使用场景

在以下情况下使用 `@Lazy`：

1. **MyBatis 拦截器**：拦截器依赖 Mapper，而 Mapper 需要 SqlSessionFactory
2. **配置类依赖**：配置类依赖需要 SqlSessionFactory 的服务
3. **复杂依赖链**：多个 Bean 之间形成循环依赖

### 最佳实践

1. **优先使用 @Lazy**
   - 对于 MyBatis 相关的依赖，优先使用 `@Lazy`
   - 在构造函数参数或 `@Bean` 方法参数上使用

2. **避免循环依赖**
   - 设计时考虑依赖关系
   - 使用事件驱动或异步处理解耦

3. **使用事件监听**
   - 对于初始化操作，使用 `ApplicationReadyEvent`
   - 在应用完全启动后执行

4. **移除重复定义**
   - 避免同时使用 `@Component` 和 `@Bean`
   - 优先使用 `@Bean` + `@ConditionalOnProperty`

---

## 修改的文件列表

### 1. SqlStatisticsCollector
- **文件**：`basebackend-database/src/main/java/com/basebackend/database/statistics/collector/SqlStatisticsCollector.java`
- **修改**：移除 `@Component` 注解

### 2. SqlStatisticsConfig
- **文件**：`basebackend-database/src/main/java/com/basebackend/database/statistics/config/SqlStatisticsConfig.java`
- **修改**：在 Bean 方法参数上添加 `@Lazy` 注解

### 3. TenantDataSourceConfig
- **文件**：`basebackend-database/src/main/java/com/basebackend/database/tenant/config/TenantDataSourceConfig.java`
- **修改**：
  - 移除 `@RequiredArgsConstructor` 注解
  - 手动编写构造函数
  - 在构造函数参数上添加 `@Lazy` 注解

### 4. MyBatisPlusConfig
- **文件**：`basebackend-database/src/main/java/com/basebackend/database/config/MyBatisPlusConfig.java`
- **修改**：移除重复的 `sqlStatisticsInterceptor` Bean 定义

### 5. TROUBLESHOOTING.md
- **文件**：`basebackend-database/TROUBLESHOOTING.md`
- **修改**：添加循环依赖问题的详细说明和解决方案

---

## 验证结果

### 编译测试
```bash
# database 模块编译成功
mvn clean compile -pl basebackend-database -DskipTests
# BUILD SUCCESS

# user-api 模块编译成功
mvn clean compile -pl basebackend-user-api -DskipTests
# BUILD SUCCESS
```

### 启动测试
所有循环依赖问题已解决，应用可以正常启动。

---

## 预防措施

### 1. 代码审查
- 检查新增的配置类是否引入循环依赖
- 审查 Bean 定义，避免重复

### 2. 设计原则
- 遵循单一职责原则
- 减少 Bean 之间的直接依赖
- 使用接口和抽象类解耦

### 3. 测试策略
- 单元测试覆盖配置类
- 集成测试验证 Bean 创建
- 启动测试检查循环依赖

### 4. 文档维护
- 记录复杂的依赖关系
- 更新故障排查文档
- 提供解决方案示例

---

## 参考资料

### Spring 官方文档
- [Circular Dependencies](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-dependency-resolution)
- [Lazy Initialization](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-factory-lazy-init)

### 相关文档
- [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - 完整的故障排查指南
- [DATABASE_ENHANCEMENT_README.md](DATABASE_ENHANCEMENT_README.md) - 数据库增强功能说明

---

## 总结

通过使用 `@Lazy` 注解和移除重复的 Bean 定义，成功解决了所有循环依赖问题。关键点：

1. ✅ 识别循环依赖链
2. ✅ 使用 `@Lazy` 打破循环
3. ✅ 移除重复定义
4. ✅ 更新文档
5. ✅ 验证修复

所有修改已通过编译测试，应用可以正常启动。
