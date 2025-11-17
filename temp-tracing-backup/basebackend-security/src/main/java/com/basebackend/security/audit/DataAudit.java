package com.basebackend.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据审计注解
 * 用于标记需要审计的数据库操作
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAudit {

    /**
     * 操作类型 (INSERT, UPDATE, DELETE, SELECT)
     */
    String operation() default "";

    /**
     * 表名
     */
    String table() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
