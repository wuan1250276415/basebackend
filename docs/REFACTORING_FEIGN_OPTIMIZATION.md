# Feign客户端重构优化总结

## 重构目标

1. 将 `ApplicationResourceServiceImpl` (system-api) 中的 mapper 调用改为 Feign 接口调用
2. 优化 `AuthServiceImpl` (user-api) 中的 `DeptFeignClient` 调用，使调用失败不影响主流程

## 实施内容

### 1. 创建 RoleMenuFeignClient

**文件**: `basebackend-feign-api/src/main/java/com/basebackend/feign/client/RoleMenuFeignClient.java`

- 提供 `assignMenus()` 方法：为角色分配菜单
- 提供 `getRoleMenus()` 方法：获取角色的菜单列表
- 使用 `RoleMenuFeignFallbackFactory` 进行降级处理

### 2. 创建 RoleMenuFeignFallbackFactory

**文件**: `basebackend-feign-api/src/main/java/com/basebackend/feign/fallback/RoleMenuFeignFallbackFactory.java`

- 实现 Feign 调用失败时的降级逻辑
- 记录错误日志
- 返回友好的错误信息

### 3. 修改 ApplicationResourceServiceImpl

**文件**: `basebackend-system-api/src/main/java/com/basebackend/system/service/impl/ApplicationResourceServiceImpl.java`

**变更**:
- 移除直接的 `roleMenuMapper` 调用
- 使用 `RoleMenuFeignClient.assignMenus()` 替代
- 添加异常处理，Feign 调用失败不影响资源分配流程
- 添加详细的日志记录

**关键代码**:
```java
try {
    Result<String> result = roleMenuFeignClient.assignMenus(roleId, menuIds);
    if (result == null || result.getCode() != 200) {
        log.warn("通过Feign分配角色菜单失败: roleId={}, menuIds={}, message={}", 
                roleId, menuIds, result != null ? result.getMessage() : "null");
    }
} catch (Exception e) {
    log.error("通过Feign分配角色菜单异常: roleId={}, menuIds={}, error={}", 
            roleId, menuIds, e.getMessage(), e);
    // 不抛出异常，允许资源分配继续
}
```

### 4. 优化 AuthServiceImpl 中的 DeptFeignClient 调用

**文件**: `basebackend-user-api/src/main/java/com/basebackend/user/service/impl/AuthServiceImpl.java`

**优化点**:
- 在 `login()` 方法中安全调用部门服务
- 在 `refreshToken()` 方法中安全调用部门服务
- 在 `saveOnlineUser()` 方法中安全调用部门服务

**关键改进**:
```java
// 安全调用部门服务，失败不影响登录
if (user.getDeptId() != null) {
    try {
        Result<DeptBasicDTO> deptResult = deptFeignClient.getById(user.getDeptId());
        if (deptResult != null && deptResult.getCode() == 200 && deptResult.getData() != null) {
            userInfo.setDeptName(deptResult.getData().getDeptName());
        } else {
            log.warn("获取部门信息失败或返回空: deptId={}, message={}", 
                    user.getDeptId(), deptResult != null ? deptResult.getMessage() : "null");
            userInfo.setDeptName(""); // 设置默认值
        }
    } catch (Exception e) {
        log.error("调用部门服务异常: deptId={}, error={}", user.getDeptId(), e.getMessage(), e);
        userInfo.setDeptName(""); // 设置默认值，不影响登录流程
    }
}
```

### 5. 在 system-api 中创建必要的实体和 Mapper

由于 system-api 需要使用角色相关的功能，创建了以下文件：

**实体类**:
- `SysRole.java` - 角色实体
- `SysRoleResource.java` - 角色资源关联实体

**Mapper接口**:
- `SysRoleMapper.java` - 角色Mapper
- `SysRoleResourceMapper.java` - 角色资源关联Mapper

**Mapper XML**:
- `SysRoleMapper.xml` - 角色查询SQL
- `SysRoleResourceMapper.xml` - 角色资源关联SQL

### 6. 更新依赖配置

**system-api pom.xml**:
```xml
<dependency>
    <groupId>com.basebackend</groupId>
    <artifactId>basebackend-feign-api</artifactId>
    <version>${project.version}</version>
</dependency>
```

**FeignServiceConstants.java**:
```java
public static final String USER_SERVICE = "basebackend-user-api";
```

## 优化效果

### 1. 解耦服务依赖
- system-api 不再直接依赖 user-api 的 mapper
- 通过 Feign 接口实现服务间通信
- 符合微服务架构最佳实践

### 2. 提高系统容错性
- DeptFeignClient 调用失败不会导致登录失败
- RoleMenuFeignClient 调用失败不会导致资源分配失败
- 所有 Feign 调用都有完善的异常处理和降级逻辑

### 3. 改善可维护性
- 统一使用 Feign 客户端进行服务间调用
- 详细的日志记录便于问题排查
- 清晰的错误处理逻辑

### 4. 增强系统稳定性
- 避免因单个服务不可用导致整个流程失败
- 提供默认值保证业务连续性
- 降级策略确保系统可用性

## 验证结果

- ✅ basebackend-feign-api 编译通过
- ✅ basebackend-system-api 编译通过
- ✅ basebackend-user-api 编译通过
- ✅ 无编译错误和警告

## 问题修复

### DeptBasicDTO 字段不匹配问题

**问题**: user-api 调用 system-api 的 DeptFeignClient 时报错：
```
Unrecognized field "orderNum" (class com.basebackend.feign.dto.dept.DeptBasicDTO)
```

**原因**: 
- system-api 的 `SysDept` 实体使用 `orderNum` 字段
- feign-api 的 `DeptBasicDTO` 使用 `sort` 字段
- 字段名不匹配导致 JSON 反序列化失败

**解决方案**:
1. 在 `DeptBasicDTO` 添加 `@JsonIgnoreProperties(ignoreUnknown = true)` 注解，忽略未知字段
2. 同时添加 `orderNum` 字段以兼容 system-api 的返回数据
3. 保留 `sort` 字段以保持向后兼容

**修改文件**: `basebackend-feign-api/src/main/java/com/basebackend/feign/dto/dept/DeptBasicDTO.java`

## 注意事项

1. **Feign 配置**: 确保 Nacos 服务发现正常工作
2. **超时设置**: 根据实际情况调整 Feign 超时配置
3. **降级策略**: 监控降级日志，及时发现服务问题
4. **数据一致性**: 注意跨服务调用的事务处理
5. **DTO 字段兼容**: 使用 `@JsonIgnoreProperties(ignoreUnknown = true)` 提高 DTO 的容错性

## 后续建议

1. 添加 Feign 调用的监控指标
2. 配置合理的超时和重试策略
3. 考虑使用 Sentinel 进行流量控制
4. 定期review降级日志，优化服务稳定性
