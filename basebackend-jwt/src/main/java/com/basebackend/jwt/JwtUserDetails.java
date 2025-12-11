package com.basebackend.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * JWT用户详情
 * <p>
 * 从JWT Token中解析的用户信息，用于避免频繁Feign调用
 * 在JwtAuthenticationFilter中解析Token后设置到Authentication的principal
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
     * 获取用户ID（兼容方法）
     */
    public Long getId() {
        return userId;
    }
}
