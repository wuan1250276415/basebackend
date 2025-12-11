package com.basebackend.database.security.interceptor;

import com.basebackend.security.context.DataScopeContextHolder;
import com.basebackend.security.enums.DataScopeType;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据权限拦截器
 * <p>
 * 智能检测表结构，只对配置了部门字段的表应用数据权限过滤。
 * 未在白名单中的表不会被添加任何数据权限过滤条件。
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class, Integer.class })
})
public class DataScopeInterceptor implements Interceptor {

    /**
     * 需要进行数据权限过滤的表（这些表有dept_id字段）
     * 只有在此集合中的表才会被添加数据权限过滤条件
     */
    private static final Set<String> DEPT_SCOPED_TABLES = new HashSet<>();

    /**
     * 需要进行创建者过滤的表（这些表有create_by字段）
     */
    private static final Set<String> CREATOR_SCOPED_TABLES = new HashSet<>();

    /**
     * 用于从SQL中提取表名的正则表达式
     */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile(
            "(?i)\\bFROM\\s+([`\\[\"']?\\w+[`\\]\"']?)(?:\\s+(?:AS\\s+)?\\w+)?",
            Pattern.CASE_INSENSITIVE);

    static {
        // ==================== 配置需要数据权限过滤的表 ====================
        // 只有这些表会被添加 dept_id 相关的过滤条件
        // 如果新表需要数据权限，在这里添加表名

        // 系统核心表（有dept_id字段）
        DEPT_SCOPED_TABLES.add("sys_user");
        // 如有其他表有dept_id字段，在此添加
        // DEPT_SCOPED_TABLES.add("other_table");

        // ==================== 配置需要创建者过滤的表 ====================
        // 这些表会被添加 create_by 相关的过滤条件（用于SELF权限类型）
        CREATOR_SCOPED_TABLES.add("sys_user");
        // 可根据实际业务添加更多表
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 先解开可能存在的代理，避免无法访问真实属性
        StatementHandler statementHandler = (StatementHandler) PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);

        // 兼容不同的 StatementHandler 实现，优先访问 delegate.mappedStatement
        MappedStatement mappedStatement = null;
        if (metaObject.hasGetter("delegate.mappedStatement")) {
            mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        } else if (metaObject.hasGetter("mappedStatement")) {
            mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");
        } else {
            return invocation.proceed();
        }

        // 检查是否是查询操作
        if (mappedStatement.getSqlCommandType() != SqlCommandType.SELECT) {
            return invocation.proceed();
        }

        // 检查是否设置了数据权限上下文
        if (!DataScopeContextHolder.isSet()) {
            return invocation.proceed();
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String originalSql = boundSql.getSql();

        // 从SQL中提取表名
        String tableName = extractTableName(originalSql);
        if (tableName == null) {
            log.debug("无法从SQL中提取表名，跳过数据权限过滤");
            return invocation.proceed();
        }

        // 检查该表是否需要数据权限过滤
        DataScopeType dataScopeType = DataScopeContextHolder.getDataScopeType();
        boolean needDeptFilter = needsDeptScopeFilter(tableName, dataScopeType);
        boolean needCreatorFilter = needsCreatorScopeFilter(tableName, dataScopeType);

        if (!needDeptFilter && !needCreatorFilter) {
            log.debug("表 {} 不需要数据权限过滤，跳过", tableName);
            return invocation.proceed();
        }

        log.debug("原始SQL: {}", originalSql);

        try {
            // 生成数据权限过滤 SQL
            String filteredSql = addDataScopeFilter(originalSql, needDeptFilter, needCreatorFilter);
            if (filteredSql != null && !filteredSql.equals(originalSql)) {
                // 重写 SQL
                if (metaObject.hasGetter("delegate.boundSql.sql")) {
                    metaObject.setValue("delegate.boundSql.sql", filteredSql);
                } else if (metaObject.hasGetter("boundSql.sql")) {
                    metaObject.setValue("boundSql.sql", filteredSql);
                }
                log.debug("过滤后SQL: {}", filteredSql);
            }
        } catch (Exception e) {
            log.error("数据权限过滤失败", e);
            // 失败时继续执行原SQL
        }

        return invocation.proceed();
    }

    /**
     * 从SQL中提取主表名
     */
    private String extractTableName(String sql) {
        Matcher matcher = TABLE_NAME_PATTERN.matcher(sql);
        if (matcher.find()) {
            String tableName = matcher.group(1);
            // 移除可能的引号
            return tableName.replaceAll("[`\\[\\]\"']", "").toLowerCase();
        }
        return null;
    }

