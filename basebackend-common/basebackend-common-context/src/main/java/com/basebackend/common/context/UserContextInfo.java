package com.basebackend.common.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * 用户上下文信息接口
 * <p>
 * 定义用户上下文的标准契约，所有需要传递用户信息的场景都应实现此接口。
 * 该接口仅定义数据结构，不包含任何业务逻辑。
 * </p>
 *
 * <h3>设计原则：</h3>
 * <ul>
 *   <li>仅包含用户身份和基本信息的只读访问器</li>
 *   <li>不依赖任何具体实现（如 Spring Security）</li>
 *   <li>支持跨模块、跨服务传递用户上下文</li>
 * </ul>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取当前用户上下文
 * UserContextInfo userContext = UserContextHolder.get();
 * Long userId = userContext.getUserId();
 * String username = userContext.getUsername();
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public interface UserContextInfo extends Serializable {

    /**
     * 获取用户ID
     *
     * @return 用户唯一标识
     */
    Long getUserId();

    /**
     * 获取用户名
     *
     * @return 用户登录名
     */
    String getUsername();

    /**
     * 获取用户昵称/显示名
     *
     * @return 用户昵称，可能为 null
     */
    default String getNickname() {
        return getUsername();
    }

    /**
     * 获取用户所属部门ID
     *
     * @return 部门ID，可能为 null
     */
    default Long getDeptId() {
        return null;
    }

    /**
     * 获取用户角色编码集合
     *
     * @return 角色编码集合，不会返回 null
     */
    default Set<String> getRoleCodes() {
        return Set.of();
    }

    /**
     * 获取用户权限编码集合
     *
     * @return 权限编码集合，不会返回 null
     */
    default Set<String> getPermissions() {
        return Set.of();
    }

    /**
     * 判断用户是否拥有指定角色
     *
     * @param roleCode 角色编码
     * @return 是否拥有该角色
     */
    default boolean hasRole(String roleCode) {
        return getRoleCodes().contains(roleCode);
    }

    /**
     * 判断用户是否拥有指定权限
     *
     * @param permission 权限编码
     * @return 是否拥有该权限
     */
    default boolean hasPermission(String permission) {
        return getPermissions().contains(permission);
    }

    /**
     * 判断用户是否拥有任意一个指定角色
     *
     * @param roleCodes 角色编码集合
     * @return 是否拥有任意一个角色
     */
    default boolean hasAnyRole(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return false;
        }
        Set<String> userRoles = getRoleCodes();
        return roleCodes.stream().anyMatch(userRoles::contains);
    }

    /**
     * 判断用户是否拥有任意一个指定权限
     *
     * @param permissions 权限编码集合
     * @return 是否拥有任意一个权限
     */
    default boolean hasAnyPermission(Collection<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        Set<String> userPerms = getPermissions();
        return permissions.stream().anyMatch(userPerms::contains);
    }

    /**
     * 判断是否为超级管理员
     *
     * @return 是否为超级管理员
     */
    default boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN") || hasRole("ROLE_SUPER_ADMIN");
    }

    /**
     * 获取用户数据范围（数据权限）
     * <p>
     * 数据范围定义：
     * </p>
     * <ul>
     *   <li>1: 全部数据</li>
     *   <li>2: 本部门及以下数据</li>
     *   <li>3: 本部门数据</li>
     *   <li>4: 仅本人数据</li>
     *   <li>5: 自定义数据</li>
     * </ul>
     *
     * @return 数据范围类型，默认返回 4（仅本人数据）
     */
    default Integer getDataScope() {
        return 4;
    }
}
