package com.basebackend.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务Feign客户端API
 *
 * @author 浮浮酱
 * @since 2025-11-15
 */
@FeignClient(name = "basebackend-user-service", path = "/api/users")
@Tag(name = "用户服务API", description = "用户管理相关接口")
public interface UserServiceApi {

    @Operation(summary = "根据ID查询用户", description = "根据ID查询用户详情")
    @GetMapping("/{id}")
    UserDTO getById(@Parameter(description = "用户ID") @PathVariable Long id);

    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户详情")
    @GetMapping("/by-username/{username}")
    UserDTO getByUsername(@Parameter(description = "用户名") @PathVariable String username);

    @Operation(summary = "根据手机号查询用户", description = "根据手机号查询用户详情")
    @GetMapping("/by-phone/{phone}")
    UserDTO getByPhone(@Parameter(description = "手机号") @PathVariable String phone);

    @Operation(summary = "根据邮箱查询用户", description = "根据邮箱查询用户详情")
    @GetMapping("/by-email/{email}")
    UserDTO getByEmail(@Parameter(description = "邮箱") @PathVariable String email);

    @Operation(summary = "批量查询用户", description = "根据用户ID列表批量查询用户信息")
    @GetMapping("/batch")
    List<UserDTO> getBatchByIds(@Parameter(description = "用户ID列表") @RequestParam List<Long> ids);

    @Operation(summary = "获取用户角色ID列表", description = "获取用户角色ID列表")
    @GetMapping("/{id}/roles")
    List<Long> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId);

    @Operation(summary = "检查用户名是否唯一", description = "检查用户名是否唯一")
    @GetMapping("/check-username")
    boolean checkUsernameUnique(
            @Parameter(description = "用户名") @RequestParam String username,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId);

    @Operation(summary = "检查邮箱是否唯一", description = "检查邮箱是否唯一")
    @GetMapping("/check-email")
    boolean checkEmailUnique(
            @Parameter(description = "邮箱") @RequestParam String email,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId);

    @Operation(summary = "检查手机号是否唯一", description = "检查手机号是否唯一")
    @GetMapping("/check-phone")
    boolean checkPhoneUnique(
            @Parameter(description = "手机号") @RequestParam String phone,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId);

    @Operation(summary = "更新用户基本信息", description = "更新用户基本信息")
    @PutMapping("/{id}")
    void updateUser(@Parameter(description = "用户ID") @PathVariable Long id,
                    @Parameter(description = "用户信息") @RequestBody UserDTO userDTO);

    @Operation(summary = "修改用户密码", description = "修改用户登录密码")
    @PutMapping("/{id}/password")
    void changePassword(@Parameter(description = "用户ID") @PathVariable Long id,
                        @Parameter(description = "旧密码") @RequestParam String oldPassword,
                        @Parameter(description = "新密码") @RequestParam String newPassword);

    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    @PutMapping("/{id}/reset-password")
    void resetPassword(@Parameter(description = "用户ID") @PathVariable Long id,
                       @Parameter(description = "新密码") @RequestParam String newPassword);

    @Operation(summary = "更新登录信息", description = "更新用户登录IP和登录时间")
    @PutMapping("/{id}/login-info")
    void updateLoginInfo(@Parameter(description = "用户ID") @PathVariable Long id,
                         @Parameter(description = "登录IP") @RequestParam String loginIp,
                         @Parameter(description = "登录时间") @RequestParam String loginTime);
}
