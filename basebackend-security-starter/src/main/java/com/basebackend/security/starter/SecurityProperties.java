package com.basebackend.security.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 安全配置属性
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Data
@ConfigurationProperties(prefix = "basebackend.security")
public class SecurityProperties {

    /**
     * 是否启用安全控制
     */
    private boolean enabled = true;

    /**
     * 权限控制配置
     */
    private Permission permission = new Permission();

    /**
     * JWT 配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 数据权限配置
     */
    private DataScope dataScope = new DataScope();

    /**
     * 审计配置
     */
    private Audit audit = new Audit();

    @Data
    public static class Permission {
        /**
         * 是否启用权限注解
         */
        private boolean enabled = true;

        /**
         * 当未实现 PermissionService 时是否失败
         */
        private boolean failWhenMissing = true;

        /**
         * 默认权限列表
         */
        private List<String> defaults;
    }

    @Data
    public static class Jwt {
        /**
         * 是否启用 JWT 验证
         */
        private boolean enabled = true;

        /**
         * Token 头部名称
         */
        private String headerName = "Authorization";

        /**
         * Token 前缀
         */
        private String prefix = "Bearer ";

        /**
         * 是否从请求参数中获取 Token
         */
        private boolean allowFromRequestParameter = false;
    }

    @Data
    public static class DataScope {
        /**
         * 是否启用数据权限
         */
        private boolean enabled = true;

        /**
         * 默认数据权限类型
         */
        private String defaultType = "ALL";
    }

    @Data
    public static class Audit {
        /**
         * 是否启用审计日志
         */
        private boolean enabled = true;

        /**
         * 是否记录成功操作
         */
        private boolean logSuccess = false;

        /**
         * 是否记录失败操作
         */
        private boolean logFailure = true;
    }
}
