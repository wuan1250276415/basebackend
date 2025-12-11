# 权限控制数据可见性使用指南

## 概述

权限控制数据可见性功能允许根据用户权限动态控制敏感数据的显示。当用户没有查看某个敏感字段的权限时，系统会自动对该字段进行脱敏处理。

## 核心组件

### 1. PermissionContext

权限上下文，用于存储当前用户的权限信息。

```java
// 设置用户权限
PermissionContext.setPermissions(Set.of("VIEW_SENSITIVE_DATA", "VIEW_PHONE"));

// 检查权限
if (PermissionContext.hasPermission("VIEW_PHONE")) {
    // 用户有权限查看手机号
}

// 清除权限上下文（请求结束时必须调用）
PermissionContext.clear();
```

### 2. @Sensitive 注解

标记敏感字段，支持权限控制。

```java
public class User {
    // 需要VIEW_PHONE权限才能查看未脱敏的手机号
    @Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_PHONE")
    private String phone;
    
    // 需要VIEW_ID_CARD权限才能查看未脱敏的身份证号
    @Sensitive(type = SensitiveType.ID_CARD, requiredPermission = "VIEW_ID_CARD")
    private String idCard;
    
    // 使用默认权限（VIEW_SENSITIVE_DATA）
    @Sensitive(type = SensitiveType.EMAIL)
    private String email;
}
```

### 3. PermissionMaskingInterceptor

MyBatis 拦截器，在查询结果返回前根据权限进行脱敏。

## 使用场景

### 场景1：基于角色的数据可见性

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public User getUserById(Long userId, Set<String> userPermissions) {
        // 设置当前用户的权限
        PermissionContext.setPermissions(userPermissions);
        
        try {
            // 查询用户信息
            User user = userMapper.selectById(userId);
            // 返回的user对象中，敏感字段会根据权限自动脱敏
            return user;
        } finally {
            // 清除权限上下文
            PermissionContext.clear();
        }
    }
}
```

### 场景2：不同权限级别的数据展示

```java
// 管理员：拥有所有权限，可以查看所有敏感数据
Set<String> adminPermissions = Set.of(PermissionContext.VIEW_SENSITIVE_DATA);
PermissionContext.setPermissions(adminPermissions);
User user1 = userMapper.selectById(1L);
// user1.phone = "13812345678" (未脱敏)
// user1.idCard = "110101199001011234" (未脱敏)

// 普通用户：只有查看手机号的权限
Set<String> normalPermissions = Set.of(PermissionContext.VIEW_PHONE);
PermissionContext.setPermissions(normalPermissions);
User user2 = userMapper.selectById(1L);
// user2.phone = "13812345678" (未脱敏)
// user2.idCard = "110101********1234" (已脱敏)

// 访客：没有任何权限
PermissionContext.setPermissions(new HashSet<>());
User user3 = userMapper.selectById(1L);
// user3.phone = "138****5678" (已脱敏)
// user3.idCard = "110101********1234" (已脱敏)
```

### 场景3：Web 应用中的自动权限设置

在 Web 应用中，建议创建一个过滤器或拦截器来自动设置权限上下文：

```java
@Component
@Order(1000)
public class PermissionContextFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            try {
                // 从JWT token、Session或请求头中提取用户权限
                Set<String> permissions = extractPermissions(httpRequest);
                PermissionContext.setPermissions(permissions);
                
                // 继续执行请求
                chain.doFilter(request, response);
            } finally {
                // 清除权限上下文，避免内存泄漏
                PermissionContext.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
    
    private Set<String> extractPermissions(HttpServletRequest request) {
        // 从JWT token、Session或其他安全来源获取权限
        // 示例：从请求头获取（生产环境应使用更安全的方式）
        String permissionsStr = request.getHeader("X-User-Permissions");
        if (StringUtils.hasText(permissionsStr)) {
            return Arrays.stream(permissionsStr.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        }
        return new HashSet<>();
    }
}

// 使用示例
@RestController
public class UserController {
    
    @Autowired
    private UserMapper userMapper;
    
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // 过滤器已经自动设置了权限
        // 直接查询即可，敏感字段会根据权限自动脱敏
        return userMapper.selectById(id);
    }
}
```

## 内置权限常量

```java
// 查看所有敏感数据的权限（超级权限）
PermissionContext.VIEW_SENSITIVE_DATA

// 查看手机号的权限
PermissionContext.VIEW_PHONE

// 查看身份证号的权限
PermissionContext.VIEW_ID_CARD

// 查看银行卡号的权限
PermissionContext.VIEW_BANK_CARD

// 查看邮箱的权限
PermissionContext.VIEW_EMAIL

// 查看地址的权限
PermissionContext.VIEW_ADDRESS
```

## 配置

在 `application.yml` 中启用脱敏功能：

```yaml
database:
  enhanced:
    security:
      # 脱敏配置
      masking:
        enabled: true  # 启用脱敏
        rules:
          phone: "***-****-####"
          id-card: "######********####"
          bank-card: "#### **** **** ####"
