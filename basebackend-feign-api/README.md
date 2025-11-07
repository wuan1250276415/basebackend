# Feign API 模块使用指南

## 📖 模块介绍

`basebackend-feign-api` 模块是一个独立的 Feign 客户端接口模块，用于定义服务间调用的 API 接口。通过将 Feign 接口抽象到独立模块，实现了服务间调用的标准化和解耦。

## 🎯 设计目标

1. **解耦服务**: 调用方无需直接依赖被调用服务的实现
2. **统一接口**: 统一定义服务间调用的接口规范
3. **降级处理**: 提供完善的服务降级和容错机制
4. **类型安全**: 使用强类型 DTO，避免参数错误

## 📦 模块结构

```
basebackend-feign-api/
├── src/main/java/com/basebackend/feign/
│   ├── client/                      # Feign 客户端接口
│   │   ├── UserFeignClient.java    # 用户服务客户端
│   │   └── DeptFeignClient.java    # 部门服务客户端
│   ├── dto/                         # 数据传输对象
│   │   ├── user/
│   │   │   └── UserBasicDTO.java   # 用户基础信息
│   │   └── dept/
│   │       └── DeptBasicDTO.java   # 部门基础信息
│   ├── fallback/                    # 降级处理
│   │   ├── UserFeignFallbackFactory.java
│   │   └── DeptFeignFallbackFactory.java
│   └── constant/                    # 常量定义
│       └── FeignServiceConstants.java
└── pom.xml
```

## 🔧 快速开始

### 1. 添加依赖

在需要使用 Feign 调用的模块中添加依赖：

```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-feign-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 启用 Feign 客户端

在启动类上添加 `@EnableFeignClients` 注解：

```java
@SpringBootApplication
@EnableFeignClients(basePackages = "com.basebackend.feign.client")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 注入并使用

```java
@Service
@RequiredArgsConstructor
public class SomeService {

    private final UserFeignClient userFeignClient;
    private final DeptFeignClient deptFeignClient;

    public void businessLogic() {
        // 调用用户服务
        Result<UserBasicDTO> userResult = userFeignClient.getById(1L);
        if (userResult.isSuccess()) {
            UserBasicDTO user = userResult.getData();
            // 业务处理...
        }

        // 调用部门服务
        Result<List<DeptBasicDTO>> deptResult = deptFeignClient.getChildrenByDeptId(10L);
        if (deptResult.isSuccess()) {
            List<DeptBasicDTO> depts = deptResult.getData();
            // 业务处理...
        }
    }
}
```

## 📚 API 接口列表

### 用户服务接口 (UserFeignClient)

| 方法 | 路径 | 说明 |
|------|------|------|
| `getById(Long id)` | `GET /api/admin/users/{id}` | 根据ID获取用户 |
| `getByUsername(String username)` | `GET /api/admin/users/by-username` | 根据用户名获取用户 |
| `getByPhone(String phone)` | `GET /api/admin/users/by-phone` | 根据手机号获取用户 |
| `getByEmail(String email)` | `GET /api/admin/users/by-email` | 根据邮箱获取用户 |
| `getBatchByIds(String userIds)` | `GET /api/admin/users/batch` | 批量获取用户（ID逗号分隔） |
| `getByDeptId(Long deptId)` | `GET /api/admin/users/by-dept` | 根据部门ID获取用户列表 |
| `getUserRoles(Long userId)` | `GET /api/admin/users/{id}/roles` | 获取用户角色ID列表 |
| `checkUsernameUnique(...)` | `GET /api/admin/users/check-username` | 检查用户名唯一性 |
| `checkEmailUnique(...)` | `GET /api/admin/users/check-email` | 检查邮箱唯一性 |
| `checkPhoneUnique(...)` | `GET /api/admin/users/check-phone` | 检查手机号唯一性 |

### 部门服务接口 (DeptFeignClient)

