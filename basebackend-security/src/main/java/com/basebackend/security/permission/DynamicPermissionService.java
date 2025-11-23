package com.basebackend.security.permission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态权限服务
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicPermissionService {

    /**
     * 权限缓存
     */
    private final Map<String, Set<String>> permissionCache = new ConcurrentHashMap<>();

    /**
     * 权限变更监听器
     */
    private final List<PermissionChangeListener> listeners = new ArrayList<>();

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
        String cacheKey = "user:" + userId;
        return permissionCache.computeIfAbsent(cacheKey, k -> loadUserPermissions(userId));
    }

    /**
     * 刷新用户权限缓存
     */
    public void refreshUserPermissions(Long userId) {
        String cacheKey = "user:" + userId;
        permissionCache.remove(cacheKey);
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
     * 加载用户权限（实际项目中应该从数据库或权限中心加载）
     */
    private Set<String> loadUserPermissions(Long userId) {
        // TODO: 从数据库加载用户权限
        // 这里只是示例实现
        Set<String> permissions = new HashSet<>();

        // 根据用户ID模拟权限加载
        if (userId == 1L) {
            // 超级管理员
            permissions.add("*:*");
        } else {
            // 普通用户
            permissions.add("system:dept:view");
            permissions.add("system:dict:view");
        }

        return permissions;
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
