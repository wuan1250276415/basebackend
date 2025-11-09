# Spring Boot 用户上下文管理使用指南

## 📖 概述

浮浮酱为后端创建了一套完整的用户上下文管理系统，类似于前端的 `useUser()` Hook，让您能够在任何地方快速获取当前登录用户的信息、权限和角色 ฅ'ω'ฅ

## 🎯 核心功能

### 1. **UserContext** - 用户上下文类
- 完整的用户信息（ID、用户名、昵称、部门等）
- 角色和权限列表
- 内置权限检查方法
- 请求IP和时间戳

### 2. **UserContextHolder** - 上下文持有者
- 基于 ThreadLocal 的线程安全存储
- 静态方法快速访问用户信息
- 便捷的权限和角色检查
- 自动的生命周期管理

### 3. **UserContextInterceptor** - 自动加载拦截器
- 请求开始时自动加载用户信息
- 请求结束时自动清理上下文
- 防止内存泄漏

---

## 🚀 快速开始

### 安装（已完成）

所有组件已自动配置，无需额外设置！拦截器已在 `WebMvcConfig` 中注册。

### 基础使用

#### 示例 1: 获取当前用户信息

```java
import com.basebackend.admin.context.UserContextHolder;

@Service
public class SomeService {

    public void doSomething() {
        // 获取当前用户ID
        Long userId = UserContextHolder.getUserId();

        // 获取当前用户名
        String username = UserContextHolder.getUsername();

        // 获取当前用户昵称
        String nickname = UserContextHolder.getNickname();

        // 获取当前用户部门ID
        Long deptId = UserContextHolder.getDeptId();

        // 获取当前用户部门名称
        String deptName = UserContextHolder.getDeptName();

        // 获取完整的用户上下文
        UserContext context = UserContextHolder.getContext();

        System.out.println("当前用户: " + username + " (" + nickname + ")");
        System.out.println("所属部门: " + deptName);
    }
}
```

#### 示例 2: 权限检查

```java
import com.basebackend.admin.context.UserContextHolder;

@Service
public class UserService {

    public void addUser(UserDTO userDTO) {
        // 检查是否有添加用户的权限
        if (!UserContextHolder.hasPermission("system:user:add")) {
            throw new RuntimeException("没有权限添加用户");
        }

        // 执行添加用户逻辑
        // ...
    }

    public void deleteUser(Long id) {
        // 检查是否有任意一个权限
        if (!UserContextHolder.hasAnyPermission(
                "system:user:delete",
                "system:user:*")) {
            throw new RuntimeException("没有权限删除用户");
        }

        // 执行删除用户逻辑
        // ...
    }

    public void updateUser(UserDTO userDTO) {
        // 要求必须有权限，否则抛出异常
        UserContextHolder.requirePermission("system:user:edit");

        // 执行更新用户逻辑
        // ...
    }
}
```

#### 示例 3: 角色检查

```java
import com.basebackend.admin.context.UserContextHolder;

@Service
public class SystemService {

    public void performAdminTask() {
        // 检查是否是管理员
        if (!UserContextHolder.isAdmin()) {
            throw new RuntimeException("只有管理员可以执行此操作");
        }

        // 执行管理员操作
        // ...
    }

    public void checkRole() {
        // 检查是否有指定角色
        if (UserContextHolder.hasRole("admin")) {
            System.out.println("当前用户是管理员");
        }

        // 检查是否有任意一个角色
        if (UserContextHolder.hasAnyRole("admin", "manager")) {
            System.out.println("当前用户是管理员或经理");
        }

        // 要求必须有角色，否则抛出异常
        UserContextHolder.requireRole("admin");
    }
}
```

---

## 💡 实际应用示例

### 示例 1: 自动填充创建人和更新人

