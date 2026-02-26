package com.basebackend.service.client.fallback;

import com.basebackend.api.model.user.UserBasicDTO;
import com.basebackend.common.model.Result;
import com.basebackend.service.client.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户服务客户端降级实现
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Component
public class UserServiceClientFallback implements UserServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UserServiceClientFallback.class);

    @Override
    public Result<UserBasicDTO> getById(Long id) {
        log.error("[服务降级] 获取用户信息失败: userId={}", id);
        return Result.error("用户服务不可用，获取用户信息失败");
    }

    @Override
    public Result<UserBasicDTO> getByUsername(String username) {
        log.error("[服务降级] 根据用户名获取用户信息失败: username={}", username);
        return Result.error("用户服务不可用，获取用户信息失败");
    }

    @Override
    public Result<UserBasicDTO> getByPhone(String phone) {
        log.error("[服务降级] 根据手机号获取用户信息失败: phone={}", phone);
        return Result.error("用户服务不可用，获取用户信息失败");
    }

    @Override
    public Result<UserBasicDTO> getByEmail(String email) {
        log.error("[服务降级] 根据邮箱获取用户信息失败: email={}", email);
        return Result.error("用户服务不可用，获取用户信息失败");
    }

    @Override
    public Result<List<UserBasicDTO>> getBatchByIds(String userIds) {
        log.error("[服务降级] 批量获取用户信息失败: userIds={}", userIds);
        return Result.success("用户服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<UserBasicDTO>> getByDeptId(Long deptId) {
        log.error("[服务降级] 根据部门ID获取用户列表失败: deptId={}", deptId);
        return Result.success("用户服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<List<Long>> getUserRoles(Long userId) {
        log.error("[服务降级] 获取用户角色列表失败: userId={}", userId);
        return Result.success("用户服务降级，返回空列表", Collections.emptyList());
    }

    @Override
    public Result<Boolean> checkUsernameUnique(String username, Long userId) {
        log.error("[服务降级] 检查用户名唯一性失败: username={}", username);
        return Result.success("用户服务降级", false);
    }

    @Override
    public Result<Boolean> checkEmailUnique(String email, Long userId) {
        log.error("[服务降级] 检查邮箱唯一性失败: email={}", email);
        return Result.success("用户服务降级", false);
    }

    @Override
    public Result<Boolean> checkPhoneUnique(String phone, Long userId) {
        log.error("[服务降级] 检查手机号唯一性失败: phone={}", phone);
        return Result.success("用户服务降级", false);
    }

    @Override
    public Result<List<Long>> getAllActiveUserIds() {
        log.error("[服务降级] 获取所有活跃用户ID失败");
        return Result.success("用户服务降级，返回空列表", Collections.emptyList());
    }
}
