package com.basebackend.security.rbac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 增强的权限计算服务
 * 支持动态权限计算、权限继承、权限缓存和权限变更通知
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedPermissionService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSION_CACHE_KEY = "rbac:permissions:user:";
    private static final String ROLE_CACHE_KEY = "rbac:roles:user:";
    private static final String INHERITANCE_CACHE_KEY = "rbac:inheritance:role:";

    /**
     * 计算用户的所有权限
     *
     * @param userId 用户ID
     * @return 权限集合
     */
    public Set<String> calculateUserPermissions(Long userId) {
        // 1. 尝试从缓存获取
        String cacheKey = PERMISSION_CACHE_KEY + userId;
        Set<String> cachedPermissions = getCachedPermissions(cacheKey);
        if (cachedPermissions != null) {
            log.debug("从缓存获取用户权限: userId={}, count={}", userId, cachedPermissions.size());
            return cachedPermissions;
        }

        // 2. 从数据库查询用户角色
        List<Role> userRoles = getUserRoles(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            log.warn("用户没有分配任何角色: userId={}", userId);
            return new HashSet<>();
        }

        // 3. 计算角色权限 (包括继承的权限)
        Set<String> allPermissions = new HashSet<>();
        for (Role role : userRoles) {
            Set<String> rolePermissions = calculateRolePermissions(role);
            allPermissions.addAll(rolePermissions);
        }

        // 4. 缓存结果
        cachePermissions(cacheKey, allPermissions);

        log.info("计算用户权限完成: userId={}, roles={}, permissions={}",
                userId, userRoles.size(), allPermissions.size());

        return allPermissions;
    }

    /**
     * 计算角色的所有权限 (包括继承的权限)
     *
     * @param role 角色
     * @return 权限集合
     */
    public Set<String> calculateRolePermissions(Role role) {
        Set<String> allPermissions = new HashSet<>();

        // 1. 添加直接权限
        if (role.getDirectPermissions() != null) {
            allPermissions.addAll(role.getDirectPermissions());
        }

        // 2. 添加继承的权限
        if (role.getInheritedPermissions() != null) {
            allPermissions.addAll(role.getInheritedPermissions());
        }

        // 3. 递归获取父角色权限
        if (role.getParentId() != null) {
            Role parentRole = getRoleById(role.getParentId());
            if (parentRole != null) {
                Set<String> parentPermissions = calculateRolePermissions(parentRole);
                allPermissions.addAll(parentPermissions);
            }
        }

        log.debug("计算角色权限: roleCode={}, direct={}, inherited={}, total={}",
                role.getCode(),
                role.getDirectPermissions() != null ? role.getDirectPermissions().size() : 0,
                role.getInheritedPermissions() != null ? role.getInheritedPermissions().size() : 0,
                allPermissions.size());

        return allPermissions;
    }

    /**
     * 检查用户是否拥有指定权限
     *
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否拥有权限
     */
    public boolean hasPermission(Long userId, String permissionCode) {
        if (permissionCode == null || permissionCode.isEmpty()) {
            return false;
        }

        // 1. 超级管理员拥有所有权限
        if (isSuperAdmin(userId)) {
            return true;
        }

        // 2. 计算用户权限
        Set<String> userPermissions = calculateUserPermissions(userId);

        // 3. 检查权限
        boolean hasPermission = userPermissions.contains(permissionCode);

        log.debug("权限检查: userId={}, permission={}, result={}",
                userId, permissionCode, hasPermission);

        return hasPermission;
    }

    /**
     * 检查用户是否拥有指定权限集合中的任意一个
     *
     * @param userId 用户ID
     * @param permissionCodes 权限编码集合
     * @return 是否拥有权限
     */
    public boolean hasAnyPermission(Long userId, String... permissionCodes) {
        for (String permissionCode : permissionCodes) {
            if (hasPermission(userId, permissionCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查用户是否拥有指定权限集合中的所有权限
     *
     * @param userId 用户ID
     * @param permissionCodes 权限编码集合
     * @return 是否拥有所有权限
     */
    public boolean hasAllPermissions(Long userId, String... permissionCodes) {
        Set<String> userPermissions = calculateUserPermissions(userId);
        for (String permissionCode : permissionCodes) {
            if (!userPermissions.contains(permissionCode)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取用户的数据范围
     *
     * @param userId 用户ID
     * @return 数据范围
     */
    public PermissionContext.DataScope getUserDataScope(Long userId) {
        List<Role> userRoles = getUserRoles(userId);
        if (CollectionUtils.isEmpty(userRoles)) {
            return PermissionContext.DataScope.SELF;
        }

        // 取最小数据范围 (限制最严格的数据范围)
        PermissionContext.DataScope minScope = PermissionContext.DataScope.ALL;
        for (Role role : userRoles) {
            if (role.getDataScope() != null) {
                minScope = getMoreRestrictiveScope(minScope, role.getDataScope());
            }
        }

        return minScope;
    }

    /**
     * 检查用户是否满足数据范围要求
     *
     * @param userId 用户ID
     * @param resourceOwnerId 资源Owner ID
     * @param resourceDeptId 资源所属部门ID
     * @return 是否满足
     */
    public boolean checkDataScope(Long userId, Long resourceOwnerId, Long resourceDeptId) {
        PermissionContext.DataScope dataScope = getUserDataScope(userId);
        Long userDeptId = getUserDeptId(userId);

        switch (dataScope) {
            case ALL:
                return true;
            case SELF:
                return Objects.equals(userId, resourceOwnerId);
            case DEPT:
                return Objects.equals(userDeptId, resourceDeptId);
            case DEPT_AND_CHILD:
            case DEPT_AND_SUB:
                // 检查是否为同一部门或子部门
                return isSameOrChildDept(userDeptId, resourceDeptId);
            case CUSTOM:
                // 自定义数据范围检查
                return checkCustomDataScope(userId, resourceOwnerId, resourceDeptId);
            default:
                return false;
        }
    }

    /**
     * 获取用户的角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    public List<Role> getUserRoles(Long userId) {
        // 从数据库查询用户角色
        // TODO: 实现具体的数据库查询
        return new ArrayList<>();
    }

    /**
     * 根据ID获取角色
     *
     * @param roleId 角色ID
     * @return 角色
     */
    public Role getRoleById(Long roleId) {
        // 从数据库查询角色
        // TODO: 实现具体的数据库查询
        return null;
    }

    /**
     * 获取用户部门ID
     *
     * @param userId 用户ID
     * @return 部门ID
     */
    private Long getUserDeptId(Long userId) {
        // 从数据库查询用户信息
        // TODO: 实现具体的数据库查询
        return 1L; // 默认值
    }

    /**
     * 检查是否为超级管理员
     *
     * @param userId 用户ID
     * @return 是否为超级管理员
     */
    private boolean isSuperAdmin(Long userId) {
        // 查询用户是否拥有超级管理员角色
        // TODO: 实现具体的数据库查询
        return false;
    }

    /**
     * 获取更严格的数据范围
     */
    private PermissionContext.DataScope getMoreRestrictiveScope(
            PermissionContext.DataScope current,
            PermissionContext.DataScope target) {
        // 数据范围严格性排序: ALL < DEPT_AND_SUB < DEPT < SELF
        int currentOrder = getScopeOrder(current);
        int targetOrder = getScopeOrder(target);
        return targetOrder < currentOrder ? target : current;
    }

    /**
     * 获取数据范围的严格性排序
     */
    private int getScopeOrder(PermissionContext.DataScope scope) {
        switch (scope) {
            case ALL:
                return 4;
            case DEPT_AND_SUB:
                return 3;
            case DEPT:
                return 2;
            case SELF:
                return 1;
            case CUSTOM:
                return 0;
            default:
                return 0;
        }
    }

    /**
     * 检查是否为同一部门或子部门
     */
    private boolean isSameOrChildDept(Long userDeptId, Long resourceDeptId) {
        // TODO: 实现部门层级检查
        return Objects.equals(userDeptId, resourceDeptId);
    }

    /**
     * 检查自定义数据范围
     */
    private boolean checkCustomDataScope(Long userId, Long resourceOwnerId, Long resourceDeptId) {
        // TODO: 实现自定义数据范围检查逻辑
        return false;
    }

    /**
     * 从缓存获取权限
     */
    private Set<String> getCachedPermissions(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Set) {
                return (Set<String>) cached;
            }
        } catch (Exception e) {
            log.error("从缓存获取权限失败", e);
        }
        return null;
    }

    /**
     * 缓存权限
     */
    private void cachePermissions(String cacheKey, Set<String> permissions) {
        try {
            // 缓存1小时
            redisTemplate.opsForValue().set(cacheKey, permissions, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("缓存权限失败", e);
        }
    }

    /**
     * 清除用户权限缓存
     *
     * @param userId 用户ID
     */
    public void clearUserPermissionCache(Long userId) {
        String cacheKey = PERMISSION_CACHE_KEY + userId;
        try {
            redisTemplate.delete(cacheKey);
            log.info("清除用户权限缓存: userId={}", userId);
        } catch (Exception e) {
            log.error("清除用户权限缓存失败", e);
        }
    }

    /**
     * 清除所有用户权限缓存
     */
    public void clearAllPermissionCaches() {
        try {
            Set<String> keys = redisTemplate.keys(PERMISSION_CACHE_KEY + "*");
            if (!CollectionUtils.isEmpty(keys)) {
                redisTemplate.delete(keys);
                log.info("清除所有用户权限缓存: count={}", keys.size());
            }
        } catch (Exception e) {
            log.error("清除所有用户权限缓存失败", e);
        }
    }

    /**
     * 预加载用户权限
     *
     * @param userId 用户ID
     */
    public void preloadUserPermissions(Long userId) {
        log.info("预加载用户权限: userId={}", userId);
        calculateUserPermissions(userId);
    }

    /**
     * 批量预加载多个用户的权限
     *
     * @param userIds 用户ID列表
     */
    public void batchPreloadUserPermissions(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        log.info("批量预加载用户权限: count={}", userIds.size());

        // 使用并行流提高性能
        userIds.parallelStream().forEach(this::preloadUserPermissions);
    }

    /**
     * 获取权限使用统计
     *
     * @return 权限使用统计
     */
    public Map<String, Object> getPermissionStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 统计缓存中的用户数
            Set<String> userCacheKeys = redisTemplate.keys(PERMISSION_CACHE_KEY + "*");
            stats.put("cachedUsers", userCacheKeys != null ? userCacheKeys.size() : 0);

            // 统计总权限数
            // TODO: 从数据库统计权限总数
            stats.put("totalPermissions", 0);

            // 统计角色总数
            // TODO: 从数据库统计角色总数
            stats.put("totalRoles", 0);

            // 统计平均每用户权限数
            Set<String> allPermissions = new HashSet<>();
            if (userCacheKeys != null) {
                for (String key : userCacheKeys) {
                    Set<String> permissions = getCachedPermissions(key);
                    if (permissions != null) {
                        allPermissions.addAll(permissions);
                    }
                }
            }
            stats.put("averagePermissionsPerUser", allPermissions.size());

        } catch (Exception e) {
            log.error("获取权限统计失败", e);
        }

        return stats;
    }
}
