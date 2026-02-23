package com.basebackend.jwt;

import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JWT用户详情
 * <p>
 * 从JWT Token中解析的用户信息，用于避免频繁Feign调用。
 * 在JwtAuthenticationFilter中解析Token后设置到Authentication的principal。
 *
 * @author BaseBackend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserDetails implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 多租户ID
     */
    private String tenantId;

    /**
     * 设备标识
     */
    private String deviceId;

    /**
     * 获取用户ID（兼容方法）
     */
    public Long getId() {
        return userId;
    }

    /**
     * 从 JWT Claims 构建 JwtUserDetails
     *
     * @param claims JWT 解析后的 Claims
     * @return JwtUserDetails 实例
     */
    @SuppressWarnings("unchecked")
    public static JwtUserDetails fromClaims(Claims claims) {
        JwtUserDetailsBuilder builder = JwtUserDetails.builder()
                .username(claims.getSubject());

        // userId
        Object userIdObj = claims.get("userId");
        if (userIdObj instanceof Integer intVal) {
            builder.userId(intVal.longValue());
        } else if (userIdObj instanceof Long longVal) {
            builder.userId(longVal);
        } else if (userIdObj instanceof Number numVal) {
            builder.userId(numVal.longValue());
        }

        // deptId
        Object deptIdObj = claims.get("deptId");
        if (deptIdObj instanceof Integer intVal) {
            builder.deptId(intVal.longValue());
        } else if (deptIdObj instanceof Long longVal) {
            builder.deptId(longVal);
        } else if (deptIdObj instanceof Number numVal) {
            builder.deptId(numVal.longValue());
        }

        // roles
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> rawList) {
            List<String> roles = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            builder.roles(roles);
        }

        // permissions
        Object permissionsObj = claims.get("permissions");
        if (permissionsObj instanceof List<?> rawList) {
            List<String> permissions = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) {
                    permissions.add(item.toString());
                }
            }
            builder.permissions(permissions);
        }

        // tenantId
        Object tenantIdObj = claims.get("tenantId");
        if (tenantIdObj instanceof String strVal) {
            builder.tenantId(strVal);
        }

        // deviceId
        Object deviceIdObj = claims.get("deviceId");
        if (deviceIdObj instanceof String strVal) {
            builder.deviceId(strVal);
        }

        return builder.build();
    }

    /**
     * 返回不可变的角色列表
     */
    public List<String> getRoles() {
        return roles != null ? Collections.unmodifiableList(roles) : Collections.emptyList();
    }

    /**
     * 返回不可变的权限列表
     */
    public List<String> getPermissions() {
        return permissions != null ? Collections.unmodifiableList(permissions) : Collections.emptyList();
    }
}
