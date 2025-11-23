package com.basebackend.common.starter.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BaseBackend Common 模块统一配置属性
 * <p>
 * 集中管理通用模块的所有可配置项，支持 IDE 自动提示和校验。
 * </p>
 *
 * <h3>配置示例：</h3>
 * <pre>
 * basebackend:
 *   common:
 *     # 是否启用通用模块（默认 true）
 *     enabled: true
 *     exception:
 *       # 是否启用全局异常处理（默认 true）
 *       enabled: true
 *       # 是否在响应中包含异常堆栈（默认 false，生产环境建议关闭）
 *       include-stack-trace: false
 *     jackson:
 *       # 是否启用 Jackson 自动配置（默认 true）
 *       enabled: true
 *       # 日期时间格式（默认 yyyy-MM-dd HH:mm:ss）
 *       date-format: yyyy-MM-dd HH:mm:ss
 *       # 时区（默认 GMT+8）
 *       time-zone: GMT+8
 *     context:
 *       # 是否启用上下文自动清理（默认 true）
 *       auto-cleanup: true
 * </pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "basebackend.common")
public class CommonProperties {

    /**
     * 是否启用通用模块
     * <p>
     * 设置为 false 将禁用所有自动配置。
     * </p>
     */
    private Boolean enabled = true;

    /**
     * 异常处理配置
     */
    private ExceptionProperties exception = new ExceptionProperties();

    /**
     * Jackson 配置
     */
    private JacksonProperties jackson = new JacksonProperties();

    /**
     * 上下文配置
     */
    private ContextProperties context = new ContextProperties();

    // ========== 内部配置类 ==========

    /**
     * 异常处理配置
     */
    @Data
    public static class ExceptionProperties {

        /**
         * 是否启用全局异常处理
         */
        private Boolean enabled = true;

        /**
         * 是否在响应中包含异常堆栈
         * <p>
         * 生产环境建议设置为 false，避免泄露系统内部信息。
         * </p>
         */
        private Boolean includeStackTrace = false;

        /**
         * 是否记录异常日志
         */
        private Boolean logEnabled = true;

        /**
         * 是否记录请求信息（URI、参数等）
         */
        private Boolean logRequestInfo = true;
    }

    /**
     * Jackson 配置
     */
    @Data
    public static class JacksonProperties {

        /**
         * 是否启用 Jackson 自动配置
         */
        private Boolean enabled = true;

        /**
         * 日期时间格式
         */
        private String dateFormat = "yyyy-MM-dd HH:mm:ss";

        /**
         * 时区
         */
        private String timeZone = "GMT+8";

        /**
         * 序列化时是否包含 null 值字段
         */
        private Boolean includeNulls = false;

        /**
         * 是否启用驼峰命名转下划线
         */
        private Boolean snakeCaseEnabled = false;

        /**
         * 是否启用 FAIL_ON_UNKNOWN_PROPERTIES
         * <p>
         * 设置为 false 时，遇到未知属性不会抛出异常。
         * </p>
         */
        private Boolean failOnUnknownProperties = false;
    }

    /**
     * 上下文配置
     */
    @Data
    public static class ContextProperties {

        /**
         * 是否启用上下文自动清理
         * <p>
         * 在请求结束时自动清除 UserContext 和 TenantContext。
         * </p>
         */
        private Boolean autoCleanup = true;

        /**
         * 上下文清理过滤器的执行顺序
         */
        private Integer filterOrder = Integer.MIN_VALUE + 100;
    }
}
