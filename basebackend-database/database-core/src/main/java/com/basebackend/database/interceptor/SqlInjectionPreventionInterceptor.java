package com.basebackend.database.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.basebackend.database.config.DatabaseEnhancedProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * SQL 注入检测拦截器
 * 
 * 功能特性：
 * 1. 基于正则表达式检测危险 SQL 模式
 * 2. 支持白名单机制（SQL模式白名单、Mapper方法白名单）
 * 3. 支持配置开关（启用/禁用、严格模式/警告模式）
 * 4. 性能统计和监控
 * 
 * 配置示例：
 * database.enhanced.sql-injection.enabled=true
 * database.enhanced.sql-injection.strict-mode=true
 * database.enhanced.sql-injection.whitelist-patterns[0]=.*UNION.*ALL.*SELECT.*FROM.*sys_config.*
 * database.enhanced.sql-injection.whitelist-mappers[0]=com.example.mapper.ReportMapper.generateReport
 */
@Slf4j
public class SqlInjectionPreventionInterceptor implements InnerInterceptor {

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(?i)(--|/\\*|\\bor\\s+1\\s*=\\s*1\\b|\\bunion\\s+select\\b|(?<!\\w)(?:drop\\s+(?:table|database|schema|view|function|procedure|index)|alter\\s+table|truncate\\s+table)\\b)");

    private final DatabaseEnhancedProperties properties;

    // 编译后的白名单模式缓存
    private final List<Pattern> compiledWhitelistPatterns = new ArrayList<>();

    // 性能统计
    private static final AtomicLong TOTAL_CHECKS = new AtomicLong(0);
    private static final AtomicLong BLOCKED_COUNT = new AtomicLong(0);
    private static final AtomicLong WHITELISTED_COUNT = new AtomicLong(0);
    private static final ConcurrentHashMap<String, AtomicLong> BLOCKED_PATTERNS = new ConcurrentHashMap<>();

    /**
     * 默认构造函数（向后兼容）
     */
    public SqlInjectionPreventionInterceptor() {
        this(null);
    }

    /**
     * 带配置的构造函数
     */
    public SqlInjectionPreventionInterceptor(DatabaseEnhancedProperties properties) {
        this.properties = properties;
        initWhitelistPatterns();
    }

    /**
     * 初始化白名单模式
     */
    private void initWhitelistPatterns() {
        if (properties == null || properties.getSqlInjection() == null) {
            return;
        }

        List<String> patterns = properties.getSqlInjection().getWhitelistPatterns();
        if (patterns != null) {
            for (String pattern : patterns) {
                try {
                    compiledWhitelistPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
                    log.info("SQL injection whitelist pattern added: {}", pattern);
                } catch (Exception e) {
                    log.error("Invalid whitelist pattern: {}", pattern, e);
                }
            }
        }
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        // 检查是否启用
        if (properties != null && !properties.getSqlInjection().isEnabled()) {
            return;
        }

        BoundSql boundSql = sh.getBoundSql();
        if (boundSql == null) {
            return;
        }

        String sql = boundSql.getSql();
        if (sql == null) {
            return;
        }

        TOTAL_CHECKS.incrementAndGet();

        String normalized = sql.replaceAll("\\s+", " ").trim();

        // 获取 Mapper 方法 ID
        String mapperId = getMapperId(sh);

        // 检查 Mapper 白名单
        if (isMapperWhitelisted(mapperId)) {
            WHITELISTED_COUNT.incrementAndGet();
            log.debug("SQL check skipped (mapper whitelisted): mapperId={}", mapperId);
            return;
        }

        // 检查 SQL 模式白名单
        if (isSqlWhitelisted(normalized)) {
            WHITELISTED_COUNT.incrementAndGet();
            log.debug("SQL check skipped (pattern whitelisted): sql={}", truncateSql(normalized));
            return;
        }

        // 执行危险模式检测
        if (DANGEROUS_PATTERN.matcher(normalized).find()) {
            BLOCKED_COUNT.incrementAndGet();

            // 记录被阻止的模式类型
            String patternType = detectPatternType(normalized);
            BLOCKED_PATTERNS.computeIfAbsent(patternType, k -> new AtomicLong(0)).incrementAndGet();

            boolean strictMode = properties == null || properties.getSqlInjection().isStrictMode();
            boolean logBlocked = properties == null || properties.getSqlInjection().isLogBlockedSql();

            if (logBlocked) {
                log.warn("Potential SQL injection detected: mapperId={}, patternType={}, sql={}",
                        mapperId, patternType, truncateSql(normalized));
            }

            if (strictMode) {
                throw new SqlInjectionException(
                        String.format("Potentially dangerous SQL detected [%s]. " +
                                "If this is a legitimate query, add it to the whitelist. mapperId=%s",
                                patternType, mapperId));
            } else {
                log.warn("SQL injection warning (non-strict mode, allowing execution): mapperId={}", mapperId);
            }
        }
    }

