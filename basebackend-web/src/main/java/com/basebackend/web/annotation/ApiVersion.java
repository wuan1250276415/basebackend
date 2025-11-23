package com.basebackend.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 版本控制注解
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    /**
     * API 版本号（支持v1, v2等）
     * 默认v1
     */
    String value() default "v1";

    /**
     * 兼容的版本列表
     */
    String[] compatible() default {};

    /**
     * 最低支持版本
     */
    String minVersion() default "";

    /**
     * 最高支持版本
     */
    String maxVersion() default "";

    /**
     * 是否废弃（deprecated）
     */
    boolean deprecated() default false;

    /**
     * 版本描述
     */
    String description() default "";

    /**
     * 废弃时间
     */
    String deprecatedDate() default "";

    /**
     * 迁移指南
     */
    String migrationGuide() default "";
}