```java
import com.basebackend.admin.context.UserContextHolder;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements DeptService {

    private final SysDeptMapper deptMapper;

    @Override
    @Transactional
    public void create(DeptDTO deptDTO) {
        SysDept dept = new SysDept();
        BeanUtil.copyProperties(deptDTO, dept);

        // 自动填充创建人
        Long currentUserId = UserContextHolder.getUserId();
        dept.setCreateBy(currentUserId);
        dept.setCreateTime(LocalDateTime.now());

        deptMapper.insert(dept);

        log.info("用户 {} 创建了部门: {}",
                UserContextHolder.getUsername(),
                dept.getDeptName());
    }

    @Override
    @Transactional
    public void update(DeptDTO deptDTO) {
        SysDept dept = deptMapper.selectById(deptDTO.getId());
        if (dept == null) {
            throw new RuntimeException("部门不存在");
        }

        // 检查权限
        if (!UserContextHolder.hasPermission("system:dept:edit")) {
            throw new RuntimeException("没有权限编辑部门");
        }

        BeanUtil.copyProperties(deptDTO, dept);

        // 自动填充更新人
        Long currentUserId = UserContextHolder.getUserId();
        dept.setUpdateBy(currentUserId);
        dept.setUpdateTime(LocalDateTime.now());

        deptMapper.updateById(dept);

        log.info("用户 {} 更新了部门: {}",
                UserContextHolder.getUsername(),
                dept.getDeptName());
    }
}
```

### 示例 2: 数据权限过滤（只能查看本部门数据）

```java
import com.basebackend.admin.context.UserContextHolder;

@Service
public class DataPermissionService {

    public List<UserDTO> getUserList(UserQueryDTO queryDTO) {
        // 如果不是管理员，只能查看本部门的数据
        if (!UserContextHolder.isAdmin()) {
            Long currentDeptId = UserContextHolder.getDeptId();
            queryDTO.setDeptId(currentDeptId);

            log.info("非管理员用户 {}，限制查看本部门 {} 的数据",
                    UserContextHolder.getUsername(),
                    UserContextHolder.getDeptName());
        }

        // 查询用户列表
        return userService.page(queryDTO);
    }
}
```

### 示例 3: 操作日志记录

```java
import com.basebackend.admin.context.UserContextHolder;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final SysOperationLogMapper logMapper;

    public void recordOperation(String operation, String params, String result) {
        SysOperationLog log = new SysOperationLog();

        // 自动填充用户信息
        log.setUserId(UserContextHolder.getUserId());
        log.setUsername(UserContextHolder.getUsername());
        log.setOperation(operation);
        log.setParams(params);
        log.setResult(result);
        log.setIpAddress(UserContextHolder.getIpAddress());
        log.setOperationTime(LocalDateTime.now());

        logMapper.insert(log);
    }
}

// 在业务代码中使用
@Service
public class UserService {

    @Autowired
    private OperationLogService operationLogService;

    public void deleteUser(Long userId) {
        // 业务逻辑
        userMapper.deleteById(userId);

        // 记录操作日志（自动包含当前用户信息）
        operationLogService.recordOperation(
                "删除用户",
                "userId=" + userId,
                "成功"
        );
    }
}
```

### 示例 4: Controller 中使用

```java
import com.basebackend.admin.context.UserContextHolder;

@RestController
@RequestMapping("/api/admin/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping
    public Result<UserDTO> getCurrentUser() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }

        UserDTO user = userService.getById(userId);
        return Result.success(user);
    }

    /**
     * 更新当前用户信息
     */
    @PutMapping
    public Result<String> updateProfile(@RequestBody UpdateProfileDTO dto) {
        // 只能更新自己的信息
        Long currentUserId = UserContextHolder.getUserId();
        dto.setUserId(currentUserId);

        userService.updateProfile(dto);

        log.info("用户 {} 更新了个人资料",
                UserContextHolder.getUsername());

        return Result.success("更新成功");
    }
}
```

### 示例 5: AOP 切面中使用

```java
import com.basebackend.admin.context.UserContextHolder;

@Aspect
@Component
@Slf4j
public class LogAspect {

    /**
     * 环绕通知：记录方法执行时间和操作人
     */
    @Around("@annotation(com.basebackend.admin.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取当前用户信息
        String username = UserContextHolder.getUsername();
        String ipAddress = UserContextHolder.getIpAddress();

        log.info("用户 {} (IP: {}) 开始执行: {}",
                username,
                ipAddress,
                point.getSignature());

        Object result = point.proceed();

        long endTime = System.currentTimeMillis();
        log.info("用户 {} 执行完成，耗时: {}ms",
                username,
                (endTime - startTime));

        return result;
    }
}
```

