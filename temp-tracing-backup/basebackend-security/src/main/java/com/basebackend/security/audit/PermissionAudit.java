package com.basebackend.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限审计注解
 * 用于标记需要审计的权限操作
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionAudit {

    /**
     * 操作类型 (GRANT, REVOKE, MODIFY, QUERY)
     */
    String action() default "";

    /**
     * 目标用户或角色
     */
    String target() default "";

    /**
     * 权限名称
     */
    String permission() default "";

    /**
     * 操作描述
     */
    String description() default "";
}
