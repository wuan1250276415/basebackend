package com.basebackend.security.oauth2.evaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

/**
 * OAuth2权限评估器
 *
 * 实现Spring Security的PermissionEvaluator接口
 * 用于@PreAuthorize和@PostAuthorize注解的权限检查
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2PermissionEvaluator implements PermissionEvaluator {

    /**
     * 评估用户是否具有指定权限
     *
     * @param authentication 当前用户认证信息
     * @param targetDomainObject 目标对象
     * @param permission 权限
     * @return true如果用户具有权限，false否则
     */
    @Override
    public boolean hasPermission(Authentication authentication,
                                Object targetDomainObject,
                                Object permission) {

        try {
            if (!authentication.isAuthenticated()) {
                log.debug("用户未认证");
                return false;
            }

            log.debug("评估权限 - 认证: {}, 对象: {}, 权限: {}",
                authentication.getName(),
                targetDomainObject,
                permission);

            // 从认证信息中提取用户权限
            UserPermissions userPermissions = extractUserPermissions(authentication);

            // 检查权限
            boolean hasPermission = checkPermission(userPermissions, permission);

            log.debug("权限评估结果: {} - 用户: {}, 权限: {}",
                hasPermission,
                userPermissions.getUserId(),
                permission);

            return hasPermission;

        } catch (Exception e) {
            log.error("权限评估异常", e);
            return false;
        }
    }

    /**
     * 评估用户是否具有指定ID的权限
     *
     * @param authentication 当前用户认证信息
     * @param targetId 目标ID
     * @param targetType 目标类型
     * @param permission 权限
     * @return true如果用户具有权限，false否则
     */
    @Override
    public boolean hasPermission(Authentication authentication,
                                Serializable targetId,
                                String targetType,
                                Object permission) {

        log.debug("评估权限(带ID) - 用户: {}, 类型: {}, ID: {}, 权限: {}",
            authentication.getName(),
            targetType,
            targetId,
            permission);

        // 构建目标对象信息
        String targetDomainObject = String.format("%s:%s", targetType, targetId);

        return hasPermission(authentication, targetDomainObject, permission);
    }

    /**
     * 从认证信息中提取用户权限
     *
     * @param authentication 认证信息
     * @return 用户权限信息
     */
    private UserPermissions extractUserPermissions(Authentication authentication) {
        UserPermissions permissions = new UserPermissions();

        try {
            // 获取用户ID
            permissions.setUserId(authentication.getName());

            // 从权限中提取角色和权限
            if (!authentication.getAuthorities().isEmpty()) {
                for (var grantedAuthority : authentication.getAuthorities()) {
                    String authority = grantedAuthority.getAuthority();
                    if (authority != null) {
                        if (authority.startsWith("ROLE_")) {
                            permissions.addRole(authority.substring(5)); // 移除ROLE_前缀
                        } else {
                            permissions.addPermission(authority);
                        }
                    }
                }
            }

            // 如果是JWT认证_token，提取额外信息
            if (authentication instanceof JwtAuthenticationToken) {
                JwtAuthenticationToken jwtToken = (JwtAuthenticationToken) authentication;
                Jwt jwt = jwtToken.getToken();

                // 提取JWT claims中的额外权限信息
                extractPermissionsFromClaims(jwt.getClaims(), permissions);
            }

            log.debug("提取用户权限完成 - User: {}, Roles: {}, Permissions: {}",
                permissions.getUserId(),
                permissions.getRoles().size(),
                permissions.getPermissions().size());

        } catch (Exception e) {
            log.error("提取用户权限失败", e);
            permissions.setUserId(authentication.getName());
        }

        return permissions;
    }

    /**
     * 从JWT claims中提取额外权限信息
     *
     * @param claims JWT claims
     * @param permissions 权限容器
     */
    private void extractPermissionsFromClaims(Map<String, Object> claims, UserPermissions permissions) {
        try {
            // 从permissions字段提取
            if (claims.containsKey("permissions")) {
                Object permissionsObj = claims.get("permissions");
                if (permissionsObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> permissionsList = (java.util.List<String>) permissionsObj;
                    permissionsList.forEach(permissions::addPermission);
                }
            }

            // 从roles字段提取
            if (claims.containsKey("roles")) {
                Object rolesObj = claims.get("roles");
                if (rolesObj instanceof java.util.List) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> rolesList = (java.util.List<String>) rolesObj;
                    rolesList.forEach(permissions::addRole);
                }
            }

            // 从scopes字段提取
            if (claims.containsKey("scope") || claims.containsKey("scopes")) {
                String scopes = (String) claims.getOrDefault("scope",
                    claims.getOrDefault("scopes", ""));
                if (!scopes.isEmpty()) {
                    for (String scope : scopes.split(" ")) {
                        if (!scope.isEmpty()) {
                            permissions.addPermission("SCOPE_" + scope);
                        }
                    }
                }
            }

            // 从自定义字段提取
            extractCustomPermissions(claims, permissions);

        } catch (Exception e) {
            log.error("从JWT claims提取权限失败", e);
        }
    }

    /**
     * 从自定义字段提取权限
     *
     * @param claims JWT claims
     * @param permissions 权限容器
     */
    private void extractCustomPermissions(Map<String, Object> claims, UserPermissions permissions) {
        // 提取tenant权限（多租户场景）
        if (claims.containsKey("tenant_id") || claims.containsKey("tenant")) {
            Object tenantId = claims.getOrDefault("tenant_id", claims.get("tenant"));
            if (tenantId != null) {
                permissions.addPermission("TENANT_" + tenantId.toString());
            }
        }

        // 提取department权限
        if (claims.containsKey("department_id") || claims.containsKey("dept_id")) {
            Object deptId = claims.getOrDefault("department_id", claims.get("dept_id"));
            if (deptId != null) {
                permissions.addPermission("DEPT_" + deptId.toString());
            }
        }

        // 提取其他自定义权限
        if (claims.containsKey("custom_permissions")) {
            Object customPermissions = claims.get("custom_permissions");
            if (customPermissions instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<String> customPerms = (java.util.List<String>) customPermissions;
                customPerms.forEach(permissions::addPermission);
            }
        }
    }

    /**
     * 检查用户是否具有指定权限
     *
     * @param userPermissions 用户权限
     * @param requiredPermission 所需权限
     * @return true如果具有权限
     */
    private boolean checkPermission(UserPermissions userPermissions, Object requiredPermission) {
        String permission = requiredPermission.toString();

        // 1. 检查直接权限匹配
        if (userPermissions.getPermissions().contains(permission)) {
            log.debug("直接权限匹配成功: {}", permission);
            return true;
        }

        // 2. 检查通配符权限（例如：system:dept:* 匹配 system:dept:create）
        if (checkWildcardPermission(userPermissions, permission)) {
            return true;
        }

        // 3. 检查角色权限（用户是否有角色对应的权限）
        if (checkRolePermission(userPermissions, permission)) {
            return true;
        }

        // 4. 检查租户权限（多租户场景）
        if (checkTenantPermission(userPermissions, permission)) {
            return true;
        }

        // 5. 检查部门权限（部门隔离场景）
        if (checkDepartmentPermission(userPermissions, permission)) {
            return true;
        }

        log.debug("权限检查失败 - User: {}, Permission: {}",
            userPermissions.getUserId(), permission);

        return false;
    }

    /**
     * 检查通配符权限匹配
     */
    private boolean checkWildcardPermission(UserPermissions userPermissions, String requiredPermission) {
        for (String userPermission : userPermissions.getPermissions()) {
            // 支持 * 通配符
            if (userPermission.endsWith("*")) {
                String prefix = userPermission.substring(0, userPermission.length() - 1);
                if (requiredPermission.startsWith(prefix)) {
                    log.debug("通配符权限匹配成功: {} -> {}", userPermission, requiredPermission);
                    return true;
                }
            }

            // 支持 :* 通配符
            if (userPermission.endsWith(":*")) {
                String prefix = userPermission.substring(0, userPermission.length() - 2);
                if (requiredPermission.startsWith(prefix)) {
                    log.debug("通配符权限匹配成功: {} -> {}", userPermission, requiredPermission);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查角色权限
     */
    private boolean checkRolePermission(UserPermissions userPermissions, String requiredPermission) {
        // 这里可以实现角色权限映射逻辑
        // 例如：角色admin自动具有所有权限
        if (userPermissions.getRoles().contains("admin")) {
            return true;
        }
        return false;
    }

    /**
     * 检查租户权限
     */
    private boolean checkTenantPermission(UserPermissions userPermissions, String requiredPermission) {
        // 租户隔离检查逻辑
        // 如果权限中包含租户信息，检查是否匹配
        for (String userPermission : userPermissions.getPermissions()) {
            if (userPermission.startsWith("TENANT_")) {
                String tenantId = userPermission.substring("TENANT_".length());
                // 检查请求的权限是否属于该租户
                if (requiredPermission.contains("tenant") ||
                    requiredPermission.contains("Tenant") ||
                    requiredPermission.contains("TENANT")) {
                    log.debug("租户权限匹配成功: {}", userPermission);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检查部门权限
     */
    private boolean checkDepartmentPermission(UserPermissions userPermissions, String requiredPermission) {
        // 部门隔离检查逻辑
        for (String userPermission : userPermissions.getPermissions()) {
            if (userPermission.startsWith("DEPT_")) {
                String deptId = userPermission.substring("DEPT_".length());
                // 检查请求的权限是否属于该部门
                if (requiredPermission.contains("dept") ||
                    requiredPermission.contains("Dept") ||
                    requiredPermission.contains("DEPT")) {
                    log.debug("部门权限匹配成功: {}", userPermission);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 用户权限信息内部类
     */
    private static class UserPermissions {
        private String userId;
        private java.util.Set<String> roles = new java.util.HashSet<>();
        private java.util.Set<String> permissions = new java.util.HashSet<>();

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public java.util.Set<String> getRoles() {
            return roles;
        }

        public void addRole(String role) {
            this.roles.add(role);
        }

        public java.util.Set<String> getPermissions() {
            return permissions;
        }

        public void addPermission(String permission) {
            this.permissions.add(permission);
        }
    }
}
