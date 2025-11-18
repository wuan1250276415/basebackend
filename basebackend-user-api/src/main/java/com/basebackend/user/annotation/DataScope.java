package com.basebackend.user.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据权限范围
     */
    DataScopeType value() default DataScopeType.ALL;

    /**
     * 数据权限类型枚举
     */
    enum DataScopeType {
        /**
         * 全部数据权限
         */
        ALL,
        /**
         * 本部门数据权限
         */
        DEPT,
        /**
         * 本部门及以下数据权限
         */
        DEPT_AND_CHILD,
        /**
         * 仅本人数据权限
         */
        SELF
    }
}
