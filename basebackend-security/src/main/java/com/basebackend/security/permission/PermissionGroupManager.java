package com.basebackend.security.permission;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限组管理器
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Component
public class PermissionGroupManager {

    /**
     * 权限组定义
     */
    private final Map<String, PermissionGroup> permissionGroups = new ConcurrentHashMap<>();

    /**
     * 权限组继承关系
     */
    private final Map<String, Set<String>> groupInheritance = new ConcurrentHashMap<>();

    /**
     * 初始化默认权限组
     */
    @PostConstruct
    public void initializeDefaultGroups() {
        // 超级管理员组
        addPermissionGroup(new PermissionGroup("super_admin", "超级管理员",
                Arrays.asList("*:*"), Arrays.asList()));

        // 系统管理员组
        addPermissionGroup(new PermissionGroup("sys_admin", "系统管理员",
                Arrays.asList("system:application:*", "system:dept:*", "system:dict:*",
                        "system:resource:view", "system:resource:list"),
                Arrays.asList("super_admin")));

        // 普通管理员组
        addPermissionGroup(new PermissionGroup("normal_admin", "普通管理员",
                Arrays.asList("system:application:view", "system:dept:view", "system:dict:view",
                        "system:dept:list", "system:dict:list"),
                Arrays.asList()));

        // 普通用户组
        addPermissionGroup(new PermissionGroup("user", "普通用户",
                Arrays.asList("system:dept:view", "system:dict:view"),
                Arrays.asList()));

        log.info("默认权限组初始化完成");
    }

    /**
     * 添加权限组
     */
    public void addPermissionGroup(PermissionGroup group) {
        permissionGroups.put(group.getGroupId(), group);
        groupInheritance.put(group.getGroupId(), new HashSet<>(group.getInheritedGroups()));
        log.info("权限组已添加: groupId={}, name={}", group.getGroupId(), group.getGroupName());
    }

    /**
     * 获取权限组
     */
    public PermissionGroup getPermissionGroup(String groupId) {
        return permissionGroups.get(groupId);
    }

    /**
     * 获取用户所属的所有权限组
     */
    public Set<String> getUserPermissionGroups(Long userId) {
        // TODO: 从数据库查询用户所属的权限组
        // 这里只是示例实现
        Set<String> userGroups = new HashSet<>();

        if (userId == 1L) {
            userGroups.add("super_admin");
        } else if (userId > 1L && userId <= 10L) {
            userGroups.add("sys_admin");
        } else {
            userGroups.add("user");
        }

        return userGroups;
    }

    /**
     * 获取用户的所有权限（包括继承的权限）
     */
    public Set<String> getUserAllPermissions(Long userId) {
        Set<String> allPermissions = new HashSet<>();
        Set<String> userGroups = getUserPermissionGroups(userId);

        // 递归获取权限
        collectGroupPermissions(userGroups, allPermissions, new HashSet<>());

        return allPermissions;
    }

    /**
     * 递归收集权限组及其继承组的权限
     */
    private void collectGroupPermissions(Set<String> groups, Set<String> permissions, Set<String> visited) {
        for (String groupId : groups) {
            if (visited.contains(groupId)) {
                continue; // 避免循环依赖
            }
            visited.add(groupId);

            PermissionGroup group = permissionGroups.get(groupId);
            if (group != null) {
                // 添加当前组的权限
                permissions.addAll(group.getPermissions());

                // 递归处理继承的组
                Set<String> inheritedGroups = groupInheritance.get(groupId);
                if (inheritedGroups != null && !inheritedGroups.isEmpty()) {
                    collectGroupPermissions(inheritedGroups, permissions, visited);
                }
            }
        }
    }

    /**
     * 检查用户是否有指定权限
     */
    public boolean hasPermission(Long userId, String permission) {
        Set<String> userPermissions = getUserAllPermissions(userId);
        return userPermissions.contains(permission) || userPermissions.contains("*:*");
    }

    /**
     * 权限组
     */
    public static class PermissionGroup {
        private String groupId;
        private String groupName;
        private List<String> permissions;
        private List<String> inheritedGroups;

        public PermissionGroup() {}

        public PermissionGroup(String groupId, String groupName, List<String> permissions, List<String> inheritedGroups) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.permissions = permissions;
            this.inheritedGroups = inheritedGroups;
        }

        // Getters and Setters
        public String getGroupId() { return groupId; }
        public void setGroupId(String groupId) { this.groupId = groupId; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
        public List<String> getInheritedGroups() { return inheritedGroups; }
        public void setInheritedGroups(List<String> inheritedGroups) { this.inheritedGroups = inheritedGroups; }
    }
}
