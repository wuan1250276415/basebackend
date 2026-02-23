package com.basebackend.common.datascope.interceptor;

import com.basebackend.common.datascope.context.DataScopeContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;

/**
 * MyBatis 数据权限拦截器
 * <p>
 * 拦截 SQL 查询执行，在 WHERE 条件中追加数据权限过滤条件。
 * 仅当 {@link DataScopeContext} 中存在条件时才进行 SQL 改写。
 * </p>
 *
 * @author BaseBackend Team
 * @since 1.0.0
 */
@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class DataScopeInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String condition = DataScopeContext.get();
        if (condition == null || condition.isEmpty()) {
            return invocation.proceed();
        }

        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();

        // 在原始 SQL 的 WHERE 条件后追加数据权限条件
        String newSql = injectDataScopeCondition(originalSql, condition);
        log.debug("数据权限SQL改写: {} -> {}", originalSql, newSql);

        // 替换 BoundSql 中的 SQL
        setFieldValue(boundSql, "sql", newSql);

        // 重新构建 MappedStatement
        MappedStatement newMs = rebuildMappedStatement(ms, boundSql);
        args[0] = newMs;

        return invocation.proceed();
    }

    /**
     * 在 SQL 中注入数据权限条件
     * <p>
     * 策略：在 WHERE 子句后追加 AND 条件。
     * 如果原 SQL 没有 WHERE 子句，则添加 WHERE 条件。
     * </p>
     */
    private String injectDataScopeCondition(String originalSql, String condition) {
        String upperSql = originalSql.toUpperCase();

        // 查找最后一个 WHERE 关键字的位置（处理子查询中的 WHERE）
        int whereIndex = findMainWhereIndex(upperSql);

        if (whereIndex >= 0) {
            // 已有 WHERE 子句，在末尾追加 AND 条件
            return originalSql + " AND " + condition;
        } else {
            // 没有 WHERE 子句
            // 查找 ORDER BY / GROUP BY / LIMIT 的位置，在其前面插入 WHERE
            int insertIndex = findInsertIndex(upperSql);
            if (insertIndex >= 0) {
                return originalSql.substring(0, insertIndex)
                        + " WHERE " + condition + " "
                        + originalSql.substring(insertIndex);
            } else {
                return originalSql + " WHERE " + condition;
            }
        }
    }

    /**
     * 查找主查询的 WHERE 关键字位置（忽略子查询中的 WHERE）
     */
    private int findMainWhereIndex(String upperSql) {
        int depth = 0;
        int lastWhereIndex = -1;

        for (int i = 0; i < upperSql.length(); i++) {
            char c = upperSql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && i + 5 <= upperSql.length()
                    && upperSql.substring(i, i + 5).equals("WHERE")
                    && (i == 0 || !Character.isLetterOrDigit(upperSql.charAt(i - 1)))
                    && (i + 5 >= upperSql.length() || !Character.isLetterOrDigit(upperSql.charAt(i + 5)))) {
                lastWhereIndex = i;
            }
        }

        return lastWhereIndex;
    }

    /**
     * 查找 ORDER BY / GROUP BY / LIMIT 的位置
     */
    private int findInsertIndex(String upperSql) {
        String[] keywords = {"ORDER BY", "GROUP BY", "LIMIT", "HAVING"};
        int minIndex = -1;

        for (String keyword : keywords) {
            int index = upperSql.lastIndexOf(keyword);
            if (index >= 0 && (minIndex < 0 || index < minIndex)) {
                minIndex = index;
            }
        }

        return minIndex;
    }

    /**
     * 通过反射设置字段值
     */
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("反射设置字段值失败, field={}", fieldName, e);
        }
    }

    /**
     * 重新构建 MappedStatement（替换 SqlSource）
     */
    private MappedStatement rebuildMappedStatement(MappedStatement ms, BoundSql boundSql) {
        SqlSource newSqlSource = parameterObject -> boundSql;

        return new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType())
                .resource(ms.getResource())
                .parameterMap(ms.getParameterMap())
                .resultMaps(ms.getResultMaps())
                .fetchSize(ms.getFetchSize())
                .timeout(ms.getTimeout())
                .statementType(ms.getStatementType())
                .resultSetType(ms.getResultSetType())
                .cache(ms.getCache())
                .flushCacheRequired(ms.isFlushCacheRequired())
                .useCache(ms.isUseCache())
                .keyGenerator(ms.getKeyGenerator())
                .keyProperty(ms.getKeyProperties() != null ? String.join(",", ms.getKeyProperties()) : null)
                .keyColumn(ms.getKeyColumns() != null ? String.join(",", ms.getKeyColumns()) : null)
                .databaseId(ms.getDatabaseId())
                .lang(ms.getLang())
                .build();
    }
}
