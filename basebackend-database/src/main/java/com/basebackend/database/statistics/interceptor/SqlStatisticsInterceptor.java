package com.basebackend.database.statistics.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.statistics.collector.SqlStatisticsCollector;
import com.basebackend.database.statistics.model.SqlExecutionInfo;
import com.basebackend.database.statistics.util.SqlTemplateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.time.LocalDateTime;
import java.util.Properties;

/**
 * SQL统计拦截器
 * 拦截SQL执行，收集执行统计信息
 * Note: This is registered as a bean in MyBatisPlusConfig, not auto-scanned
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SqlStatisticsInterceptor implements Interceptor {

    private final SqlStatisticsCollector statisticsCollector;
    private final DatabaseEnhancedProperties properties;

    public SqlStatisticsInterceptor(SqlStatisticsCollector statisticsCollector,
                                   DatabaseEnhancedProperties properties) {
        this.statisticsCollector = statisticsCollector;
        this.properties = properties;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.getSqlStatistics().isEnabled()) {
            return invocation.proceed();
        }

        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        // Get SQL
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();

        // Skip if SQL is empty
        if (sql == null || sql.trim().isEmpty()) {
            return invocation.proceed();
        }

        // Record start time
        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        String failureReason = null;
        Integer affectedRows = null;

        try {
            // Execute SQL
            result = invocation.proceed();

            // Calculate affected rows
            if (result instanceof Integer) {
                affectedRows = (Integer) result;
            } else if (result instanceof java.util.List) {
                affectedRows = ((java.util.List<?>) result).size();
            }

            return result;
        } catch (Exception e) {
            success = false;
            failureReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            throw e;
        } finally {
            // Calculate execution time
            long executionTime = System.currentTimeMillis() - startTime;

            // Build SQL execution info
            SqlExecutionInfo executionInfo = buildExecutionInfo(
                sql, executionTime, affectedRows, success, failureReason, ms.getId()
            );

            // Collect statistics
            try {
                statisticsCollector.collect(executionInfo);
            } catch (Exception e) {
                log.error("Failed to collect SQL statistics", e);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        // No additional properties needed
    }

    /**
     * Build SQL execution info
     */
    private SqlExecutionInfo buildExecutionInfo(String sql, long executionTime, Integer affectedRows,
                                               boolean success, String failureReason, String mapperId) {
        // Normalize SQL (remove extra whitespace)
        String normalizedSql = SqlTemplateUtil.normalizeSql(sql);

        // Generate SQL template (parameterized SQL)
        String sqlTemplate = SqlTemplateUtil.generateTemplate(normalizedSql);

        // Calculate MD5
        String sqlMd5 = SqlTemplateUtil.calculateMd5(sqlTemplate);

        return SqlExecutionInfo.builder()
                .sql(normalizedSql)
                .sqlTemplate(sqlTemplate)
                .sqlMd5(sqlMd5)
                .executionTime(executionTime)
                .affectedRows(affectedRows)
                .success(success)
                .failureReason(failureReason)
                .executeTime(LocalDateTime.now())
                .dataSourceName(getCurrentDataSourceName())
                .tenantId(getCurrentTenantId())
                .mapperId(mapperId)
                .build();
    }

    /**
     * Get current data source name
     * This is a placeholder - should be implemented based on your dynamic data source implementation
     */
    private String getCurrentDataSourceName() {
        // TODO: Implement based on your dynamic data source context
        return "default";
    }

    /**
     * Get current tenant ID
     * This is a placeholder - should be implemented based on your multi-tenancy implementation
     */
    private String getCurrentTenantId() {
        // TODO: Implement based on your tenant context
        return null;
    }
}