    /**
     * 获取 Mapper 方法 ID
     */
    private String getMapperId(StatementHandler sh) {
        try {
            // 先解开可能存在的代理，避免无法访问真实属性
            Object target = PluginUtils.realTarget(sh);
            MetaObject metaObject = SystemMetaObject.forObject(target);

            // 兼容不同的 StatementHandler 实现，优先访问 delegate.mappedStatement
            MappedStatement ms = null;
            if (metaObject.hasGetter("delegate.mappedStatement")) {
                ms = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            } else if (metaObject.hasGetter("mappedStatement")) {
                ms = (MappedStatement) metaObject.getValue("mappedStatement");
            }

            if (ms != null) {
                return ms.getId();
            }
        } catch (Exception e) {
            log.debug("Failed to get mapper ID from StatementHandler: {}", sh.getClass().getName(), e);
        }
        return "unknown";
    }

    /**
     * 检查 Mapper 是否在白名单中
     */
    private boolean isMapperWhitelisted(String mapperId) {
        if (properties == null || properties.getSqlInjection() == null) {
            return false;
        }

        List<String> whitelistMappers = properties.getSqlInjection().getWhitelistMappers();
        if (whitelistMappers == null || whitelistMappers.isEmpty()) {
            return false;
        }

        return whitelistMappers.stream()
                .anyMatch(pattern -> mapperId.equals(pattern) || mapperId.matches(pattern));
    }

    /**
     * 检查 SQL 是否匹配白名单模式
     */
    private boolean isSqlWhitelisted(String sql) {
        for (Pattern pattern : compiledWhitelistPatterns) {
            if (pattern.matcher(sql).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测危险模式类型
     */
    private String detectPatternType(String sql) {
        String lowerSql = sql.toLowerCase();
        if (lowerSql.contains("union select")) {
            return "UNION_INJECTION";
        } else if (lowerSql.contains("or 1=1") || lowerSql.contains("or 1 = 1")) {
            return "BOOLEAN_INJECTION";
        } else if (lowerSql.contains("drop ")) {
            return "DROP_STATEMENT";
        } else if (lowerSql.contains("alter table")) {
            return "ALTER_STATEMENT";
        } else if (lowerSql.contains("truncate")) {
            return "TRUNCATE_STATEMENT";
        } else if (lowerSql.contains("--") || lowerSql.contains("/*")) {
            return "COMMENT_INJECTION";
        }
        return "UNKNOWN";
    }

    /**
     * 截断 SQL 用于日志记录
     */
    private String truncateSql(String sql) {
        if (sql == null) {
            return null;
        }
        return sql.length() > 500 ? sql.substring(0, 500) + "..." : sql;
    }

    /**
     * 获取性能统计信息
     */
    public static java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalChecks", TOTAL_CHECKS.get());
        stats.put("blockedCount", BLOCKED_COUNT.get());
        stats.put("whitelistedCount", WHITELISTED_COUNT.get());
        stats.put("blockRate", TOTAL_CHECKS.get() > 0 ? (double) BLOCKED_COUNT.get() / TOTAL_CHECKS.get() * 100 : 0);

        java.util.Map<String, Long> patternStats = new java.util.HashMap<>();
        BLOCKED_PATTERNS.forEach((k, v) -> patternStats.put(k, v.get()));
        stats.put("blockedPatterns", patternStats);

        return stats;
    }

    /**
     * 重置统计计数器
     */
    public static void resetStatistics() {
        TOTAL_CHECKS.set(0);
        BLOCKED_COUNT.set(0);
        WHITELISTED_COUNT.set(0);
        BLOCKED_PATTERNS.clear();
    }

    /**
     * 动态添加白名单模式
     */
    public void addWhitelistPattern(String pattern) {
        try {
            compiledWhitelistPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            log.info("SQL injection whitelist pattern added dynamically: {}", pattern);
        } catch (Exception e) {
            log.error("Invalid whitelist pattern: {}", pattern, e);
            throw new IllegalArgumentException("Invalid regex pattern: " + pattern, e);
        }
    }

    /**
     * SQL 注入异常
     */
    public static class SqlInjectionException extends RuntimeException {
        public SqlInjectionException(String message) {
            super(message);
        }
    }
}
