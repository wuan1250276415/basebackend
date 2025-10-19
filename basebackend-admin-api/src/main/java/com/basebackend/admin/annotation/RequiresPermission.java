package com.basebackend.admin.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限标识
     */
    String value();

    /**
     * 权限标识数组
     */
    String[] values() default {};

    /**
     * 逻辑关系：AND-所有权限都需要，OR-任一权限即可
     */
    Logical logical() default Logical.AND;

    /**
     * 逻辑关系枚举
     */
    enum Logical {
        AND, OR
    }
}