```

## 工作流程

1. **请求到达**：Web 过滤器从安全来源（JWT、Session等）提取用户权限
2. **设置上下文**：将权限信息存储到 `PermissionContext`
3. **执行查询**：MyBatis 执行数据库查询
4. **解密数据**：`DecryptionInterceptor` 解密敏感字段（如果启用了加密）
5. **权限检查**：`PermissionMaskingInterceptor` 检查用户权限
6. **脱敏处理**：对用户没有权限查看的字段进行脱敏
7. **返回结果**：返回处理后的数据
8. **清除上下文**：请求结束时，Web 过滤器清除权限上下文

## 最佳实践

### 1. 始终清除权限上下文

```java
try {
    PermissionContext.setPermissions(permissions);
    // 执行业务逻辑
} finally {
    PermissionContext.clear(); // 必须清除，避免内存泄漏
}
```

### 2. 使用细粒度权限

```java
// 推荐：使用细粒度权限
@Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_USER_PHONE")
@Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_CUSTOMER_PHONE")

// 不推荐：所有字段使用相同权限
@Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_SENSITIVE_DATA")
```

### 3. 在 Web 应用中使用过滤器

创建自定义过滤器自动处理权限上下文的设置和清除，避免手动管理。参考上面的场景3示例。

### 4. 权限信息来源

在生产环境中，权限信息应该从可信来源获取：
- JWT Token
- Session
- 认证服务器
- 不要直接从请求头获取（可能被篡改）

### 5. 结合加密使用

```java
// 同时启用加密和权限控制
@Sensitive(
    type = SensitiveType.PHONE,
    encrypt = true,  // 数据库中加密存储
    mask = true,     // 根据权限决定是否脱敏
    requiredPermission = "VIEW_PHONE"
)
private String phone;
```

## 注意事项

1. **性能影响**：权限检查和脱敏处理会增加一定的性能开销，建议在高并发场景下进行性能测试
2. **权限粒度**：权限粒度应该根据业务需求设计，过细会增加管理复杂度，过粗会降低安全性
3. **日志记录**：敏感操作应该记录审计日志，包括谁在什么时候查看了哪些敏感数据
4. **缓存问题**：如果使用了缓存，需要注意缓存的数据可能包含未脱敏的敏感信息
5. **测试覆盖**：确保对不同权限级别的数据访问进行充分测试

## 示例代码

完整的使用示例：

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    
    private String username;
    
    @Sensitive(type = SensitiveType.PHONE, requiredPermission = "VIEW_PHONE")
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD, requiredPermission = "VIEW_ID_CARD")
    private String idCard;
    
    @Sensitive(type = SensitiveType.EMAIL, requiredPermission = "VIEW_EMAIL")
    private String email;
    
    @Sensitive(type = SensitiveType.ADDRESS, requiredPermission = "VIEW_ADDRESS")
    private String address;
}

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 根据用户角色查询用户信息
     */
    public User getUserByRole(Long userId, String role) {
        // 根据角色设置权限
        Set<String> permissions = getPermissionsByRole(role);
        PermissionContext.setPermissions(permissions);
        
        try {
            return userMapper.selectById(userId);
        } finally {
            PermissionContext.clear();
        }
    }
    
    private Set<String> getPermissionsByRole(String role) {
        return switch (role) {
            case "ADMIN" -> Set.of(PermissionContext.VIEW_SENSITIVE_DATA);
            case "MANAGER" -> Set.of(
                PermissionContext.VIEW_PHONE,
                PermissionContext.VIEW_EMAIL
            );
            case "USER" -> Set.of(PermissionContext.VIEW_EMAIL);
            default -> new HashSet<>();
        };
    }
}
```

## 故障排查

### 问题1：敏感数据没有被脱敏

**可能原因**：
- 脱敏功能未启用
- 权限上下文中包含了查看权限
- 字段没有标记 `@Sensitive` 注解

**解决方法**：
1. 检查配置：`database.enhanced.security.masking.enabled=true`
2. 检查权限：`PermissionContext.getPermissions()`
3. 检查注解：确保字段有 `@Sensitive` 注解

### 问题2：所有用户都看不到敏感数据

**可能原因**：
- 权限上下文未设置
- 权限名称不匹配

**解决方法**：
1. 确保调用了 `PermissionContext.setPermissions()`
2. 检查权限名称是否与注解中的 `requiredPermission` 一致

### 问题3：内存泄漏

**可能原因**：
- 未调用 `PermissionContext.clear()`

**解决方法**：
- 使用 try-finally 确保清除
- 使用 `PermissionContextFilter` 自动管理

## 相关文档

- [数据加密使用指南](ENCRYPTION_USAGE.md)
- [数据脱敏使用指南](DATA_MASKING_USAGE.md)
- [多租户使用指南](MULTI_TENANCY_USAGE.md)
