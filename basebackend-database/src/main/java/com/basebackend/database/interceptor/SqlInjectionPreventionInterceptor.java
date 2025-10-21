package com.basebackend.database.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;

import java.sql.Connection;
import java.util.regex.Pattern;

/**
 * 简单的 SQL 注入检测拦截器
 */
@Slf4j
public class SqlInjectionPreventionInterceptor implements InnerInterceptor {

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(?i)(--|/\\*|;\\s*(drop|alter|truncate)\\b|\\bor\\s+1=1|\\bunion\\s+select)"
    );

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        BoundSql boundSql = sh.getBoundSql();
        if (boundSql == null) {
            return;
        }
        String sql = boundSql.getSql();
        if (sql == null) {
            return;
        }
        String normalized = sql.replaceAll("\\s+", " ").trim();
        if (DANGEROUS_PATTERN.matcher(normalized).find()) {
            log.warn("Potential SQL injection attempt blocked. sql={}", normalized);
            throw new IllegalArgumentException("Potentially dangerous SQL detected");
        }
    }
}
