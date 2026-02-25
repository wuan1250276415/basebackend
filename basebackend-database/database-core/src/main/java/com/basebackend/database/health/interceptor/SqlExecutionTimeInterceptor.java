package com.basebackend.database.health.interceptor;

import com.basebackend.database.config.DatabaseEnhancedProperties;
import com.basebackend.database.health.logger.SlowQueryLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Properties;

/**
 * SQL执行时间拦截器
 * 拦截所有SQL执行，记录执行时间，并识别慢查询
 * 
 * 使用MyBatis标准拦截器而不是InnerInterceptor，以便能够在SQL执行前后进行拦截
 * Note: This is registered as a bean in MyBatisPlusConfig, not auto-scanned
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class SqlExecutionTimeInterceptor implements Interceptor {

    private final DatabaseEnhancedProperties properties;
    private final SlowQueryLogger slowQueryLogger;

    public SqlExecutionTimeInterceptor(DatabaseEnhancedProperties properties,
                                      SlowQueryLogger slowQueryLogger) {
        this.properties = properties;
        this.slowQueryLogger = slowQueryLogger;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (!properties.getHealth().isEnabled()) {
            return invocation.proceed();
        }

        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        Object result = null;
        try {
            // 执行SQL
            result = invocation.proceed();
            return result;
        } finally {
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 记录SQL执行时间
            if (log.isDebugEnabled()) {
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                log.debug("SQL execution time: {}ms - {}", executionTime, ms.getId());
            }

            // 检查是否为慢查询
            if (slowQueryLogger.isSlowQuery(executionTime)) {
                logSlowQuery(invocation, executionTime);
            }
        }
    }

    /**
     * 记录慢查询
     */
    private void logSlowQuery(Invocation invocation, long executionTime) {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];
            
            BoundSql boundSql;
            if (args.length == 6) {
                // query with CacheKey and BoundSql
                boundSql = (BoundSql) args[5];
            } else {
                // query or update without BoundSql
                boundSql = ms.getBoundSql(parameter);
            }
            
            String sql = boundSql.getSql();
            Object params = formatParameters(boundSql);
            
            slowQueryLogger.logSlowQuery(sql, executionTime, params);
        } catch (Exception e) {
            log.error("Failed to log slow query", e);
        }
    }

    /**
     * 格式化SQL参数
     */
    private Object formatParameters(BoundSql boundSql) {
        try {
            Object parameterObject = boundSql.getParameterObject();
            List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
            
            if (parameterMappings == null || parameterMappings.isEmpty()) {
                return parameterObject;
            }

            // 简化参数显示
            if (parameterObject != null) {
                return parameterObject.toString();
            }
            
            return null;
        } catch (Exception e) {
            log.debug("Failed to format SQL parameters", e);
            return null;
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
}