    /**
     * 检查表是否需要部门数据权限过滤
     */
    private boolean needsDeptScopeFilter(String tableName, DataScopeType dataScopeType) {
        if (dataScopeType == DataScopeType.ALL || dataScopeType == DataScopeType.SELF) {
            return false;
        }
        return DEPT_SCOPED_TABLES.contains(tableName.toLowerCase());
    }

    /**
     * 检查表是否需要创建者数据权限过滤
     */
    private boolean needsCreatorScopeFilter(String tableName, DataScopeType dataScopeType) {
        if (dataScopeType != DataScopeType.SELF) {
            return false;
        }
        return CREATOR_SCOPED_TABLES.contains(tableName.toLowerCase());
    }

    /**
     * 添加数据权限过滤条件
     */
    private String addDataScopeFilter(String sql, boolean needDeptFilter, boolean needCreatorFilter) {
        DataScopeType dataScopeType = DataScopeContextHolder.getDataScopeType();
        Long userId = DataScopeContextHolder.getUserId();
        Long deptId = DataScopeContextHolder.getDeptId();

        if (dataScopeType == DataScopeType.ALL) {
            return sql;
        }

        // 生成过滤条件
        String whereCondition = generateWhereCondition(dataScopeType, userId, deptId, needDeptFilter,
                needCreatorFilter);
        if (whereCondition == null) {
            return sql;
        }

        // 检查是否已经包含 WHERE 子句
        boolean hasWhere = sql.toLowerCase().contains(" where ");

        // 查找 ORDER BY、LIMIT、OFFSET 等关键字的位置
        int insertPos = findInsertPosition(sql);

        StringBuilder filteredSql = new StringBuilder();

        if (hasWhere) {
            filteredSql.append(sql, 0, insertPos)
                    .append(" AND ")
                    .append(whereCondition)
                    .append(sql.substring(insertPos));
        } else {
            filteredSql.append(sql, 0, insertPos)
                    .append(" WHERE ")
                    .append(whereCondition)
                    .append(sql.substring(insertPos));
        }

        return filteredSql.toString();
    }

    /**
     * 查找SQL中WHERE条件的插入位置
     */
    private int findInsertPosition(String sql) {
        int[] keywords = {
                findKeyword(sql, " order by "),
                findKeyword(sql, " limit "),
                findKeyword(sql, " offset "),
                findKeyword(sql, " group by "),
                findKeyword(sql, " having ")
        };

        int insertPos = sql.length();
        for (int pos : keywords) {
            if (pos > 0 && pos < insertPos) {
                insertPos = pos;
            }
        }

        return insertPos;
    }

    /**
     * 在SQL中查找关键字（不区分大小写）
     */
    private int findKeyword(String sql, String keyword) {
        return sql.toLowerCase().indexOf(keyword.toLowerCase());
    }

    /**
     * 根据数据权限类型生成 WHERE 条件
     */
    private String generateWhereCondition(DataScopeType dataScopeType, Long userId, Long deptId,
            boolean needDeptFilter, boolean needCreatorFilter) {
        switch (dataScopeType) {
            case ALL:
                return null;

            case DEPT:
                if (needDeptFilter && deptId != null) {
                    validateLongValue(deptId, "deptId");
                    return "dept_id = " + deptId;
                }
                return null; // 表没有dept_id字段，不添加过滤条件

            case DEPT_AND_CHILD:
                if (needDeptFilter && deptId != null) {
                    validateLongValue(deptId, "deptId");
                    return "(dept_id = " + deptId + " OR dept_id IN (SELECT id FROM sys_dept WHERE parent_id = "
                            + deptId + "))";
                }
                return null;

            case SELF:
                if (needCreatorFilter && userId != null) {
                    validateLongValue(userId, "userId");
                    return "(create_by = " + userId + ")";
                }
                return null;

            case CUSTOM:
                // 自定义数据权限，由调用方处理
                return null;

            default:
                return null;
        }
    }

    /**
     * 验证Long类型参数值，确保安全
     */
    private void validateLongValue(Long value, String paramName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("无效的" + paramName + "值: " + value);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        // 可以从配置文件读取属性
    }

    /**
     * 动态添加需要数据权限过滤的表
     * 可在应用启动时调用此方法配置
     *
     * @param tableName   表名
     * @param hasDeptId   是否有dept_id字段
     * @param hasCreateBy 是否有create_by字段
     */
    public static void registerTable(String tableName, boolean hasDeptId, boolean hasCreateBy) {
        if (hasDeptId) {
            DEPT_SCOPED_TABLES.add(tableName.toLowerCase());
        }
        if (hasCreateBy) {
            CREATOR_SCOPED_TABLES.add(tableName.toLowerCase());
        }
        log.info("注册数据权限表: {} (dept_id={}, create_by={})", tableName, hasDeptId, hasCreateBy);
    }
}
