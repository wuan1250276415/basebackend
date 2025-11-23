package com.basebackend.system.service.impl;

import com.basebackend.security.service.PermissionService;
import com.basebackend.feign.client.UserFeignClient;
import com.basebackend.feign.dto.user.UserBasicDTO;
import com.basebackend.common.model.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统服务权限服务实现
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemPermissionServiceImpl implements PermissionService {

    private final UserFeignClient userFeignClient;

    @Override
    public List<String> getCurrentUserPermissions() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }

        try {
            // 获取用户角色
            Result<List<Long>> rolesResult = userFeignClient.getUserRoles(userId);
            if (rolesResult == null || !rolesResult.isSuccess() || rolesResult.getData() == null) {
                log.warn("获取用户角色失败: userId={}", userId);
                return List.of();
            }

            List<Long> roleIds = rolesResult.getData();
            if (roleIds.isEmpty()) {
                return List.of();
            }

            // 将角色ID转换为权限标识
            // 这里采用简化的权限映射策略：系统管理相关权限
            List<String> permissions = new ArrayList<>();

            // 根据角色ID分配系统管理权限
            for (Long roleId : roleIds) {
                switch (roleId.intValue()) {
                    case 1: // 超级管理员
                        permissions.addAll(getAdminPermissions());
                        break;
                    case 2: // 系统管理员
                        permissions.addAll(getSysAdminPermissions());
                        break;
                    case 3: // 普通管理员
                        permissions.addAll(getNormalAdminPermissions());
                        break;
                    default:
                        permissions.addAll(getDefaultPermissions());
                        break;
                }
            }

            // 去重并返回
            return permissions.stream().distinct().collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户权限失败: userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public List<String> getCurrentUserRoles() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }

        try {
            Result<List<Long>> rolesResult = userFeignClient.getUserRoles(userId);
            if (rolesResult == null || !rolesResult.isSuccess() || rolesResult.getData() == null) {
                return List.of();
            }

            List<Long> roleIds = rolesResult.getData();

            // 将角色ID转换为角色标识
            return roleIds.stream()
                    .map(this::getRoleKey)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户角色失败: userId={}", userId, e);
            return List.of();
        }
    }

    @Override
    public Long getCurrentUserId() {
        // 从 SecurityContext 中获取当前用户ID
        try {
            org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                String userIdStr = authentication.getName();
                if (userIdStr != null && userIdStr.matches("\\d+")) {
                    return Long.valueOf(userIdStr);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户ID失败", e);
        }
        return null;
    }

    @Override
    public Long getCurrentUserDeptId() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return null;
        }

        try {
            // 通过 FeignClient 获取用户基本信息，提取部门ID
            Result<UserBasicDTO> userResult = userFeignClient.getById(userId);
            if (userResult != null && userResult.isSuccess() && userResult.getData() != null) {
                return userResult.getData().getDeptId();
            }
            log.warn("获取用户部门信息失败: userId={}", userId);
            return null;
        } catch (Exception e) {
            log.error("获取用户部门信息异常: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 获取管理员权限
     */
    private List<String> getAdminPermissions() {
        return List.of(
            "system:application:*",
            "system:dept:*",
            "system:dict:*",
            "system:resource:*",
            "system:user:view",
            "system:user:list",
            "system:user:create",
            "system:user:update",
            "system:user:delete",
            "system:role:*"
        );
    }

    /**
     * 获取系统管理员权限
     */
    private List<String> getSysAdminPermissions() {
        return List.of(
            "system:application:view",
            "system:application:list",
            "system:application:create",
            "system:application:update",
            "system:dept:view",
            "system:dept:list",
            "system:dept:create",
            "system:dept:update",
            "system:dept:delete",
            "system:dict:view",
            "system:dict:list",
            "system:dict:create",
            "system:dict:update",
            "system:dict:delete",
            "system:resource:view",
            "system:resource:list"
        );
    }

    /**
     * 获取普通管理员权限
     */
    private List<String> getNormalAdminPermissions() {
        return List.of(
            "system:application:view",
            "system:application:list",
            "system:dept:view",
            "system:dept:list",
            "system:dict:view",
            "system:dict:list"
        );
    }

    /**
     * 获取默认权限
     */
    private List<String> getDefaultPermissions() {
        return List.of(
            "system:application:view",
            "system:dept:view",
            "system:dict:view"
        );
    }

    /**
     * 将角色ID转换为角色标识
     */
    private String getRoleKey(Long roleId) {
        return switch (roleId.intValue()) {
            case 1 -> "admin";
            case 2 -> "sys_admin";
            case 3 -> "admin";
            default -> "user";
        };
    }
}
