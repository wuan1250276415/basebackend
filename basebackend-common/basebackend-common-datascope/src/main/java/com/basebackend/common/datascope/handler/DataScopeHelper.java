package com.basebackend.common.datascope.handler;

import com.basebackend.common.datascope.enums.DataScopeType;

import java.util.Set;

/**
 * 数据权限辅助工具类
 * <p>
 * 提供静态方法，支持手动开启/关闭数据权限过滤，
 * 适用于不使用 {@link com.basebackend.common.datascope.annotation.DataScope} 注解的场景。
 * </p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 手动开启数据权限过滤
 * DataScopeHelper.startScope(DataScopeType.DEPT, "d", "dept_id", "u", "create_by");
 * try {
 *     userMapper.selectList(queryWrapper);
 * } finally {
 *     DataScopeHelper.clearScope();
 * }
 * }</pre>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class DataScopeHelper {

    private DataScopeHelper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 手动开启数据权限过滤（使用默认别名和字段名）
     *
     * @param type 数据范围类型
     */
    public static void startScope(DataScopeType type) {
        startScope(type, "d", "dept_id", "u", "create_by");
    }

    /**
     * 手动开启数据权限过滤（自定义别名和字段名）
     *
     * @param type      数据范围类型
     * @param deptAlias 部门表别名
     * @param deptField 部门ID字段名
     * @param userAlias 用户表别名
     * @param userField 创建者字段名
     */
    public static void startScope(DataScopeType type, String deptAlias, String deptField,
                                  String userAlias, String userField) {
        String condition = DataScopeSqlBuilder.buildCondition(
                type, deptAlias, deptField, userAlias, userField, null
        );
        if (condition != null && !condition.isEmpty()) {
            com.basebackend.common.datascope.context.DataScopeContext.set(condition);
        }
    }

    /**
     * 清除数据权限过滤
     */
    public static void clearScope() {
        com.basebackend.common.datascope.context.DataScopeContext.clear();
    }
}
