package com.basebackend.security.rbac.example;

import com.basebackend.security.rbac.annotation.*;
import com.basebackend.security.rbac.PermissionContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理控制器示例
 * 演示如何使用权限注解进行权限控制
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    /**
     * 获取用户列表
     * 需要权限: user:read
     * 数据范围: 根据用户角色自动确定
     */
    @GetMapping
    @RequirePermission(value = "user:read", type = RequirePermission.PermissionType.API)
    public ResponseEntity<List<UserDto>> getUsers(@PermissionContextParam UserQueryParam param) {
        // TODO: 实现获取用户列表逻辑
        return ResponseEntity.ok(List.of());
    }

    /**
     * 获取用户详情
     * 需要权限: user:read
     * 数据范围: 用户只能查看自己或本部门的数据
     */
    @GetMapping("/{id}")
    @RequirePermission(value = "user:read", type = RequirePermission.PermissionType.API)
    @DataScope(type = DataScope.DataScopeType.DEPT)
    public ResponseEntity<UserDto> getUser(@PathVariable Long id,
                                         @PermissionContextParam UserQueryParam param) {
        // TODO: 实现获取用户详情逻辑
        return ResponseEntity.ok(new UserDto());
    }

    /**
     * 创建用户
     * 需要权限: user:write
     * 角色要求: ADMIN 或 USER_MANAGER
     */
    @PostMapping
    @RequirePermission(value = "user:write", type = RequirePermission.PermissionType.API)
    @RequireRole(value = "ADMIN,USER_MANAGER")
    public ResponseEntity<UserDto> createUser(@RequestBody CreateUserRequest request) {
        // TODO: 实现创建用户逻辑
        return ResponseEntity.ok(new UserDto());
    }

    /**
     * 更新用户
     * 需要权限: user:write
     * 数据范围: 用户只能修改自己或本部门的数据
     * Owner验证: 只有Owner或具有ADMIN角色才能修改
     */
    @PutMapping("/{id}")
    @RequirePermission(value = "user:write", type = RequirePermission.PermissionType.API)
    @DataScope(type = DataScope.DataScopeType.DEPT)
    @RequireOwner(allowedRoles = {"ADMIN"})
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id,
                                            @RequestBody UpdateUserRequest request) {
        // TODO: 实现更新用户逻辑
        return ResponseEntity.ok(new UserDto());
    }

    /**
     * 删除用户
     * 需要权限: user:delete
     * 角色要求: 只有ADMIN角色可以删除用户
     */
    @DeleteMapping("/{id}")
    @RequirePermission(value = "user:delete", type = RequirePermission.PermissionType.API)
    @RequireRole(value = "ADMIN")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // TODO: 实现删除用户逻辑
        return ResponseEntity.noContent().build();
    }

    /**
     * 重置用户密码
     * 需要权限: user:reset-password
     * 角色要求: ADMIN 或 USER_MANAGER
     */
    @PostMapping("/{id}/reset-password")
    @RequirePermission(value = "user:reset-password", type = RequirePermission.PermissionType.API)
    @RequireRole(value = "ADMIN,USER_MANAGER")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id) {
        // TODO: 实现重置密码逻辑
        return ResponseEntity.ok().build();
    }

    /**
     * 分配用户角色
     * 需要权限: user:assign-role
     * 角色要求: ADMIN
     */
    @PostMapping("/{id}/roles")
    @RequirePermission(value = "user:assign-role", type = RequirePermission.PermissionType.API)
    @RequireRole(value = "ADMIN")
    public ResponseEntity<Void> assignRole(@PathVariable Long id,
                                         @RequestBody AssignRoleRequest request) {
        // TODO: 实现分配角色逻辑
        return ResponseEntity.ok().build();
    }

    /**
     * 获取用户权限
     * 用户只能查看自己的权限，管理员可以查看任意用户权限
     */
    @GetMapping("/{id}/permissions")
    @RequirePermission(value = "user:read-permission", type = RequirePermission.PermissionType.API)
    public ResponseEntity<UserPermissions> getUserPermissions(@PathVariable Long id,
                                                             @PermissionContextParam UserQueryParam param) {
        // TODO: 实现获取用户权限逻辑
        return ResponseEntity.ok(new UserPermissions());
    }

    /**
     * 批量操作用户
     * 需要权限: user:batch-write
     * 逻辑要求: 必须拥有所有指定的权限
     */
    @PostMapping("/batch")
    @RequirePermission(value = "user:write,user:batch", type = RequirePermission.PermissionType.API, logic = RequirePermission.Logic.ALL)
    @RequireRole(value = "ADMIN")
    public ResponseEntity<Void> batchOperation(@RequestBody BatchUserRequest request) {
        // TODO: 实现批量操作逻辑
        return ResponseEntity.ok().build();
    }

    /**
     * 导出用户数据
     * 需要权限: user:export
     * 数据范围: 根据用户角色确定可导出的数据范围
     */
    @GetMapping("/export")
    @RequirePermission(value = "user:export", type = RequirePermission.PermissionType.API)
    @DataScope(type = DataScope.DataScopeType.DEPT)
    public ResponseEntity<String> exportUsers(@PermissionContextParam UserQueryParam param) {
        // TODO: 实现导出逻辑
        return ResponseEntity.ok("导出完成");
    }

    /**
     * 获取当前用户信息
     * 无需权限检查，登录用户即可访问
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@CurrentUser Long userId) {
        // TODO: 实现获取当前用户信息逻辑
        return ResponseEntity.ok(new UserDto());
    }

    /**
     * 更新当前用户信息
     * 需要权限: user:update-self
     * Owner验证: 只能更新自己的信息
     */
    @PutMapping("/me")
    @RequirePermission(value = "user:update-self", type = RequirePermission.PermissionType.API)
    @RequireOwner
    public ResponseEntity<UserDto> updateCurrentUser(@RequestBody UpdateCurrentUserRequest request,
                                                   @CurrentUser Long userId) {
        // TODO: 实现更新当前用户信息逻辑
        return ResponseEntity.ok(new UserDto());
    }
}

/**
 * 用户查询参数
 */
@Data
class UserQueryParam {
    private Long userId;
    private String username;
    private String email;
    private Long deptId;
    private Long resourceOwnerId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}

/**
 * 创建用户请求
 */
@Data
class CreateUserRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private Long deptId;
    private List<String> roles;
}

/**
 * 更新用户请求
 */
@Data
class UpdateUserRequest {
    private String email;
    private String phone;
    private Long deptId;
    private Integer status;
    private List<String> roles;
}

/**
 * 分配角色请求
 */
@Data
class AssignRoleRequest {
    private List<String> roles;
    private String remark;
}

/**
 * 批量用户操作请求
 */
@Data
class BatchUserRequest {
    private List<Long> userIds;
    private String operation;
    private Object params;
}

/**
 * 更新当前用户请求
 */
@Data
class UpdateCurrentUserRequest {
    private String email;
    private String phone;
    private String avatar;
}

/**
 * 用户数据传输对象
 */
@Data
class UserDto {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private Long deptId;
    private String deptName;
    private Integer status;
    private String avatar;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}

/**
 * 用户权限信息
 */
@Data
class UserPermissions {
    private Long userId;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
    private PermissionContext.DataScope dataScope;
}
