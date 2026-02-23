[< 返回首页](Home) | [< 上一页: JWT认证体系](JWT认证体系)

---

# 权限控制 RBAC

---

## RBAC 模型

BaseBackend 采用标准的 RBAC（Role-Based Access Control）权限模型，支持用户-角色-权限三级关联。

```
┌────────┐     N:N      ┌────────┐     N:N      ┌────────┐
│  用户   │ ──────────> │  角色   │ ──────────> │  权限   │
│ User   │             │  Role  │             │ Perm   │
└────────┘             └────────┘             └────────┘
     │                      │                      │
     │                      │                      │
  sys_user            sys_user_role           sys_role_menu
                      sys_role                sys_menu
```

### 数据表关系

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表 |
| `sys_role` | 角色表 |
| `sys_menu` | 菜单/权限表（包含菜单和按钮权限） |
| `sys_user_role` | 用户-角色关联表 |
| `sys_role_menu` | 角色-菜单/权限关联表 |
| `sys_dept` | 部门表 |

### 权限标识规范

权限标识采用 `模块:操作` 格式：

```
user:list        # 用户列表查看
user:add         # 用户新增
user:edit        # 用户编辑
user:delete      # 用户删除
user:query       # 用户详情查询
role:list        # 角色列表
role:add         # 角色新增
system:log:list  # 操作日志查看
system:monitor   # 系统监控
```

---

## `@RequiresPermission` 权限注解

### 基本使用

```java
/**
 * 需要 user:list 权限才能访问
 */
@RequiresPermission("user:list")
@GetMapping("/users")
public Result<PageResult<UserVO>> listUsers(PageQuery pageQuery) {
    return Result.success(userService.listUsers(pageQuery));
}

/**
 * 需要 user:add 权限
 */
@RequiresPermission("user:add")
@PostMapping("/users")
public Result<Void> createUser(@RequestBody UserCreateDTO dto) {
    userService.create(dto);
    return Result.success();
}
```

### 多权限组合

```java
/**
 * 需要同时拥有 user:edit 和 user:assign 权限（AND 逻辑）
 */
@RequiresPermission(value = {"user:edit", "user:assign"}, logical = Logical.AND)
@PutMapping("/users/{id}/role")
public Result<Void> assignRole(@PathVariable Long id, @RequestBody RoleAssignDTO dto) {
    userService.assignRole(id, dto);
    return Result.success();
}

/**
 * 拥有 user:delete 或 admin 权限之一即可（OR 逻辑）
 */
@RequiresPermission(value = {"user:delete", "admin"}, logical = Logical.OR)
@DeleteMapping("/users/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    userService.delete(id);
    return Result.success();
}
```

---

## `@RequiresRole` 角色注解

```java
/**
 * 需要 admin 角色
 */
@RequiresRole("admin")
@GetMapping("/system/config")
public Result<SystemConfig> getConfig() {
    return Result.success(configService.get());
}

/**
 * 需要 admin 或 manager 角色之一
 */
@RequiresRole(value = {"admin", "manager"}, logical = Logical.OR)
@PostMapping("/system/config")
public Result<Void> updateConfig(@RequestBody SystemConfigDTO dto) {
    configService.update(dto);
    return Result.success();
}
```

---

## 动态权限加载

权限不是硬编码的，而是从数据库动态加载：

```
启动时 / 权限变更时
       │
       ▼
从 sys_menu 表加载所有权限
       │
       ▼
按角色分组缓存到 Redis
       │
       ▼
请求到达时
       │
       ▼
从用户上下文获取角色列表
       │
       ▼
从缓存获取角色对应的权限集
       │
       ▼
检查目标权限是否在用户权限集中
```

### 权限缓存刷新

```java
// 当角色权限发生变更时，刷新缓存
@EventListener
public void onPermissionChanged(PermissionChangedEvent event) {
    permissionCacheService.refreshCache(event.getRoleId());
}
```

---

## 数据权限 `@DataScope`

> 详见 [Common 公共模块详解](Common公共模块详解) 中 common-datascope 部分

数据权限是对 RBAC 的扩展，不仅控制**能不能访问**，还控制**能看到哪些数据**。

### 使用示例

```java
/**
 * 查询用户列表 - 应用数据权限
 * 管理员看全部，部门经理看本部门，普通用户看自己
 */
@DataScope(deptAlias = "u", userAlias = "u")
public List<UserVO> selectUserList(UserQuery query) {
    return userMapper.selectUserList(query);
}
```

### 工作原理

```
1. AOP 拦截标注了 @DataScope 的方法
2. 获取当前用户的数据权限范围
3. 根据用户角色的 data_scope 字段确定范围类型
4. MyBatis 拦截器在 SQL 中追加 WHERE 条件

示例 SQL 变化：
原始：SELECT * FROM sys_user u
追加：SELECT * FROM sys_user u WHERE u.dept_id IN (1, 2, 3)
```

### 角色数据权限配置

在角色管理中可以为每个角色配置数据权限范围：

| data_scope 值 | 说明 |
|--------------|------|
| 1 | 全部数据权限 |
| 2 | 本部门数据权限 |
| 3 | 本部门及以下数据权限 |
| 4 | 仅本人数据权限 |
| 5 | 自定义数据权限（手动选择部门） |

---

## 权限校验流程

```
请求到达微服务
      │
      ▼
JwtAuthenticationFilter
├── 从请求头提取 Token
├── 解析用户信息
├── 设置 SecurityContext
├── 设置 UserContextHolder
      │
      ▼
@RequiresPermission / @RequiresRole AOP 拦截
├── 获取目标方法需要的权限
├── 获取当前用户拥有的权限
├── 判断是否满足（AND/OR 逻辑）
├── 不满足则抛出 PermissionDeniedException
      │
      ▼
@DataScope AOP 拦截（如有）
├── 确定数据权限范围
├── 注入 SQL 过滤条件
      │
      ▼
业务逻辑执行
```

---

| [< 上一页: JWT认证体系](JWT认证体系) | [下一页: 安全配置参考 >](安全配置参考) |
|---|---|