### 示例 6: 复杂权限判断

```java
import com.basebackend.admin.context.UserContext;
import com.basebackend.admin.context.UserContextHolder;

@Service
public class ComplexPermissionService {

    public boolean canEditUser(Long targetUserId) {
        UserContext currentUser = UserContextHolder.getContext();

        // 管理员可以编辑所有用户
        if (currentUser.isAdmin()) {
            return true;
        }

        // 有编辑权限
        if (!currentUser.hasPermission("system:user:edit")) {
            return false;
        }

        // 只能编辑本部门的用户
        SysUser targetUser = userMapper.selectById(targetUserId);
        if (targetUser == null) {
            return false;
        }

        Long currentDeptId = currentUser.getDeptId();
        Long targetDeptId = targetUser.getDeptId();

        return currentDeptId.equals(targetDeptId);
    }

    public void editUser(Long userId, UserDTO userDTO) {
        if (!canEditUser(userId)) {
            throw new RuntimeException("没有权限编辑此用户");
        }

        // 执行编辑逻辑
        // ...
    }
}
```

---

## 📚 API 参考

### UserContextHolder 静态方法

#### 基础信息获取

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `getContext()` | `UserContext` | 获取完整用户上下文 |
| `getContextOptional()` | `Optional<UserContext>` | 获取 Optional 包装的上下文 |
| `getUserId()` | `Long` | 获取当前用户ID |
| `getUsername()` | `String` | 获取当前用户名 |
| `getNickname()` | `String` | 获取当前用户昵称 |
| `getDeptId()` | `Long` | 获取当前用户部门ID |
| `getDeptName()` | `String` | 获取当前用户部门名称 |
| `getIpAddress()` | `String` | 获取当前请求IP地址 |

#### 权限检查

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `hasPermission(permission)` | `boolean` | 检查是否有指定权限 |
| `hasAnyPermission(permissions...)` | `boolean` | 检查是否有任意一个权限 |
| `hasAllPermissions(permissions...)` | `boolean` | 检查是否有所有权限 |
| `requirePermission(permission)` | `void` | 要求有权限，否则抛异常 |

#### 角色检查

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `hasRole(role)` | `boolean` | 检查是否有指定角色 |
| `hasAnyRole(roles...)` | `boolean` | 检查是否有任意一个角色 |
| `requireRole(role)` | `void` | 要求有角色，否则抛异常 |
| `isAdmin()` | `boolean` | 检查是否是管理员 |
| `isSystemUser()` | `boolean` | 检查是否是系统用户 |
| `isEnabled()` | `boolean` | 检查用户是否启用 |

#### 其他方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `isAuthenticated()` | `boolean` | 检查是否已登录 |
| `requireAuthenticated()` | `UserContext` | 要求已登录，否则抛异常 |
| `setContext(context)` | `void` | 设置用户上下文 |
| `clear()` | `void` | 清空用户上下文 |

### UserContext 实例方法

| 方法 | 返回值 | 说明 |
|------|--------|------|
| `hasPermission(permission)` | `boolean` | 检查是否有指定权限 |
| `hasAnyPermission(permissions...)` | `boolean` | 检查是否有任意一个权限 |
| `hasAllPermissions(permissions...)` | `boolean` | 检查是否有所有权限 |
| `hasRole(role)` | `boolean` | 检查是否有指定角色 |
| `hasAnyRole(roles...)` | `boolean` | 检查是否有任意一个角色 |
| `isAdmin()` | `boolean` | 检查是否是管理员 |
| `isSystemUser()` | `boolean` | 检查是否是系统用户 |
| `isEnabled()` | `boolean` | 检查用户是否启用 |

---

## ⚡ 最佳实践

### 1. 在 Service 层使用

