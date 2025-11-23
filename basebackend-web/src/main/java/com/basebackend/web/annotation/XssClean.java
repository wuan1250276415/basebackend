package com.basebackend.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * XSS 清洗注解
 *
 * @author basebackend
 * @since 2025-11-23
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XssClean {

    /**
     * 是否开启XSS清洗
     */
    boolean enabled() default true;

    /**
     * 清洗策略
     */
    CleanStrategy strategy() default CleanStrategy.ESCAPE;

    /**
     * 允许的HTML标签
     */
    String[] allowedTags() default {};

    /**
     * 允许的属性
     */
    String[] allowedAttributes() default {};

    /**
     * 清理策略枚举
     */
    enum CleanStrategy {
        /**
         * 转义HTML实体
         */
        ESCAPE,
        /**
         * 完全移除脚本标签
         */
        REMOVE_SCRIPT,
        /**
         * 白名单过滤
         */
        WHITELIST_FILTER,
        /**
         * 黑名单过滤
         */
        BLACKLIST_FILTER
    }
}
