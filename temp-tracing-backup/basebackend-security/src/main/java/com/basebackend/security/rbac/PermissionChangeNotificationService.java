package com.basebackend.security.rbac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限变更通知服务
 * 负责在权限变更时通知相关系统和缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionChangeNotificationService {

    @Autowired
    private EnhancedPermissionService permissionService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private static final String PERMISSION_CHANGE_TOPIC = "permission-change";
    private static final String CACHE_INVALIDATION_TOPIC = "cache-invalidation";

    /**
     * 通知角色权限变更
     *
     * @param roleId 角色ID
     * @param roleName 角色名称
     * @param affectedUserIds 受影响的用户ID列表
     * @param changedPermissions 变更的权限列表
     * @param changeType 变更类型 (ADD, REMOVE, MODIFY)
     */
    @Async
    public void notifyRolePermissionChange(Long roleId, String roleName, List<Long> affectedUserIds,
                                         Set<String> changedPermissions, ChangeType changeType) {
        log.info("通知角色权限变更: roleId={}, type={}, affectedUsers={}, permissions={}",
                roleId, changeType, affectedUserIds.size(), changedPermissions.size());

        try {
            // 1. 清除受影响的用户缓存
            clearAffectedUserCaches(affectedUserIds);

            // 2. 发送权限变更通知
            sendPermissionChangeNotification(roleId, roleName, affectedUserIds, changedPermissions, changeType);

            // 3. 记录权限变更日志
            logRoleChange(roleId, roleName, affectedUserIds, changedPermissions, changeType);

        } catch (Exception e) {
            log.error("通知角色权限变更失败", e);
        }
    }

    /**
     * 通知用户角色变更
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param addedRoles 新增的角色
     * @param removedRoles 移除的角色
     * @param changeType 变更类型
     */
    @Async
    public void notifyUserRoleChange(Long userId, String username, List<Role> addedRoles,
                                   List<Role> removedRoles, ChangeType changeType) {
        log.info("通知用户角色变更: userId={}, type={}, added={}, removed={}",
                userId, changeType, addedRoles.size(), removedRoles.size());

        try {
            // 1. 清除用户权限缓存
            permissionService.clearUserPermissionCache(userId);

            // 2. 重新计算用户权限
            Set<String> newPermissions = permissionService.calculateUserPermissions(userId);

            // 3. 发送用户角色变更通知
            sendUserRoleChangeNotification(userId, username, addedRoles, removedRoles, newPermissions, changeType);

            // 4. 记录用户角色变更日志
            logUserRoleChange(userId, username, addedRoles, removedRoles, changeType);

            // 5. 如果有新增权限，发送权限增加通知
            if (!addedRoles.isEmpty()) {
                notifyPermissionIncrease(userId, username, newPermissions);
            }

        } catch (Exception e) {
            log.error("通知用户角色变更失败", e);
        }
    }

    /**
     * 通知权限定义变更
     *
     * @param permissionId 权限ID
     * @param permissionName 权限名称
     * @param changeType 变更类型
     */
    @Async
    public void notifyPermissionDefinitionChange(Long permissionId, String permissionName, ChangeType changeType) {
        log.info("通知权限定义变更: permissionId={}, name={}, type={}",
                permissionId, permissionName, changeType);

        try {
            // 1. 清除所有用户权限缓存 (因为权限定义变更可能影响所有用户)
            permissionService.clearAllPermissionCaches();

            // 2. 发送权限定义变更通知
            sendPermissionDefinitionChangeNotification(permissionId, permissionName, changeType);

            // 3. 记录权限定义变更日志
            logPermissionDefinitionChange(permissionId, permissionName, changeType);

        } catch (Exception e) {
            log.error("通知权限定义变更失败", e);
        }
    }

    /**
     * 清除受影响的用户缓存
     */
    private void clearAffectedUserCaches(List<Long> affectedUserIds) {
        if (affectedUserIds == null || affectedUserIds.isEmpty()) {
            return;
        }

        log.debug("清除受影响的用户缓存: count={}", affectedUserIds.size());

        for (Long userId : affectedUserIds) {
            permissionService.clearUserPermissionCache(userId);
        }
    }

    /**
     * 发送权限变更通知
     */
    private void sendPermissionChangeNotification(Long roleId, String roleName, List<Long> affectedUserIds,
                                                Set<String> changedPermissions, ChangeType changeType) {
        Map<String, Object> message = new HashMap<>();
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("eventType", "ROLE_PERMISSION_CHANGE");
        message.put("roleId", roleId);
        message.put("roleName", roleName);
        message.put("changeType", changeType.name());
        message.put("affectedUserIds", affectedUserIds);
        message.put("changedPermissions", changedPermissions);

        kafkaTemplate.send(PERMISSION_CHANGE_TOPIC, message);
    }

    /**
     * 发送用户角色变更通知
     */
    private void sendUserRoleChangeNotification(Long userId, String username, List<Role> addedRoles,
                                              List<Role> removedRoles, Set<String> newPermissions,
                                              ChangeType changeType) {
        Map<String, Object> message = new HashMap<>();
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("eventType", "USER_ROLE_CHANGE");
        message.put("userId", userId);
        message.put("username", username);
        message.put("changeType", changeType.name());
        message.put("addedRoles", addedRoles.stream().map(Role::getCode).collect(Collectors.toList()));
        message.put("removedRoles", removedRoles.stream().map(Role::getCode).collect(Collectors.toList()));
        message.put("newPermissions", newPermissions);

        kafkaTemplate.send(PERMISSION_CHANGE_TOPIC, message);
    }

    /**
     * 发送权限定义变更通知
     */
    private void sendPermissionDefinitionChangeNotification(Long permissionId, String permissionName,
                                                          ChangeType changeType) {
        Map<String, Object> message = new HashMap<>();
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("eventType", "PERMISSION_DEFINITION_CHANGE");
        message.put("permissionId", permissionId);
        message.put("permissionName", permissionName);
        message.put("changeType", changeType.name());

        kafkaTemplate.send(PERMISSION_CHANGE_TOPIC, message);
    }

    /**
     * 记录权限变更日志
     */
    private void logRoleChange(Long roleId, String roleName, List<Long> affectedUserIds,
                             Set<String> changedPermissions, ChangeType changeType) {
        // 使用审计服务记录日志
        // TODO: 注入SecurityAuditService并记录日志
        log.info("角色权限变更记录: role={}, type={}, affectedUsers={}, permissions={}",
                roleName, changeType, affectedUserIds.size(), changedPermissions.size());
    }

    /**
     * 记录用户角色变更日志
     */
    private void logUserRoleChange(Long userId, String username, List<Role> addedRoles,
                                 List<Role> removedRoles, ChangeType changeType) {
        // 使用审计服务记录日志
        // TODO: 注入SecurityAuditService并记录日志
        log.info("用户角色变更记录: user={}, type={}, added={}, removed={}",
                username, changeType, addedRoles.size(), removedRoles.size());
    }

    /**
     * 记录权限定义变更日志
     */
    private void logPermissionDefinitionChange(Long permissionId, String permissionName, ChangeType changeType) {
        // 使用审计服务记录日志
        // TODO: 注入SecurityAuditService并记录日志
        log.info("权限定义变更记录: permission={}, type={}", permissionName, changeType);
    }

    /**
     * 通知权限增加
     */
    private void notifyPermissionIncrease(Long userId, String username, Set<String> newPermissions) {
        log.info("通知用户权限增加: user={}, newPermissions={}", username, newPermissions.size());

        // 可以在这里实现具体的通知逻辑，如：
        // 1. 发送邮件通知
        // 2. 发送系统通知
        // 3. 记录审计日志
        // 4. 触发其他业务逻辑
    }

    /**
     * 发送缓存失效通知
     *
     * @param userId 用户ID
     * @param cacheKeys 需要失效的缓存键
     */
    @Async
    public void sendCacheInvalidation(Long userId, List<String> cacheKeys) {
        log.debug("发送缓存失效通知: userId={}, caches={}", userId, cacheKeys.size());

        Map<String, Object> message = new HashMap<>();
        message.put("timestamp", LocalDateTime.now().toString());
        message.put("eventType", "CACHE_INVALIDATION");
        message.put("userId", userId);
        message.put("cacheKeys", cacheKeys);

        kafkaTemplate.send(CACHE_INVALIDATION_TOPIC, message);
    }

    /**
     * 批量通知多个用户的权限变更
     *
     * @param userIds 用户ID列表
     * @param changeType 变更类型
     */
    @Async
    public void batchNotifyPermissionChange(List<Long> userIds, ChangeType changeType) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }

        log.info("批量通知权限变更: users={}, type={}", userIds.size(), changeType);

        try {
            // 1. 清除所有用户缓存
            for (Long userId : userIds) {
                permissionService.clearUserPermissionCache(userId);
            }

            // 2. 发送批量变更通知
            Map<String, Object> message = new HashMap<>();
            message.put("timestamp", LocalDateTime.now().toString());
            message.put("eventType", "BATCH_PERMISSION_CHANGE");
            message.put("changeType", changeType.name());
            message.put("userIds", userIds);

            kafkaTemplate.send(PERMISSION_CHANGE_TOPIC, message);

        } catch (Exception e) {
            log.error("批量通知权限变更失败", e);
        }
    }

    /**
     * 变更类型枚举
     */
    public enum ChangeType {
        /**
         * 新增
         */
        ADD,
        /**
         * 移除
         */
        REMOVE,
        /**
         * 修改
         */
        MODIFY,
        /**
         * 删除
         */
        DELETE,
        /**
         * 批量操作
         */
        BATCH
    }
}
