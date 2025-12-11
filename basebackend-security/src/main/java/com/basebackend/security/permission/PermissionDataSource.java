package com.basebackend.security.permission;

import java.util.Set;

/**
 * 权限数据源接口。
 * 各微服务需要实现此接口来提供用户权限数据的加载逻辑。
 *
 * @author BaseBackend Team
 * @since 2025-12-08
 */
public interface PermissionDataSource {

    /**
     * 加载指定用户的所有权限标识。
     *
     * @param userId 用户ID
     * @return 权限标识集合，不应返回null
     */
    Set<String> loadPermissions(Long userId);

    /**
     * 加载指定用户的所有角色标识。
     *
     * @param userId 用户ID
     * @return 角色标识集合，不应返回null
     */
    Set<String> loadRoles(Long userId);

    /**
     * 检查数据源是否可用。
     *
     * @return true表示可用
     */
    default boolean isAvailable() {
        return true;
    }
}
