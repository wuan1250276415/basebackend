package com.basebackend.security.enums;

/**
 * 数据权限类型枚举
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
public enum DataScopeType {

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
    SELF,

    /**
     * 自定义数据权限
     */
    CUSTOM
}
