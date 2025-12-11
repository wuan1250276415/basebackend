package com.basebackend.feign.fallback;

import com.basebackend.common.model.Result;
import com.basebackend.feign.client.UserFeignClient;
import com.basebackend.feign.dto.user.UserBasicDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户服务 Feign 降级处理工厂
 *
 * @author Claude Code
 * @since 2025-11-08
 */
@Slf4j
@Component
public class UserFeignFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {

            @Override
            public Result<UserBasicDTO> getById(Long id) {
                log.error("[Feign降级] 根据ID查询用户失败: userId={}, error={}", id, cause.getMessage(), cause);
                return Result.error("用户服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<UserBasicDTO> getByUsername(String username) {
                log.error("[Feign降级] 根据用户名查询用户失败: username={}, error={}", username, cause.getMessage(), cause);
                return Result.error("用户服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<UserBasicDTO> getByPhone(String phone) {
                log.error("[Feign降级] 根据手机号查询用户失败: phone={}, error={}", phone, cause.getMessage(), cause);
                return Result.error("用户服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<UserBasicDTO> getByEmail(String email) {
                log.error("[Feign降级] 根据邮箱查询用户失败: email={}, error={}", email, cause.getMessage(), cause);
                return Result.error("用户服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<UserBasicDTO>> getBatchByIds(String userIds) {
                log.error("[Feign降级] 批量查询用户失败: userIds={}, error={}", userIds, cause.getMessage(), cause);
                return Result.error("用户服务暂时不可用，请稍后重试");
            }

            @Override
            public Result<List<UserBasicDTO>> getByDeptId(Long deptId) {
                log.error("[Feign降级] 根据部门ID查询用户失败: deptId={}, error={}", deptId, cause.getMessage(), cause);
                return Result.success("用户服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<List<Long>> getUserRoles(Long userId) {
                log.error("[Feign降级] 获取用户角色失败: userId={}, error={}", userId, cause.getMessage(), cause);
                return Result.success("用户服务暂时不可用，返回空列表", Collections.emptyList());
            }

            @Override
            public Result<Boolean> checkUsernameUnique(String username, Long userId) {
                log.error("[Feign降级] 检查用户名唯一性失败: username={}, error={}", username, cause.getMessage(), cause);
                // 降级时返回不唯一，避免误操作
                return Result.success("用户服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<Boolean> checkEmailUnique(String email, Long userId) {
                log.error("[Feign降级] 检查邮箱唯一性失败: email={}, error={}", email, cause.getMessage(), cause);
                return Result.success("用户服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<Boolean> checkPhoneUnique(String phone, Long userId) {
                log.error("[Feign降级] 检查手机号唯一性失败: phone={}, error={}", phone, cause.getMessage(), cause);
                return Result.success("用户服务暂时不可用，建议稍后重试", false);
            }

            @Override
            public Result<List<Long>> getAllActiveUserIds() {
                log.error("[Feign降级] 获取所有活跃用户ID失败: error={}", cause.getMessage(), cause);
                return Result.success("用户服务暂时不可用，返回空列表", Collections.emptyList());
            }
        };
    }
}
