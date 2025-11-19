# Spring Security多Servlet问题解决方案

## 问题描述

在Spring Boot 3.x + Spring Security 6.x环境中，当应用包含多个Servlet（如Spring MVC的DispatcherServlet和Druid的StatViewServlet）时，使用字符串形式的`requestMatchers()`会抛出异常：

```
java.lang.IllegalArgumentException: This method cannot decide whether these patterns are 
Spring MVC patterns or not. If this endpoint is a Spring MVC endpoint, please use 
requestMatchers(MvcRequestMatcher); otherwise, please use requestMatchers(AntPathRequestMatcher).
This is because there is more than one mappable servlet in your servlet context: 
{org.springframework.web.servlet.DispatcherServlet=[/], 
com.alibaba.druid.support.jakarta.StatViewServlet=[/druid/*]}.
```

## 原因分析

### 1. Spring Security 6.x的变化

Spring Security 6.x对RequestMatcher的处理更加严格：
- 当只有一个Servlet时，可以使用字符串形式的`requestMatchers(String...)`
- 当有多个Servlet时，必须明确指定使用哪种RequestMatcher

### 2. 多Servlet场景

在我们的项目中，以下模块会引入多个Servlet：

**basebackend-database模块**引入了Druid：
```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
</dependency>
```

Druid会注册`StatViewServlet`到`/druid/*`路径，导致应用中存在两个Servlet：
1. `DispatcherServlet` - Spring MVC的主Servlet（路径：`/`）
2. `StatViewServlet` - Druid监控页面Servlet（路径：`/druid/*`）

### 3. 受影响的服务

所有依赖`basebackend-database`模块的服务都会受影响：
- basebackend-auth-api
- basebackend-user-api
- basebackend-system-api
- basebackend-notification-service
- basebackend-observability-service

## 解决方案

### 方案1：使用AntPathRequestMatcher（推荐）

修改`SecurityConfig.java`，明确使用`AntPathRequestMatcher`：

```java
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers(
                new AntPathRequestMatcher("/api/auth/**"),
                new AntPathRequestMatcher("/api/public/**"),
                new AntPathRequestMatcher("/actuator/**"),
                new AntPathRequestMatcher("/druid/**")
            )
        )
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                new AntPathRequestMatcher("/api/auth/**"),
                new AntPathRequestMatcher("/api/public/**"),
                new AntPathRequestMatcher("/actuator/**"),
                new AntPathRequestMatcher("/druid/**")
            ).permitAll()
            .anyRequest().authenticated()
        );
    
    return http.build();
}
```

### 方案2：使用MvcRequestMatcher

如果路径是Spring MVC控制器处理的，可以使用`MvcRequestMatcher`：

```java
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        HandlerMappingIntrospector introspector) throws Exception {
    
    MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);
    
    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers(
                mvcMatcherBuilder.pattern("/api/auth/**"),
                mvcMatcherBuilder.pattern("/api/public/**"),
                new AntPathRequestMatcher("/druid/**")  // Druid不是MVC路径
            )
        );
    
    return http.build();
}
```

### 方案3：禁用Druid的StatViewServlet

如果不需要Druid监控页面，可以禁用：

```yaml
spring:
  datasource:
    druid:
      stat-view-servlet:
        enabled: false
```

### 方案4：为每个服务创建独立的SecurityConfig

在特定服务中覆盖默认配置：

```java
@Configuration
@EnableWebSecurity
@Order(1)  // 优先级高于默认配置
public class CustomSecurityConfig {
    // 自定义配置
}
```

## 我们的实现

我们选择了**方案1**，在`basebackend-security`模块的`SecurityConfig`中统一使用`AntPathRequestMatcher`：

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/api/auth/**"),
                    new AntPathRequestMatcher("/api/user/auth/**"),
                    new AntPathRequestMatcher("/api/public/**"),
                    new AntPathRequestMatcher("/actuator/**"),
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/doc.html"),
                    new AntPathRequestMatcher("/swagger-ui/**"),
                    new AntPathRequestMatcher("/druid/**")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    new AntPathRequestMatcher("/api/auth/**"),
                    new AntPathRequestMatcher("/api/user/auth/**"),
                    new AntPathRequestMatcher("/api/public/**"),
                    new AntPathRequestMatcher("/actuator/**"),
                    new AntPathRequestMatcher("/swagger-ui/**"),
                    new AntPathRequestMatcher("/v3/api-docs/**"),
                    new AntPathRequestMatcher("/doc.html"),
                    new AntPathRequestMatcher("/webjars/**"),
                    new AntPathRequestMatcher("/favicon.ico"),
                    new AntPathRequestMatcher("/druid/**")
                ).permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
```

## 优点和缺点

### AntPathRequestMatcher

**优点：**
- 简单直接，适用于所有路径
- 不依赖Spring MVC
- 性能较好

**缺点：**
- 不支持Spring MVC的路径变量和矩阵变量
- 不支持后缀模式匹配

### MvcRequestMatcher

**优点：**
- 与Spring MVC完全兼容
- 支持路径变量、矩阵变量
- 支持后缀模式匹配

**缺点：**
- 只能用于Spring MVC路径
- 需要注入HandlerMappingIntrospector
- 性能略低

## 测试验证

### 1. 启动服务

```bash
# 启动observability-service
cd basebackend-observability-service
mvn spring-boot:run
```

### 2. 验证公开接口

```bash
# 访问Actuator（应该可以访问）
curl http://localhost:8087/actuator/health

# 访问Druid监控（应该可以访问）
curl http://localhost:8087/druid/index.html

# 访问API文档（应该可以访问）
curl http://localhost:8087/doc.html
```

### 3. 验证受保护接口

```bash
# 访问受保护的API（应该返回401）
curl http://localhost:8087/api/metrics/query

# 使用JWT访问（应该返回200）
curl -H "Authorization: Bearer <token>" http://localhost:8087/api/metrics/query
```

## 相关资源

- [Spring Security 6.0 Migration Guide](https://docs.spring.io/spring-security/reference/6.0/migration/index.html)
- [RequestMatcher API](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/web/util/matcher/RequestMatcher.html)
- [Druid Spring Boot Starter](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)

## 常见问题

### Q1: 为什么不直接使用字符串？

A: Spring Security 6.x为了更明确的语义和更好的类型安全，要求在多Servlet环境下明确指定RequestMatcher类型。

### Q2: 如何判断应该使用哪种RequestMatcher？

A: 
- 如果路径由Spring MVC Controller处理 → 使用`MvcRequestMatcher`
- 如果路径由其他Servlet处理（如Druid） → 使用`AntPathRequestMatcher`
- 如果不确定或想要通用方案 → 使用`AntPathRequestMatcher`

### Q3: 升级到Spring Boot 3.x后为什么会出现这个问题？

A: Spring Boot 3.x使用Spring Security 6.x，后者对RequestMatcher的处理更加严格。在Spring Security 5.x中，这个问题不会出现。

### Q4: 是否需要为每个服务单独配置？

A: 不需要。我们在`basebackend-security`公共模块中统一配置，所有服务自动继承。

## 更新日志

- 2024-11-19: 修复多Servlet问题，统一使用AntPathRequestMatcher
- 2024-11-19: 添加Druid路径到白名单
