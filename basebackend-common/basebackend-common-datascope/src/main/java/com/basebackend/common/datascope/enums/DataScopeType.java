package com.basebackend.common.datascope.enums;

/**
 * 数据范围类型枚举
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public enum DataScopeType {

    /**
     * 全部数据权限
     */
    ALL,

    /**
     * 本部门数据
     */
    DEPT,

    /**
     * 本部门及以下
     */
    DEPT_AND_BELOW,

    /**
     * 仅本人数据
     */
    SELF,

    /**
     * 自定义（基于角色配置的部门集合）
     */
    CUSTOM,

    /**
     * 自动（根据当前用户角色的数据范围设置决定）
     */
    AUTO
}
