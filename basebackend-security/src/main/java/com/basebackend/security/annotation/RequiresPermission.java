package com.basebackend.security.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 
 * 使用示例：
 * @RequiresPermission("system:user:list")
 * @RequiresPermission(values = {"system:user:add", "system:user:edit"}, logical = Logical.OR)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * 权限标识
     */
    String value() default "";

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
        /**
         * 所有权限都需要满足
         */
        AND,
        /**
         * 任一权限满足即可
         */
        OR
    }
}
