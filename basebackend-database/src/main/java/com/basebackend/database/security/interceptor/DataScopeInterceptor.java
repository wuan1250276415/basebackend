package com.basebackend.database.security.interceptor;

import com.basebackend.security.annotation.DataScope;
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
import java.util.HashMap;
import java.util.Map;

/**
 * 数据权限拦截器
 *
 * @author Claude Code (浮浮酱)
 * @since 2025-11-26
 */
@Slf4j
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataScopeInterceptor implements Interceptor {

    /**
     * 部门ID字段名映射
     */
    private static final Map<String, String> DEPT_COLUMN_MAP = new HashMap<>();

    static {
        // 常见部门字段名映射
        DEPT_COLUMN_MAP.put("sys_user", "dept_id");
        DEPT_COLUMN_MAP.put("sys_dept", "id");
        DEPT_COLUMN_MAP.put("sys_role", "dept_id");
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

        log.debug("原始SQL: {}", originalSql);

        try {
            // 生成数据权限过滤 SQL
            String filteredSql = addDataScopeFilter(originalSql, mappedStatement, boundSql);
            if (filteredSql != null) {
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
     * 添加数据权限过滤条件（在ORDER BY/LIMIT之前插入）
     */
    private String addDataScopeFilter(String sql, MappedStatement mappedStatement, BoundSql boundSql) {
        // 获取数据权限类型
        DataScopeType dataScopeType = DataScopeContextHolder.getDataScopeType();
        Long userId = DataScopeContextHolder.getUserId();
        Long deptId = DataScopeContextHolder.getDeptId();

        if (dataScopeType == DataScopeType.ALL) {
            // 全部数据权限，不需要过滤
            return sql;
        }

        // 根据数据权限类型生成过滤条件
        String whereCondition = generateWhereConditionWithParams(dataScopeType, userId, deptId, boundSql);
        if (whereCondition == null) {
            return sql;
        }

        // 检查是否已经包含 WHERE 子句
        boolean hasWhere = sql.toLowerCase().contains(" where ");

        // 查找 ORDER BY、LIMIT、OFFSET 等关键字的位置
        int insertPos = findInsertPosition(sql);

        StringBuilder filteredSql = new StringBuilder();

        if (hasWhere) {
            // 已有 WHERE 子句，追加 AND 条件
            filteredSql.append(sql, 0, insertPos)
                    .append(" AND ")
                    .append(whereCondition)
                    .append(sql.substring(insertPos));
        } else {
            // 没有 WHERE 子句，添加 WHERE 子句
            filteredSql.append(sql, 0, insertPos)
                    .append(" WHERE ")
                    .append(whereCondition)
                    .append(sql.substring(insertPos));
        }

        return filteredSql.toString();
    }

    /**
     * 查找SQL中WHERE条件的插入位置（在ORDER BY、LIMIT等之前）
     */
    private int findInsertPosition(String sql) {
        // 查找最后一个WHERE、ORDER BY、LIMIT、OFFSET、GROUP BY、HAVING的位置
        int[] keywords = {
                findKeyword(sql, " where "),
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
        String lowerSql = sql.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        return lowerSql.indexOf(lowerKeyword);
    }

    /**
     * 根据数据权限类型生成 WHERE 条件（安全的参数处理）
     */
    private String generateWhereConditionWithParams(DataScopeType dataScopeType, Long userId, Long deptId, BoundSql boundSql) {
        switch (dataScopeType) {
            case ALL:
                return null; // 不需要过滤

            case DEPT:
                // 本部门数据 - 直接使用参数值（已验证为Long类型）
                if (deptId != null) {
                    // 严格验证参数类型，防止注入
                    validateLongValue(deptId, "deptId");
                    return "dept_id = " + deptId;
                }
                return "1=0"; // 无法确定部门时返回空

            case DEPT_AND_CHILD:
                // 本部门及下级部门数据
                if (deptId != null) {
                    validateLongValue(deptId, "deptId");
                    return "(dept_id = " + deptId + " OR dept_id IN (SELECT id FROM sys_dept WHERE parent_id = " + deptId + "))";
                }
                return "1=0";

            case SELF:
                // 仅本人数据
                if (userId != null) {
                    validateLongValue(userId, "userId");
                    return "(user_id = " + userId + " OR create_by = " + userId + ")";
                }
                return "1=0";

            case CUSTOM:
                // 自定义数据权限，由调用方处理
                return "1=1";

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
        // Long类型的值天然安全，不会导致SQL注入
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        // 可以从配置文件读取属性
    }
}
