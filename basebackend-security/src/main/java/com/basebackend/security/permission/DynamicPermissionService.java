package com.basebackend.security.permission;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 动态权限服务。
 * 支持通过 {@link PermissionDataSource} 接口从外部数据源加载权限。
 *
 * @author BaseBackend Team
 * @since 2025-11-26
 */
@Slf4j
@Service
public class DynamicPermissionService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final PermissionDataSource permissionDataSource;

    /**
     * 权限缓存
     */
    private final Map<Long, CachedPermissions> permissionCache = new ConcurrentHashMap<>();

    /**
     * 权限变更监听器
     */
    private final List<PermissionChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 构造函数，支持注入自定义数据源。
     * 如果没有提供数据源，则使用默认实现。
     */
    @Autowired
    public DynamicPermissionService(@Autowired(required = false) PermissionDataSource permissionDataSource) {
        this.permissionDataSource = permissionDataSource != null
                ? permissionDataSource
                : new DefaultPermissionDataSource();
        log.info("DynamicPermissionService 初始化完成，数据源类型: {}",
                this.permissionDataSource.getClass().getSimpleName());
    }

    /**
     * 添加权限变更监听器
     */
    public void addListener(PermissionChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(Long userId, String permission) {
        try {
            if (userId == null || permission == null) {
                return false;
            }
            Set<String> userPermissions = getUserPermissions(userId);
            return userPermissions.contains(permission) || userPermissions.contains("*:*");
        } catch (Exception e) {
            log.error("检查用户权限失败: userId={}, permission={}", userId, permission, e);
            return false;
        }
    }

    /**
     * 获取用户所有权限
     */
    public Set<String> getUserPermissions(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        long now = System.currentTimeMillis();
        CachedPermissions cached = permissionCache.get(userId);
        if (cached != null && !cached.isExpired(now)) {
            return cached.permissions;
        }
        Set<String> permissions = Collections.unmodifiableSet(loadUserPermissions(userId));
        permissionCache.put(userId, new CachedPermissions(permissions, now + CACHE_TTL.toMillis()));
        return permissions;
    }

    /**
     * 刷新用户权限缓存
     */
    public void refreshUserPermissions(Long userId) {
        if (userId == null) {
            return;
        }
        permissionCache.remove(userId);
        log.info("用户权限缓存已刷新: userId={}", userId);

        // 通知监听器
        listeners.forEach(listener -> listener.onPermissionChanged(userId));
    }

    /**
     * 批量刷新权限缓存
     */
    public void refreshAllPermissions() {
        permissionCache.clear();
        log.info("所有用户权限缓存已刷新");

        // 通知监听器
        listeners.forEach(listener -> listener.onAllPermissionsChanged());
    }

    /**
     * 从数据源加载用户权限。
     */
    private Set<String> loadUserPermissions(Long userId) {
        try {
            if (!permissionDataSource.isAvailable()) {
                log.warn("权限数据源不可用，返回空权限集合: userId={}", userId);
                return Collections.emptySet();
            }
            Set<String> permissions = permissionDataSource.loadPermissions(userId);
            log.debug("加载用户权限成功: userId={}, permissionCount={}", userId,
                    permissions != null ? permissions.size() : 0);
            return permissions != null ? permissions : Collections.emptySet();
        } catch (Exception e) {
            log.error("加载用户权限失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取用户角色。
     */
    public Set<String> getUserRoles(Long userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        try {
            return permissionDataSource.loadRoles(userId);
        } catch (Exception e) {
            log.error("加载用户角色失败: userId={}", userId, e);
            return Collections.emptySet();
        }
    }

    private static final class CachedPermissions {
        private final Set<String> permissions;
        private final long expiresAt;

        private CachedPermissions(Set<String> permissions, long expiresAt) {
            this.permissions = permissions;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired(long now) {
            return now >= expiresAt;
        }
    }

    /**
     * 权限变更监听器
     */
    public interface PermissionChangeListener {
        /**
         * 单个用户权限变更时调用
         */
        void onPermissionChanged(Long userId);

        /**
         * 所有权限变更时调用
         */
        void onAllPermissionsChanged();
    }

    /**
     * 条件权限评估器
     */
    public boolean evaluateCondition(PermissionCondition condition) {
        // TODO: 实现条件权限评估逻辑
        // 可以基于时间、IP、设备等条件进行评估
        return true;
    }

    /**
     * 权限条件
     */
    public static class PermissionCondition {
        private String resourceType;
        private String resourceId;
        private String action;
        private Map<String, Object> context;

        // Getters and Setters
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceId() { return resourceId; }
        public void setResourceId(String resourceId) { this.resourceId = resourceId; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public Map<String, Object> getContext() { return context; }
        public void setContext(Map<String, Object> context) { this.context = context; }
    }
}
