package com.basebackend.feign.client;

import com.basebackend.common.model.Result;
import com.basebackend.feign.constant.FeignServiceConstants;
import com.basebackend.feign.dto.user.UserBasicDTO;
import com.basebackend.feign.fallback.UserFeignFallbackFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@FeignClient(
        name = FeignServiceConstants.ADMIN_SERVICE,
        contextId = "userFeignClient",
        path = "/api/admin/users",
        fallbackFactory = UserFeignFallbackFactory.class
)
public interface UserFeignClient {

    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户", description = "根据ID查询用户详情")
    Result<UserBasicDTO> getById(@Parameter(description = "用户ID") @PathVariable("id") Long id);

    /**
     * 根据用户名获取用户信息
     *
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/by-username")
    @Operation(summary = "根据用户名查询用户", description = "根据用户名查询用户详情")
    Result<UserBasicDTO> getByUsername(@Parameter(description = "用户名") @RequestParam("username") String username);

    /**
     * 根据手机号获取用户信息
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @GetMapping("/by-phone")
    @Operation(summary = "根据手机号查询用户", description = "根据手机号查询用户详情")
    Result<UserBasicDTO> getByPhone(@Parameter(description = "手机号") @RequestParam("phone") String phone);

    /**
     * 根据邮箱获取用户信息
     *
     * @param email 邮箱
     * @return 用户信息
     */
    @GetMapping("/by-email")
    @Operation(summary = "根据邮箱查询用户", description = "根据邮箱查询用户详情")
    Result<UserBasicDTO> getByEmail(@Parameter(description = "邮箱") @RequestParam("email") String email);

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户ID列表（逗号分隔）
     * @return 用户信息列表
     */
    @GetMapping("/batch")
    @Operation(summary = "批量查询用户", description = "根据用户ID列表批量查询用户信息")
    Result<List<UserBasicDTO>> getBatchByIds(@Parameter(description = "用户ID列表") @RequestParam("userIds") String userIds);

    /**
     * 根据部门ID获取用户列表
     *
     * @param deptId 部门ID
     * @return 用户列表
     */
    @GetMapping("/by-dept")
    @Operation(summary = "根据部门ID查询用户", description = "根据部门ID查询用户列表")
    Result<List<UserBasicDTO>> getByDeptId(@Parameter(description = "部门ID") @RequestParam("deptId") Long deptId);

    /**
     * 获取用户角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户角色ID", description = "获取用户角色ID列表")
    Result<List<Long>> getUserRoleIds(@Parameter(description = "用户ID") @PathVariable("id") Long userId);

    /**
     * 检查用户名是否唯一
     *
     * @param username 用户名
     * @param userId   用户ID（更新时传入）
     * @return true-唯一，false-不唯一
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名唯一性", description = "检查用户名是否唯一")
    Result<Boolean> checkUsernameUnique(
            @Parameter(description = "用户名") @RequestParam("username") String username,
            @Parameter(description = "用户ID") @RequestParam(value = "userId", required = false) Long userId
    );

    /**
     * 检查邮箱是否唯一
     *
     * @param email  邮箱
     * @param userId 用户ID（更新时传入）
     * @return true-唯一，false-不唯一
     */
    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱唯一性", description = "检查邮箱是否唯一")
    Result<Boolean> checkEmailUnique(
            @Parameter(description = "邮箱") @RequestParam("email") String email,
            @Parameter(description = "用户ID") @RequestParam(value = "userId", required = false) Long userId
    );

    /**
     * 检查手机号是否唯一
     *
     * @param phone  手机号
     * @param userId 用户ID（更新时传入）
     * @return true-唯一，false-不唯一
     */
    @GetMapping("/check-phone")
    @Operation(summary = "检查手机号唯一性", description = "检查手机号是否唯一")
    Result<Boolean> checkPhoneUnique(
            @Parameter(description = "手机号") @RequestParam("phone") String phone,
            @Parameter(description = "用户ID") @RequestParam(value = "userId", required = false) Long userId
    );

    // ==================== 新增：用户档案服务需要的接口 ====================

    /**
     * 更新用户基本信息
     * 用于 profile-service 更新用户个人资料
     *
     * @param userId 用户ID
     * @param userDTO 用户信息
     * @return 更新结果
     */
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "用户信息")
    @org.springframework.web.bind.annotation.PutMapping("/{id}/profile")
    @Operation(summary = "更新用户个人资料", description = "更新用户基本信息（昵称、邮箱、手机号、头像、性别、生日）")
    Result<Void> updateUserProfile(
            @Parameter(description = "用户ID") @PathVariable("id") Long userId,
            @org.springframework.web.bind.annotation.RequestBody UserBasicDTO userDTO
    );

    /**
     * 修改用户密码
     * 用于 profile-service 修改用户密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 更新结果
     */
    @org.springframework.web.bind.annotation.PutMapping("/{id}/password")
    @Operation(summary = "修改用户密码", description = "修改用户登录密码")
    Result<Void> changePassword(
            @Parameter(description = "用户ID") @PathVariable("id") Long userId,
            @Parameter(description = "旧密码") @RequestParam("oldPassword") String oldPassword,
            @Parameter(description = "新密码") @RequestParam("newPassword") String newPassword
    );

    /**
     * 获取用户权限列表（权限字符串列表）
     * 用于 auth-service 获取用户权限
     *
     * @param userId 用户ID
     * @return 权限字符串列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取用户权限", description = "获取用户权限字符串列表")
    Result<List<String>> getUserPermissions(@Parameter(description = "用户ID") @PathVariable("id") Long userId);

    /**
     * 获取用户角色列表（角色名称列表）
     * 用于 auth-service 获取用户角色
     *
     * @param userId 用户ID
     * @return 角色名称列表
     */
    @GetMapping("/{id}/role-names")
    @Operation(summary = "获取用户角色名称", description = "获取用户角色名称列表")
    Result<List<String>> getUserRoleNames(@Parameter(description = "用户ID") @PathVariable("id") Long userId);

    /**
     * 更新用户登录信息
     * 用于 auth-service 更新用户登录IP和时间
     *
     * @param userId 用户ID
     * @param loginIp 登录IP
     * @param loginTime 登录时间
     * @return 更新结果
     */
    @PutMapping("/{id}/login-info")
    @Operation(summary = "更新登录信息", description = "更新用户登录IP和登录时间")
    Result<Void> updateLoginInfo(
            @Parameter(description = "用户ID") @PathVariable("id") Long userId,
            @Parameter(description = "登录IP") @RequestParam("loginIp") String loginIp,
            @Parameter(description = "登录时间") @RequestParam("loginTime") String loginTime
    );

    /**
     * 重置用户密码
     * 用于 auth-service 管理员重置用户密码
     *
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 重置结果
     */
    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable("id") Long userId,
            @Parameter(description = "新密码") @RequestParam("newPassword") String newPassword
    );
}
