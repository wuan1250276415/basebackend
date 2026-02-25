package com.basebackend.feign.dto.user;

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
            /**
             * 用户ID
             */
            Long userId,

            /**
             * 用户名
             */
            String username,

            /**
             * 昵称
             */
            String nickname,

            /**
             * 邮箱
             */
            String email,

            /**
             * 手机号
             */
            String phone,

            /**
             * 头像
             */
            String avatar,

            /**
             * 性别
             */
            Integer gender,

            /**
             * 部门ID
             */
            Long deptId,

            /**
             * 部门名称
             */
            String deptName,

            /**
             * 用户类型
             */
            Integer userType,

            /**
             * 状态
             */
            Integer status
    ) {
    }
}
