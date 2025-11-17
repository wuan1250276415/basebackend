package com.basebackend.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 安全审计注解
 * 用于标记需要审计的敏感操作
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * 审计操作名称
     */
    String value() default "";

    /**
     * 审计资源名称
     */
    String resource() default "";

    /**
     * 审计描述
     */
    String description() default "";
}