```java
// ✅ 推荐：在 Service 层使用
@Service
public class UserService {
    public void deleteUser(Long userId) {
        // 权限检查
        if (!UserContextHolder.hasPermission("system:user:delete")) {
            throw new RuntimeException("没有权限");
        }

        // 业务逻辑
        userMapper.deleteById(userId);

        // 记录日志
        log.info("用户 {} 删除了用户 {}",
                UserContextHolder.getUsername(),
                userId);
    }
}

// ❌ 不推荐：在 Controller 层做复杂的业务逻辑
@RestController
public class UserController {
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        // 不要在这里做复杂的业务逻辑
    }
}
```

### 2. 结合 Optional 使用

```java
// ✅ 推荐：使用 Optional 处理可能为 null 的情况
UserContextHolder.getContextOptional()
    .map(UserContext::getUserId)
    .ifPresent(userId -> {
        // 处理逻辑
    });

// ✅ 推荐：提供默认值
String username = UserContextHolder.getContextOptional()
    .map(UserContext::getUsername)
    .orElse("未知用户");
```

### 3. 权限注解（自定义）

```java
/**
 * 自定义权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
}

/**
 * 权限检查切面
 */
@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        String permission = requirePermission.value();
        if (!UserContextHolder.hasPermission(permission)) {
            throw new RuntimeException("没有权限: " + permission);
        }
    }
}

// 使用
@Service
public class UserService {

    @RequirePermission("system:user:delete")
    public void deleteUser(Long userId) {
        // 自动进行权限检查
        userMapper.deleteById(userId);
    }
}
```

---

## 🐛 常见问题

### Q1: UserContext 为 null

**原因：**
- 用户未登录
- 拦截器未生效
- 在拦截器之前的代码中调用

**解决：**
```java
// 方式 1: 使用 Optional
UserContextHolder.getContextOptional()
    .ifPresent(context -> {
        // 处理逻辑
    });

// 方式 2: 检查是否登录
if (UserContextHolder.isAuthenticated()) {
    Long userId = UserContextHolder.getUserId();
    // 处理逻辑
}

// 方式 3: 要求必须登录
UserContext context = UserContextHolder.requireAuthenticated();
```

### Q2: 内存泄漏

**原因：** ThreadLocal 未清理

**解决：** 拦截器已自动处理，无需手动清理。如果在异步任务中使用，需要手动清理：

```java
try {
    // 设置用户上下文
    UserContextHolder.setContext(userContext);

    // 业务逻辑
    doSomething();
} finally {
    // 清理上下文
    UserContextHolder.clear();
}
```

### Q3: 异步任务中获取不到用户信息

**原因：** ThreadLocal 是线程隔离的，异步任务在新线程中执行

**解决：** 在异步任务中传递用户上下文

```java
// 保存当前上下文
UserContext context = UserContextHolder.getContext();

// 提交异步任务
CompletableFuture.runAsync(() -> {
    try {
        // 设置上下文到新线程
        UserContextHolder.setContext(context);

        // 执行异步逻辑
        doAsyncWork();
    } finally {
        // 清理上下文
        UserContextHolder.clear();
    }
});
```

---

## 📦 文件结构

```
basebackend-admin-api/src/main/java/com/basebackend/admin/
├── context/
│   ├── UserContext.java              # 用户上下文类
│   └── UserContextHolder.java        # 上下文持有者工具类
├── interceptor/
│   └── UserContextInterceptor.java   # 用户上下文拦截器
└── config/
    └── WebMvcConfig.java             # 拦截器配置
```

---

## 🎉 总结

浮浮酱创建的用户上下文管理系统提供了：

✅ **便捷访问** - 任何地方都能快速获取用户信息
✅ **线程安全** - 基于 ThreadLocal，多线程环境下安全
✅ **自动管理** - 拦截器自动加载和清理，防止内存泄漏
✅ **丰富功能** - 内置权限检查、角色检查等便捷方法
✅ **类型安全** - 完整的类型定义和 Null 安全处理
✅ **性能优化** - 一次加载，整个请求周期内复用

现在您可以在后端任何地方轻松获取当前用户信息了喵～ ฅ'ω'ฅ

---

**创建者：** Claude Code (浮浮酱) φ(≧ω≦*)♪
**创建时间：** 2025-11-09
**版本：** v1.0.0