| 方法 | 路径 | 说明 |
|------|------|------|
| `getDeptTree()` | `GET /api/admin/depts/tree` | 获取部门树 |
| `getDeptList()` | `GET /api/admin/depts` | 获取部门列表 |
| `getById(Long id)` | `GET /api/admin/depts/{id}` | 根据ID获取部门 |
| `getChildrenByDeptId(Long id)` | `GET /api/admin/depts/{id}/children` | 获取子部门列表 |
| `getChildrenDeptIds(Long id)` | `GET /api/admin/depts/{id}/children-ids` | 获取子部门ID列表 |
| `getByDeptName(String deptName)` | `GET /api/admin/depts/by-name` | 根据部门名称获取部门 |
| `getByDeptCode(String deptCode)` | `GET /api/admin/depts/by-code` | 根据部门编码获取部门 |
| `getBatchByIds(String deptIds)` | `GET /api/admin/depts/batch` | 批量获取部门（ID逗号分隔） |
| `getByParentId(Long parentId)` | `GET /api/admin/depts/by-parent` | 根据父部门ID获取直接子部门 |
| `checkDeptNameUnique(...)` | `GET /api/admin/depts/check-dept-name` | 检查部门名称唯一性 |

## 💡 使用示例

### 示例1: 根据用户名获取用户信息

```java
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserFeignClient userFeignClient;

    public UserBasicDTO getUserByUsername(String username) {
        Result<UserBasicDTO> result = userFeignClient.getByUsername(username);

        if (!result.isSuccess()) {
            throw new BusinessException("获取用户信息失败: " + result.getMessage());
        }

        return result.getData();
    }
}
```

### 示例2: 获取部门及其所有子部门

```java
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptFeignClient deptFeignClient;

    public List<Long> getAllChildrenDeptIds(Long deptId) {
        Result<List<Long>> result = deptFeignClient.getChildrenDeptIds(deptId);

        if (result.isSuccess()) {
            return result.getData();
        }

        // 降级处理：返回空列表
        log.warn("获取子部门ID列表失败，返回空列表: {}", result.getMessage());
        return Collections.emptyList();
    }
}
```

### 示例3: 批量获取用户信息

```java
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserFeignClient userFeignClient;

    public List<UserBasicDTO> getBatchUsers(List<Long> userIds) {
        // 将ID列表转为逗号分隔的字符串
        String ids = userIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        Result<List<UserBasicDTO>> result = userFeignClient.getBatchByIds(ids);

        return result.isSuccess() ? result.getData() : Collections.emptyList();
    }
}
```

### 示例4: 检查用户名唯一性

```java
@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final UserFeignClient userFeignClient;

    public void validateUsername(String username, Long userId) {
        Result<Boolean> result = userFeignClient.checkUsernameUnique(username, userId);

        if (!result.isSuccess() || !result.getData()) {
            throw new BusinessException("用户名已存在");
        }
    }
}
```

### 示例5: 获取部门树结构

```java
@Service
@RequiredArgsConstructor
public class DeptTreeService {

    private final DeptFeignClient deptFeignClient;

    public List<DeptBasicDTO> buildDeptTree() {
        Result<List<DeptBasicDTO>> result = deptFeignClient.getDeptTree();

        if (!result.isSuccess()) {
            log.error("获取部门树失败: {}", result.getMessage());
            return Collections.emptyList();
        }

        return result.getData();
    }
}
```

## ⚙️ 配置说明

### 基础配置

```yaml
# Feign 配置
feign:
  client:
    config:
      default:
        connect-timeout: 5000        # 连接超时（毫秒）
        read-timeout: 10000          # 读取超时（毫秒）
        logger-level: basic          # 日志级别

  # 开启熔断
  circuitbreaker:
    enabled: true

  # 启用压缩
  compression:
    request:
      enabled: true
      mime-types: text/xml,application/xml,application/json
      min-request-size: 2048
    response:
      enabled: true
```

