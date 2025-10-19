package com.basebackend.admin.annotation;

import java.lang.annotation.*;

/**
 * 角色校验注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {

    /**
     * 角色标识
     */
    String value();

    /**
     * 角色标识数组
     */
    String[] values() default {};

    /**
     * 逻辑关系：AND-所有角色都需要，OR-任一角色即可
     */
    Logical logical() default Logical.OR;

    /**
     * 逻辑关系枚举
     */
    enum Logical {
        AND, OR
    }
}
