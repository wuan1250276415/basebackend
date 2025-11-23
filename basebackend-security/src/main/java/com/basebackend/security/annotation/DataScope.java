package com.basebackend.security.annotation;

import com.basebackend.security.enums.DataScopeType;

import java.lang.annotation.*;

/**
 * 数据权限注解
 * 用于控制用户可以访问的数据范围
 * 使用示例：
 * &#064;DataScope(DataScopeType.DEPT)
 * &#064;DataScope(DataScopeType.DEPT_AND_CHILD)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据权限范围
     */
    DataScopeType value() default DataScopeType.ALL;
}
