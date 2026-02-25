package com.basebackend.api.model.user;

import java.util.List;

/**
 * 登录响应DTO
 */
public record LoginResponse(

        /**
         * 访问令牌
         */
        String accessToken,

        /**
         * 令牌类型
         */
        String tokenType,

        /**
         * 过期时间（秒）
         */
        Long expiresIn,

        /**
         * 用户信息
         */
        UserInfo userInfo,

        /**
         * 权限列表
         */
        List<String> permissions,

        /**
         * 角色列表
         */
        List<String> roles

) {

    /**
     * 便捷构造方法，默认 tokenType 为 "Bearer"
     */
    public LoginResponse(String accessToken, Long expiresIn, UserInfo userInfo,
                          List<String> permissions, List<String> roles) {
        this(accessToken, "Bearer", expiresIn, userInfo, permissions, roles);
    }

    /**
     * 用户信息内部记录
     */
    public record UserInfo(
            Long userId,
            String username,
            String nickname,
            String email,
            String phone,
            String avatar,
            Integer gender,
            Long deptId,
            String deptName,
            Integer userType,
            Integer status
    ) {
    }
}
