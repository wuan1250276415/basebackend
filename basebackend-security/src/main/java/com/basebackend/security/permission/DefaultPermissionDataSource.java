package com.basebackend.security.permission;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 默认权限数据源实现（仅用于开发和测试）。
 * 生产环境应该提供自定义实现并注入到Spring容器中。
 *
 * @author BaseBackend Team
 * @since 2025-12-08
 */
@Slf4j
public class DefaultPermissionDataSource implements PermissionDataSource {

    public DefaultPermissionDataSource() {
        log.warn("使用默认权限数据源，仅适用于开发测试环境。生产环境请实现 PermissionDataSource 接口。");
    }

    @Override
    public Set<String> loadPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<String> permissions = new HashSet<>();

        // 默认实现：userId=1 为超级管理员
        if (userId == 1L) {
            permissions.add("*:*");
        } else {
            // 普通用户默认权限
            permissions.add("system:dept:view");
            permissions.add("system:dict:view");
            permissions.add("system:user:view");
        }

        log.debug("默认数据源加载用户权限: userId={}, permissions={}", userId, permissions);
        return permissions;
    }

    @Override
    public Set<String> loadRoles(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }

        Set<String> roles = new HashSet<>();

        // 默认实现
        if (userId == 1L) {
            roles.add("ROLE_ADMIN");
            roles.add("ROLE_SUPER_ADMIN");
        } else {
            roles.add("ROLE_USER");
        }

        log.debug("默认数据源加载用户角色: userId={}, roles={}", userId, roles);
        return roles;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
