package com.basebackend.service.client;

import com.basebackend.api.model.user.UserBasicDTO;
import com.basebackend.common.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * 用户服务客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@HttpExchange("/api/user/users")
public interface UserServiceClient {

    @GetExchange("/{id}")
    @Operation(summary = "根据ID获取用户信息")
    Result<UserBasicDTO> getById(@PathVariable("id") Long id);

    @GetExchange("/by-username")
    @Operation(summary = "根据用户名获取用户信息")
    Result<UserBasicDTO> getByUsername(@RequestParam("username") String username);

    @GetExchange("/by-phone")
    @Operation(summary = "根据手机号获取用户信息")
    Result<UserBasicDTO> getByPhone(@RequestParam("phone") String phone);

    @GetExchange("/by-email")
    @Operation(summary = "根据邮箱获取用户信息")
    Result<UserBasicDTO> getByEmail(@RequestParam("email") String email);

    @GetExchange("/batch")
    @Operation(summary = "批量获取用户信息")
    Result<List<UserBasicDTO>> getBatchByIds(@RequestParam("userIds") String userIds);

    @GetExchange("/by-dept")
    @Operation(summary = "根据部门ID获取用户列表")
    Result<List<UserBasicDTO>> getByDeptId(@RequestParam("deptId") Long deptId);

    @GetExchange("/{id}/roles")
    @Operation(summary = "获取用户角色列表")
    Result<List<Long>> getUserRoles(@PathVariable("id") Long userId);

    @GetExchange("/check-username")
    @Operation(summary = "检查用户名唯一性")
    Result<Boolean> checkUsernameUnique(@RequestParam("username") String username,
                                        @RequestParam(value = "userId", required = false) Long userId);

    @GetExchange("/check-email")
    @Operation(summary = "检查邮箱唯一性")
    Result<Boolean> checkEmailUnique(@RequestParam("email") String email,
                                     @RequestParam(value = "userId", required = false) Long userId);

    @GetExchange("/check-phone")
    @Operation(summary = "检查手机号唯一性")
    Result<Boolean> checkPhoneUnique(@RequestParam("phone") String phone,
                                     @RequestParam(value = "userId", required = false) Long userId);

    @GetExchange("/active-ids")
    @Operation(summary = "获取所有活跃用户ID")
    Result<List<Long>> getAllActiveUserIds();
}
