package com.basebackend.common.datascope.context;

/**
 * 数据权限上下文
 * <p>
 * 基于 ThreadLocal 存储当前查询的数据权限 SQL 片段。
 * 支持手动设置/清除数据权限条件。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
public final class DataScopeContext {

    private DataScopeContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    private static final ThreadLocal<String> SQL_CONDITION = new ThreadLocal<>();

    /**
     * 设置数据权限 SQL 条件片段
     *
     * @param sqlCondition SQL 条件片段（不含前导 AND）
     */
    public static void set(String sqlCondition) {
        SQL_CONDITION.set(sqlCondition);
    }

    /**
     * 获取数据权限 SQL 条件片段
     *
     * @return SQL 条件片段，可能为 null
     */
    public static String get() {
        return SQL_CONDITION.get();
    }

    /**
     * 清除数据权限 SQL 条件
     */
    public static void clear() {
        SQL_CONDITION.remove();
    }

    /**
     * 判断是否存在数据权限条件
     *
     * @return 是否已设置数据权限条件
     */
    public static boolean isPresent() {
        return SQL_CONDITION.get() != null;
    }
}
