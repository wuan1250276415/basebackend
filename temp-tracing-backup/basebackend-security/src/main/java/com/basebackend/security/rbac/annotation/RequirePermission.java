package com.basebackend.security.rbac.annotation;

import com.basebackend.security.rbac.PermissionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限要求注解
 * 用于标记需要特定权限才能访问的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限编码
     * 支持多个权限，用逗号分隔
     * 例如: "user:read,user:write"
     */
    String value() default "";

    /**
     * 权限类型
     */
    PermissionType type() default PermissionType.API;

    /**
     * 逻辑关系
     * ANY: 拥有任意一个权限即可
     * ALL: 必须拥有所有权限
     */
    Logic logic() default Logic.ANY;

    /**
     * 是否检查数据范围
     */
    boolean checkDataScope() default true;

    /**
     * 数据范围类型
     * 当checkDataScope为true时生效
     */
    DataScopeType dataScope() default DataScopeType.AUTO;

    /**
     * 自定义错误消息
     */
    String message() default "";

    /**
     * 自定义权限验证失败时的异常类型
     */
    Class<? extends Throwable> exception() default SecurityException.class;

    /**
     * 权限类型枚举
     */
    enum PermissionType {
        /**
         * API接口权限
         */
        API,
        /**
         * 菜单权限
         */
        MENU,
        /**
         * 按钮权限
         */
        BUTTON,
        /**
         * 数据权限
         */
        DATA,
        /**
         * 字段权限
         */
        FIELD
    }

    /**
     * 逻辑关系枚举
     */
    enum Logic {
        /**
         * 任意一个权限 (OR)
         */
        ANY,
        /**
         * 所有权限 (AND)
         */
        ALL
    }

    /**
     * 数据范围类型枚举
     */
    enum DataScopeType {
        /**
         * 自动判断 (根据用户角色自动确定)
         */
        AUTO,
        /**
         * 全部数据
         */
        ALL,
        /**
         * 本部门及以下数据
         */
        DEPT_AND_CHILD,
        /**
         * 本部门数据
         */
        DEPT,
        /**
         * 仅本人数据
         */
        SELF,
        /**
         * 自定义数据范围
         */
        CUSTOM
    }
}

/**
 * 权限上下文参数注解
 * 用于从方法参数中提取权限验证所需的上下文信息
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@interface PermissionContextParam {
    /**
     * 参数名
     */
    String value() default "context";

    /**
     * 用户ID参数名
     */
    String userId() default "userId";

    /**
     * 资源ID参数名
     */
    String resourceId() default "resourceId";

    /**
     * 资源Owner ID参数名
     */
    String resourceOwnerId() default "resourceOwnerId";

    /**
     * 部门ID参数名
     */
    String deptId() default "deptId";
}

/**
 * 数据范围验证注解
 * 用于标记需要数据范围验证的方法
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@RequirePermission
@DataScope
class DataScopeCheck {
}

/**
 * 数据范围注解
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface DataScope {
    /**
     * 数据范围类型
     */
    RequirePermission.DataScopeType type() default RequirePermission.DataScopeType.AUTO;

    /**
     * 资源Owner ID参数名
     */
    String ownerIdParam() default "ownerId";

    /**
     * 部门ID参数名
     */
    String deptIdParam() default "deptId";
}

/**
 * 角色要求注解
 * 用于标记需要特定角色才能访问的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface RequireRole {

    /**
     * 角色编码
     * 支持多个角色，用逗号分隔
     */
    String value() default "";

    /**
     * 逻辑关系
     */
    RequirePermission.Logic logic() default RequirePermission.Logic.ANY;

    /**
     * 是否必须为活跃角色
     */
    boolean activeOnly() default true;
}

/**
 * 用户ID注解
 * 用于自动注入当前用户ID
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@interface CurrentUser {
    /**
     * 用户ID字段名
     */
    String value() default "id";
}

/**
 * 资源Owner验证注解
 * 用于验证当前用户是否为资源Owner
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface RequireOwner {

    /**
     * 资源Owner ID参数名
     */
    String ownerIdParam() default "ownerId";

    /**
     * 允许Owner的角色编码
     * 如果当前用户拥有这些角色之一，也允许访问
     */
    String[] allowedRoles() default {};
}