### 服务发现配置

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.66.126:8848
        namespace: dev
        group: DEFAULT_GROUP
```

## 🔐 安全注意事项

1. **服务间认证**:
   - Feign 调用应配置服务间认证
   - 可使用 RequestInterceptor 添加认证头

```java
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 添加内部服务认证头
            template.header("X-Internal-Auth", "your-secret-token");
        };
    }
}
```

2. **数据脱敏**:
   - Feign DTO 中的敏感字段应脱敏
   - 不传输不必要的敏感信息

3. **权限控制**:
   - 服务提供方应验证调用方权限
   - 使用 IP 白名单或内部网络隔离

## 🚀 性能优化

### 1. 使用连接池

```yaml
feign:
  httpclient:
    enabled: true
    max-connections: 200        # 最大连接数
    max-connections-per-route: 50  # 每个路由的最大连接数
```

### 2. 启用响应压缩

```yaml
feign:
  compression:
    response:
      enabled: true
```

### 3. 合理设置超时时间

```yaml
feign:
  client:
    config:
      user-feign-client:  # 特定客户端配置
        connect-timeout: 3000
        read-timeout: 5000
```

## 🛠 故障排查

### 问题1: Feign 调用超时

```
解决方案：
1. 检查服务是否正常运行
2. 增加超时时间配置
3. 检查网络连接
4. 查看服务端日志
```

### 问题2: 降级处理未生效

```
解决方案：
1. 确保 `feign.circuitbreaker.enabled=true`
2. 检查 FallbackFactory 是否已注册为 Bean
3. 查看日志确认异常类型
```

### 问题3: 服务发现失败

```
解决方案：
1. 检查 Nacos 连接配置
2. 确认服务名称正确
3. 验证服务实例是否已注册
```

## 📈 监控与日志

### 启用 Feign 日志

```yaml
logging:
  level:
    com.basebackend.feign.client: DEBUG
```

### 日志级别说明

- `NONE`: 不记录日志（默认）
- `BASIC`: 仅记录请求方法、URL、响应状态码和执行时间
- `HEADERS`: 记录 BASIC 级别的日志，加上请求和响应头
- `FULL`: 记录请求和响应的所有信息

## 🔄 扩展指南

### 添加新的 Feign 客户端

1. **创建 DTO**
```java
// basebackend-feign-api/src/main/java/com/basebackend/feign/dto/xxx/XxxBasicDTO.java
```

2. **定义 Feign 接口**
```java
// basebackend-feign-api/src/main/java/com/basebackend/feign/client/XxxFeignClient.java
@FeignClient(name = "service-name", fallbackFactory = XxxFeignFallbackFactory.class)
public interface XxxFeignClient {
    // 定义接口方法
}
```

3. **实现 Fallback**
```java
// basebackend-feign-api/src/main/java/com/basebackend/feign/fallback/XxxFeignFallbackFactory.java
@Component
public class XxxFeignFallbackFactory implements FallbackFactory<XxxFeignClient> {
    // 实现降级逻辑
}
```

4. **在服务提供方实现接口**
```java
// 在对应的 Controller 中添加接口实现
```

## 📝 最佳实践

1. **DTO 设计**
   - 使用独立的 Feign DTO，不直接复用 Entity
   - DTO 应包含 Serializable 接口
   - 字段使用包装类型，避免 null 问题

2. **接口设计**
   - 接口方法要幂等
   - 返回值统一使用 `Result<T>` 包装
   - 参数尽量简单，复杂对象用 JSON 传输

3. **降级处理**
   - 返回默认值或空集合
   - 记录详细的错误日志
   - 对于关键业务考虑重试

4. **版本管理**
   - Feign API 版本与服务端保持一致
   - 接口变更要考虑向后兼容
   - 重大变更使用新版本号

---

**创建时间**: 2025-11-08
**创建者**: Claude Code (浮浮酱) ฅ'ω'ฅ
**版本**: v1.0.0
